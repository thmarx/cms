package com.condation.cms.modules.ui.extensionpoints.remotemethods;

/*-
 * #%L
 * UI Module
 * %%
 * Copyright (C) 2023 - 2026 CondationCMS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import com.condation.cms.api.Constants;
import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.eventbus.events.InvalidateContentCacheEvent;
import com.condation.cms.api.eventbus.events.ReIndexContentMetaDataEvent;
import com.condation.cms.api.extensions.AbstractExtensionPoint;
import com.condation.cms.api.feature.features.CurrentNodeFeature;
import com.condation.cms.api.feature.features.CurrentCollectionItemFeature;
import com.condation.cms.api.feature.features.EventBusFeature;
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.RequestFeature;
import com.condation.cms.api.feature.features.SitePropertiesFeature;
import com.condation.cms.api.feature.features.ConfigurationFeature;
import com.condation.cms.api.configuration.configs.CollectionConfiguration;
import com.condation.cms.api.ui.extensions.UIRemoteMethodExtensionPoint;
import com.condation.cms.api.utils.MapUtil;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.core.content.io.YamlHeaderUpdater;
import com.condation.modules.api.annotation.Extension;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.condation.cms.api.ui.annotations.RemoteMethod;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.utils.SectionUtil;
import com.condation.cms.content.SectionEntry;
import com.condation.cms.content.ConfigurableVariantSelector;
import com.condation.cms.content.CollectionRouteResolver;
import com.condation.cms.modules.ui.utils.FormHelper;
import com.condation.cms.modules.ui.utils.MarkdownHelper;
import com.condation.cms.modules.ui.utils.MetaConverter;
import com.condation.cms.modules.ui.utils.NumberUtils;
import com.condation.cms.modules.ui.utils.UIFileNameUtil;
import com.condation.cms.modules.ui.utils.UIPathUtil;
import java.nio.file.Files;

/**
 *
 * @author t.marx
 */
@Slf4j
@Extension(UIRemoteMethodExtensionPoint.class)
public class RemoteContentEndpointsExtension extends AbstractRemoteMethodeExtension {

	private static final String CONTENT_NOT_FOUND = "content not found";

	@RemoteMethod(name = "content.get", permissions = {Permissions.CONTENT_EDIT})
	public Object getContent(Map<String, Object> parameters) throws RPCException {
		var target = editableTarget(parameters);

		Map<String, Object> result = new HashMap<>();
		result.put("uri", target.uri());
		try {
			var document = loadTarget(target, parameters);
			result.put(Parameters.CONTENT, document.content());
			result.put("meta", document.metadata());
		} catch (IOException ex) {
			log.error("", ex);
			throw new RPCException(0, ex.getMessage());
		}

		return result;
	}

	@RemoteMethod(name = "content.set", permissions = {Permissions.CONTENT_EDIT})
	public Object setContent(Map<String, Object> parameters) throws RPCException {
		var updatedContent = FormHelper.getContent(parameters.get(Parameters.CONTENT));
		var target = editableTarget(parameters);

		Map<String, Object> result = new HashMap<>();
		result.put("uri", target.uri());
		try {
			var document = loadTarget(target, parameters);
			saveTarget(target, parameters, document.metadata(), updatedContent);
			log.debug(LOG_PATTERN, target.uri());
		} catch (IOException ex) {
			log.error("", ex);
			throw new RPCException(0, ex.getMessage());
		}

		return result;
	}
    private static final String LOG_PATTERN = "file {} saved";
	
