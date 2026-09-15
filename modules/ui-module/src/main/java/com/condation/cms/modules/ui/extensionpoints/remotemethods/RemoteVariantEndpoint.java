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

import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.Constants;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.eventbus.events.ReIndexContentMetaDataEvent;
import com.condation.cms.api.feature.features.EventBusFeature;
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.WorkflowFeature;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.ui.annotations.RemoteMethod;
import com.condation.cms.api.ui.extensions.UIRemoteMethodExtensionPoint;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.variants.Variant;
import com.condation.cms.api.utils.PathUtil;
import com.condation.cms.content.ConfigurableVariantSelector;
import com.condation.cms.content.VariantSelectorConfigurationRepository;
import com.condation.cms.core.content.io.ContentFileParser;
import com.condation.cms.core.content.io.YamlHeaderUpdater;
import com.condation.cms.modules.ui.extensionpoints.remotemethods.dto.VariantDto;
import com.condation.cms.modules.ui.utils.UIPathUtil;
import com.condation.modules.api.annotation.Extension;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Remote methods for loading the variants of a content node.
 *
 * @author thorstenmarx
 */
@Extension(UIRemoteMethodExtensionPoint.class)
@Slf4j
public class RemoteVariantEndpoint extends AbstractRemoteMethodeExtension {

	@RemoteMethod(name = "variants.selectors.get", permissions = {Permissions.CONTENT_EDIT})
	public Object getSelectors(Map<String, Object> parameters) throws RPCException {
		var uri = stringParameter(parameters, "uri");
		if (uri.isBlank()) {
			throw new RPCException(400, "uri must not be blank");
		}

		var node = findContentNode(parameters, uri);
		var configurableSelector = getConfigurableVariantSelector();
		var repository = getVariantSelectorConfigurationRepository();

		return Map.of(
				"selector", repository.getSelectorId(node),
				"selectors", configurableSelector.availableSelectors().values()
						.stream()
						.sorted(Comparator.comparing(
								ConfigurableVariantSelector.SelectorDescriptor::label
						))
						.toList()
		);
	}

	@RemoteMethod(name = "variants.selector.set", permissions = {Permissions.CONTENT_EDIT})
	public Object setSelector(Map<String, Object> parameters) throws RPCException {
		var uri = stringParameter(parameters, "uri");
		var selectorId = stringParameter(parameters, "selector");
		if (uri.isBlank() || selectorId.isBlank()) {
			throw new RPCException(400, "uri and selector must not be blank");
		}

		var configurableSelector = getConfigurableVariantSelector();
		if (!configurableSelector.hasSelector(selectorId)) {
			throw new RPCException(400, "unknown variant selector");
		}

		var node = findContentNode(parameters, uri);
		try {
			getVariantSelectorConfigurationRepository().setSelectorId(node, selectorId);
			return Map.of("selector", selectorId);
		} catch (Exception exception) {
			log.error("Could not save variant selector for '{}'", node.path(), exception);
			throw new RPCException(500, exception.getMessage());
		}
	}