	@RemoteMethod(name = "content.replace", permissions = {Permissions.CONTENT_EDIT})
	public Object replaceContent(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableContentRepository(parameters);

		var replacement = (String)parameters.get(Parameters.CONTENT);
		int start = NumberUtils.toInt(parameters.getOrDefault("start", -1l));
		int end = NumberUtils.toInt(parameters.getOrDefault("end", -1l));
		var uri = contentUri(parameters);

		Map<String, Object> result = new HashMap<>();
		result.put("uri", uri);
		
		if (replacement == null) {
			throw new RPCException("replacement must not be null");
		}
		
		var node = repository.get(uri);
		if (node.isPresent()) {
			try {
				var document = repository.load(node.get()).orElseThrow();
				var content = document.content();
				
                var contextPath = getContext().get(SitePropertiesFeature.class).siteProperties().contextPath();
                
				var updatedContent = MarkdownHelper.replaceImage(contextPath, content, start, end, replacement);

				repository.save(uri, node.get().data(), updatedContent);
				log.debug(LOG_PATTERN, uri);
			} catch (IOException ex) {
				log.error("", ex);
				throw new RPCException(0, ex.getMessage());
			}
		}

		return result;
	}

	@RemoteMethod(name = "meta.set", permissions = {Permissions.CONTENT_EDIT})
	public Object setMeta(Map<String, Object> parameters) throws RPCException {
		var updateParam = (Map<String, Map<String, Object>>) parameters.get("meta");
		var update = MetaConverter.convertMeta(updateParam);
		var target = editableTarget(parameters);

		Map<String, Object> result = new HashMap<>();
		result.put("uri", target.uri());
		try {
				var document = loadTarget(target, parameters);
				Map<String, Object> meta = new HashMap<>(document.metadata());
				YamlHeaderUpdater.mergeFlatMapIntoNestedMap(meta, update);
				saveTarget(target, parameters, meta, document.content());
				log.debug(LOG_PATTERN, target.uri());
		} catch (IOException ex) {
				log.error("", ex);
				throw new RPCException(0, ex.getMessage());
		}

		return result;
	}

	private record Update (String uri, Map<String, Map<String, Object>> meta) {}

	@RemoteMethod(name = "meta.set.batch", permissions = {Permissions.CONTENT_EDIT})
	public Object setMetaBatch(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableContentRepository(parameters);

		Map<String, Object> result = new HashMap<>();
		result.put("endpoint", "meta.set.batch");

		List<Map<String, Object>> updatesParam = (List<Map<String, Object>>) parameters.get("updates");

		var updates = updatesParam.stream().map(update -> {
			return new Update(
					(String)update.get("uri"),
					(Map<String, Map<String, Object>>)update.get("meta"));
		}).toList();

		try {
			updates.forEach(update -> {
				var node = repository.get(update.uri);
				if (node.isPresent()) {
					try {
						var document = repository.load(node.get()).orElseThrow();
						Map<String, Object> fileMeta = new HashMap<>(node.get().data());
						var metaUpdated = MetaConverter.convertMeta(update.meta);
						YamlHeaderUpdater.mergeFlatMapIntoNestedMap(fileMeta, metaUpdated);

						repository.save(update.uri, fileMeta, document.content());
						log.debug(LOG_PATTERN, update.uri);

						getContext().get(EventBusFeature.class).eventBus().publish(new ReIndexContentMetaDataEvent(update.uri));
					} catch (IOException ex) {
						throw new java.io.UncheckedIOException(ex);
					}
				}
			});
		} catch (java.io.UncheckedIOException ex) {
			log.error("", ex.getCause());
			throw new RPCException(0, ex.getCause().getMessage());
		}

		return result;
	}

	@RemoteMethod(name = "content.sectionEntry.delete", permissions = {Permissions.CONTENT_EDIT})
	public Object deleteSectionEntry(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableContentRepository(parameters);
		var uri = (String) parameters.get("uri");

		Map<String, Object> result = new HashMap<>();
		result.put("uri", uri);
		if (repository.resourceExists(uri)) {
			try {
				repository.delete(uri);
				getContext().get(EventBusFeature.class).eventBus().publish(new InvalidateContentCacheEvent());
			} catch (Exception ex) {
				log.error("", ex);
				throw new RPCException(0, ex.getMessage());
			}
		}

		return result;
	}

	@RemoteMethod(name = "content.sectionEntry.add", permissions = {Permissions.CONTENT_EDIT})
	public Object addSectionEntry(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableContentRepository(parameters);

		var content = (String) parameters.getOrDefault(Parameters.CONTENT, "");
		var parentUri = contentUri(parameters, "parentUri");
		var section = (String) parameters.get("section");
		var sectionEntryName = (String) parameters.get("sectionEntryName");
		var template = (String) parameters.get("template");

		var title = sectionEntryName;
		sectionEntryName = UIPathUtil.slugify(sectionEntryName);
		
		var uri = UIFileNameUtil.createSectionEntryFileName(parentUri, section, sectionEntryName);
		
		Map<String, Object> result = new HashMap<>();
		result.put("uri", uri);
		try {
				Map<String, Object> meta = Map.of(
						"template", template,
						"title", title,
						"layout", Map.of(
								"order", 1000)
				);

				repository.save(uri, meta, content);
				log.debug(LOG_PATTERN, uri);

				getContext().get(EventBusFeature.class).eventBus().publish(new ReIndexContentMetaDataEvent(uri));
		} catch (IOException ex) {
				log.error("", ex);
				throw new RPCException(0, ex.getMessage());
		}

		return result;
	}

	@RemoteMethod(name = "content.node", permissions = {Permissions.CONTENT_EDIT})
	public Object getContentNode (Map<String, Object> parameters) {
		var repository = getContentRepository(parameters);
		
		var url = (String) parameters.get("url");

		var requestUri = URI.create(url);
		var path = requestUri.getPath();
		var contextPath = getRequestContext().get(RequestFeature.class).context();
		if (contextPath != null && !"/".equals(contextPath) && path.startsWith(contextPath)) {
			path = path.substring(contextPath.length());
		}
		if (path.isBlank()) {
			path = "/";
		} else if (!path.startsWith("/")) {
			path = "/" + path;
		}

		var selectedNode = repository.findByUrl(path).orElse(null);
		String canonicalUri = selectedNode == null ? null : selectedNode.path();

		Map<String, Object> result = new HashMap<>();
		result.put("url", url);
		if (selectedNode == null || canonicalUri == null) {
			var collectionConfiguration = getContext().get(ConfigurationFeature.class)
					.configuration().get(CollectionConfiguration.class);
			var collectionRoute = new CollectionRouteResolver(
					getCollectionRepository(parameters), collectionConfiguration).resolve(path);
			if (collectionRoute.isEmpty()) {
				return result;
			}
			var item = collectionRoute.get().item();
			result.put("uri", item.path());
			result.put("canonicalUri", item.path());
			result.put("variantId", null);
			result.put("contentKind", "collection");
			result.put("supportsVariants", false);
			result.put("collection", item.collection());
			result.put("collectionItemId", item.id());
			result.put("sections", Map.of());
			return result;
		}
		result.put("contentKind", "content");
		result.put("supportsVariants", true);

		var query = com.condation.cms.api.utils.HTTPUtil.queryParameters(requestUri.getQuery());
		var variantId = query.getOrDefault(
				ConfigurableVariantSelector.VARIANT_QUERY_PARAMETER,
				List.of()
		).stream().findFirst().orElse("").trim();
		String activeVariantId = null;
		if (selectedNode != null
				&& !variantId.isBlank()
				&& !ConfigurableVariantSelector.CANONICAL_VARIANT_ID.equalsIgnoreCase(variantId)) {
			var selectedVariant = repository.variant(selectedNode, variantId);
			if (selectedVariant.isPresent()) {
				selectedNode = selectedVariant.get().node();
				activeVariantId = selectedVariant.get().id();
			}
		}
		if (selectedNode != null) {
			result.put("uri", selectedNode.path());
		} else {
			result.put("uri", canonicalUri);
		}
		result.put("canonicalUri", canonicalUri);
		result.put("variantId", activeVariantId);

		var sectionEntries = selectedNode == null
				? java.util.List.<com.condation.cms.api.repository.Section>of()
				: loadSections(repository, selectedNode);
		Map<String, List<SectionEntry>> sectionMap = new HashMap<>();
		sectionEntries.forEach(sectionEntry -> {
			String uri = sectionEntry.id();
			String name = sectionEntry.name();
			var index = MapUtil.getValue(sectionEntry.data(), Constants.MetaFields.LAYOUT_ORDER, Constants.DEFAULT_SECTION_ENTRY_LAYOUT_ORDER);

			sectionMap.computeIfAbsent(name, k -> new ArrayList<>())
					.add(new SectionEntry(fileName(uri), index, "", sectionEntry.data(), uri));
		});
		result.put("sections", sectionMap);

		return result;
	}