	@RemoteMethod(name = "variants.create", permissions = {Permissions.CONTENT_EDIT})
	public Object create(Map<String, Object> parameters) throws RPCException {
		var uri = stringParameter(parameters, "uri");
		var id = stringParameter(parameters, "id");
		var title = stringParameter(parameters, "title");
		var template = stringParameter(parameters, "template");
		var copyContent = Boolean.TRUE.equals(parameters.get("copyContent"));

		if (uri.isBlank() || id.isBlank() || title.isBlank()) {
			throw new RPCException(400, "uri, id and title must not be blank");
		}

		var repository = getMutableContentRepository(parameters);
		var requestedNode = findContentNode(parameters, uri);
		var canonicalNode = repository.variantContext(requestedNode).canonical();
		var selectedTemplate = copyContent
				? canonicalNode.getMetaValue(Constants.MetaFields.TEMPLATE, "")
				: template;
		if (selectedTemplate.isBlank()) {
			throw new RPCException(400, "template must not be blank");
		}
		if (uiHooks().contentTypes().getPageTemplates().stream()
				.noneMatch(pageTemplate -> pageTemplate.template().equals(selectedTemplate))) {
			throw new RPCException(400, "unknown page template");
		}
		var variantId = UIPathUtil.toValidFilename(id);
		if (variantId.isBlank() || ".".equals(variantId) || "..".equals(variantId)) {
			throw new RPCException(400, "invalid variant id");
		}

		var canonicalPath = canonicalNode.path().replace('\\', '/');
		var separator = canonicalPath.lastIndexOf('/');
		var parent = separator < 0 ? "" : canonicalPath.substring(0, separator);
		var fileName = separator < 0 ? canonicalPath : canonicalPath.substring(separator + 1);
		var pageName = fileName.endsWith(".md")
				? fileName.substring(0, fileName.length() - 3)
				: fileName;
		var variantFolder = join(parent, ".variants/" + pageName + "/" + variantId);
		var variantFile = join(variantFolder, fileName);

		try {
			if (repository.resourceExists(variantFile)) {
				throw new RPCException(409, "variant already exists");
			}

			var body = copyContent
					? repository.load(canonicalNode).orElseThrow().content()
					: "";
			var sectionCopies = copyContent
					? repository.sections(canonicalNode)
							.stream()
							.map(section -> new SectionCopy(
									section.data(), section.content(),
									join(variantFolder, fileName(section.id()))
							))
							.toList()
					: java.util.List.<SectionCopy>of();
			if (sectionCopies.stream().anyMatch(copy -> repository.resourceExists(copy.target()))) {
				throw new RPCException(409, "variant section already exists");
			}

			Map<String, Object> meta = new HashMap<>();
			meta.put(Constants.MetaFields.TITLE, title);
			meta.put(Constants.MetaFields.TEMPLATE, selectedTemplate);
			meta.put(Constants.MetaFields.STATUS, getContext().get(WorkflowFeature.class)
					.workflow().getStatusProvider().newNodeStatus());
			meta.put("createdAt", Date.from(Instant.now()));
			meta.put("createdBy", getUserName());

			var createdFiles = new ArrayList<String>();
			try {
				repository.save(variantFile, meta, body);
				createdFiles.add(variantFile);
				for (var sectionCopy : sectionCopies) {
					repository.save(sectionCopy.target(), sectionCopy.metadata(), sectionCopy.content());
					createdFiles.add(sectionCopy.target());
				}
			} catch (Exception exception) {
				for (var createdFile : createdFiles.reversed()) {
					repository.delete(createdFile);
				}
				throw exception;
			}

			return Map.of(
					"id", variantId,
					"uri", variantFile,
					"url", managerVariantPreviewUrl(canonicalNode.url(), variantId)
			);
		} catch (RPCException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("Could not create variant '{}' for '{}'", variantId, canonicalNode.path(), exception);
			throw new RPCException(500, exception.getMessage());
		}
	}

	@RemoteMethod(name = "variants.delete", permissions = {Permissions.CONTENT_EDIT})
	public Object delete(Map<String, Object> parameters) throws RPCException {
		var uri = stringParameter(parameters, "uri");
		var variantId = stringParameter(parameters, "id");
		if (uri.isBlank() || variantId.isBlank()) {
			throw new RPCException(400, "uri and id must not be blank");
		}

		var repository = getMutableContentRepository(parameters);
		var requestedNode = findContentNode(parameters, uri);
		var variantContext = repository.variantContext(requestedNode);
		var variant = variantContext.variants().stream()
				.filter(candidate -> candidate.id().equals(variantId))
				.findFirst()
				.orElseThrow(() -> new RPCException(404, "variant not found"));
		var variantFolder = parentPath(variant.node().path());

		try {
			if (!repository.resourceExists(variantFolder)) {
				throw new RPCException(404, "variant folder not found");
			}
			repository.deleteRecursively(variantFolder);

			return Map.of(
					"id", variantId,
					"url", managerPreviewUrl(variantContext.canonical().url())
			);
		} catch (RPCException exception) {
			throw exception;
		} catch (Exception exception) {
			log.error("Could not delete variant '{}' for '{}'", variantId, variantContext.canonical().path(), exception);
			throw new RPCException(500, exception.getMessage());
		}
	}