	private String contentUri(Map<String, Object> parameters) throws RPCException {
		return contentUri(parameters, "uri");
	}

	private String contentUri(Map<String, Object> parameters, String parameterName) throws RPCException {
		var value = parameters.get(parameterName);
		if (value instanceof String uri && !uri.isBlank()) {
			return uri;
		}
		if (getRequestContext().has(CurrentNodeFeature.class)) {
			return getRequestContext().get(CurrentNodeFeature.class).node().uri();
		}
		throw new RPCException(400, parameterName + " must not be blank");
	}

	private EditableTarget editableTarget(Map<String, Object> parameters) throws RPCException {
		if (!parameters.containsKey("uri")
				&& getRequestContext().has(CurrentCollectionItemFeature.class)) {
			var item = getRequestContext().get(CurrentCollectionItemFeature.class).item();
			if (getCollectionRepository(parameters).access(item.collection()) != CollectionAccess.READ_WRITE) {
				throw new RPCException(403, "referenced collection is read-only: " + item.collection());
			}
			return new EditableTarget(
					item.path(),
					item.collection(),
					item.id());
		}
		var uri = contentUri(parameters);
		return new EditableTarget(
				uri,
				null,
				null);
	}

	private EditableDocument loadTarget(EditableTarget target, Map<String, Object> parameters)
			throws IOException, RPCException {
		if (target.collectionName() != null) {
			var item = getCollectionRepository(parameters)
					.get(target.collectionName(), target.itemId())
					.orElseThrow(() -> new RPCException(404, CONTENT_NOT_FOUND));
			return new EditableDocument(new HashMap<>(item.meta()), item.content());
		}
		var repository = getContentRepository(parameters);
		var node = repository.get(target.uri())
				.orElseThrow(() -> new RPCException(404, CONTENT_NOT_FOUND));
		var document = repository.load(node)
				.orElseThrow(() -> new RPCException(404, CONTENT_NOT_FOUND));
		return new EditableDocument(new HashMap<>(node.data()), document.content());
	}

	private void saveTarget(EditableTarget target, Map<String, Object> parameters,
			Map<String, Object> metadata, String content) throws IOException {
		if (target.collectionName() != null) {
			getMutableCollectionRepository(parameters).save(
					target.collectionName(), target.itemId(), metadata, content);
			getContext().get(EventBusFeature.class).eventBus().publish(new InvalidateContentCacheEvent());
		} else {
			getMutableContentRepository(parameters).save(target.uri(), metadata, content);
			getContext().get(EventBusFeature.class).eventBus().publish(new InvalidateContentCacheEvent());
		}
	}

	private List<com.condation.cms.api.repository.Section> loadSections(
			ContentRepository repository, ContentNode node) {
		try {
			return repository.sections(node);
		} catch (IOException ex) {
			log.error("could not load sections for {}", node.path(), ex);
			return List.of();
		}
	}

	private static String fileName(String path) {
		var normalized = path.replace('\\', '/');
		var separator = normalized.lastIndexOf('/');
		return separator < 0 ? normalized : normalized.substring(separator + 1);
	}

	private record EditableTarget(
			String uri,
			String collectionName,
			String itemId) {
	}

	private record EditableDocument(Map<String, Object> metadata, String content) {
	}
}