	@RemoteMethod(name = "variants.get", permissions = {Permissions.CONTENT_EDIT})
	public Object get(Map<String, Object> parameters) throws RPCException {
		var uri = (String) parameters.getOrDefault("uri", "");
		if (uri.isBlank()) {
			throw new RPCException(400, "uri must not be blank");
		}

		var contentNode = findContentNode(parameters, uri);
		var variantContext = getContentRepository(parameters).variantContext(contentNode);
		var variants = variantContext.variants()
				.stream()
				.sorted(Comparator.comparing(Variant::id))
				.map(variant -> new VariantDto(
						variant.id(),
						variant.node().uri(),
						managerVariantPreviewUrl(variantContext.canonical().url(), variant.id()),
						variant.node().data()
				))
				.toList();

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("uri", variantContext.canonical().uri());
		result.put("canonical", Map.of(
				"uri", variantContext.canonical().uri(),
				"url", managerPreviewUrl(variantContext.canonical().url()),
				"title", variantContext.canonical()
						.getMetaValue(Constants.MetaFields.TITLE, variantContext.canonical().name()),
				"template", variantContext.canonical()
						.getMetaValue(Constants.MetaFields.TEMPLATE, "")
		));
		result.put("activeVariantId", variantContext.activeVariantId().orElse(null));
		result.put("variants", variants);
		return result;
	}

	private ContentNode findContentNode(Map<String, Object> parameters, String uri) throws RPCException {
		var repository = getContentRepository(parameters);
		return repository.get(uri)
				.or(() -> repository.findByUrl(uri))
				.orElseThrow(() -> new RPCException(
						404,
						"content node for uri %s not found".formatted(uri)
				));
	}

	protected ConfigurableVariantSelector getConfigurableVariantSelector() {
		return getContext().get(InjectorFeature.class)
				.injector().getInstance(ConfigurableVariantSelector.class);
	}

	protected VariantSelectorConfigurationRepository getVariantSelectorConfigurationRepository() {
		return getContext().get(InjectorFeature.class)
				.injector().getInstance(VariantSelectorConfigurationRepository.class);
	}

	private String stringParameter(Map<String, Object> parameters, String name) {
		var value = parameters.get(name);
		return value instanceof String stringValue ? stringValue.trim() : "";
	}

	private String managerPreviewUrl(String url) {
		return url + (url.contains("?") ? "&" : "?") + "preview=manager";
	}

	private String managerVariantPreviewUrl(String canonicalUrl, String variantId) {
		return managerPreviewUrl(canonicalUrl)
				+ "&"
				+ ConfigurableVariantSelector.VARIANT_QUERY_PARAMETER
				+ "="
				+ java.net.URLEncoder.encode(variantId, java.nio.charset.StandardCharsets.UTF_8);
	}

	private static String join(String parent, String child) {
		return parent.isEmpty() ? child : parent + "/" + child;
	}

	private static String parentPath(String path) {
		var normalized = path.replace('\\', '/');
		var separator = normalized.lastIndexOf('/');
		return separator < 0 ? "" : normalized.substring(0, separator);
	}

	private static String fileName(String path) {
		var normalized = path.replace('\\', '/');
		var separator = normalized.lastIndexOf('/');
		return separator < 0 ? normalized : normalized.substring(separator + 1);
	}

	private record SectionCopy(Map<String, Object> metadata, String content, String target) {
	}
}
