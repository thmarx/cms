package com.condation.cms.content;

/*-
 * #%L
 * CMS Content
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
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.db.taxonomy.Taxonomy;
import com.condation.cms.api.extensions.ContentQueryOperatorExtensionPoint;
import com.condation.cms.api.extensions.TemplateModelExtendingExtensionPoint;
import com.condation.cms.api.feature.features.AuthFeature;
import com.condation.cms.api.feature.features.HookSystemFeature;
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.IsDevModeFeature;
import com.condation.cms.api.feature.features.IsPreviewFeature;
import com.condation.cms.api.feature.features.MarkdownRendererFeature;
import com.condation.cms.api.feature.features.RequestFeature;
import com.condation.cms.api.feature.features.ServerPropertiesFeature;
import com.condation.cms.api.feature.features.SiteMediaServiceFeature;
import com.condation.cms.api.messages.MessageSource;
import com.condation.cms.api.model.ListNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentDocument;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.repository.Section;
import com.condation.cms.api.template.TemplateEngine;
import com.condation.cms.api.utils.MapUtil;
import com.condation.cms.content.pipeline.ContentPipelineFactory;
import com.condation.cms.content.views.model.View;
import com.condation.cms.api.content.MapAccess;
import com.condation.cms.content.pipeline.HTMLPipeline;
import com.condation.cms.hooksystem.extensions.DBHooks;
import com.condation.cms.hooksystem.extensions.TemplateHooks;
import com.condation.cms.content.template.functions.LinkFunction;
import com.condation.cms.content.template.functions.MarkdownFunction;
import com.condation.cms.content.template.functions.geo.GeoFunction;
import com.condation.cms.content.template.functions.list.NodeListFunctionBuilder;
import com.condation.cms.content.template.functions.navigation.NavigationFunction;
import com.condation.cms.content.template.functions.query.QueryFunction;
import com.condation.cms.content.template.functions.taxonomy.TaxonomyFunction;
import com.condation.cms.content.template.functions.translation.NodeTranslations;
import com.condation.cms.content.template.functions.translation.SiteTranslations;
import com.condation.modules.api.ModuleManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public class DefaultContentRenderer implements ContentRenderer {

	private final Supplier<TemplateEngine> templates;
	private final DB db;
	private final SiteProperties siteProperties;
	private final ModuleManager moduleManager;
	private final ContentRepository contentRepository;
	private final CollectionRepository collectionRepository;

	public DefaultContentRenderer(
			Supplier<TemplateEngine> templates,
			DB db,
			SiteProperties siteProperties,
			ModuleManager moduleManager,
			ContentRepository contentRepository,
			CollectionRepository collectionRepository) {
		this.templates = templates;
		this.db = db;
		this.siteProperties = siteProperties;
		this.moduleManager = moduleManager;
		this.contentRepository = contentRepository;
		this.collectionRepository = collectionRepository;
	}

	private record ResolvedRenderInput(
			String uri,
			Map<String, List<SectionEntry>> sectionEntries,
			Map<String, Object> meta,
			String rawContent,
			Optional<ContentNode> contentNode) {
	}

	@Override
	public String render(
			ContentDocument document,
			RequestContext context,
			Map<String, List<SectionEntry>> sectionEntries) throws IOException {
		return renderResolved(
				document.node(),
				context,
				new ResolvedRenderInput(
						document.node().path(),
						sectionEntries,
						document.node().data(),
						document.content(),
						Optional.of(document.node())),
				model -> {
				});
	}

	@Override
	public Map<String, List<SectionEntry>> renderSections(
			List<Section> sections,
			RequestContext context) throws IOException {
		if (sections.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, List<SectionEntry>> renderedSections = new HashMap<>();
		for (var section : sections) {
			try {
				var sectionNode = contentRepository.get(section.id());
				var renderedContent = renderResolved(
						sectionNode.orElse(null),
						context,
						new ResolvedRenderInput(
								section.id(),
								Collections.emptyMap(),
								section.data(),
								section.content(),
								sectionNode),
						model -> {
						});
				var index = MapUtil.getValue(
						section.data(),
						Constants.MetaFields.LAYOUT_ORDER,
						Constants.DEFAULT_SECTION_ENTRY_LAYOUT_ORDER);
				renderedSections.computeIfAbsent(section.name(), ignored -> new ArrayList<>())
						.add(new SectionEntry(section.name(), index, renderedContent, section.data()));
			} catch (Exception ex) {
				log.error("error rendering section {}", section.id(), ex);
			}
		}
		renderedSections.values().forEach(entries ->
				entries.sort((first, second) -> Integer.compare(first.index(), second.index())));
		return renderedSections;
	}

	@Override
	public String renderTaxonomyContent(
			Optional<ContentDocument> document,
			Taxonomy taxonomy,
			Optional<String> taxonomyValue,
			RequestContext context,
			Map<String, Object> meta,
			Page<ListNode> page,
			Map<String, List<SectionEntry>> sectionEntries) throws IOException {
		var rawContent = document.map(ContentDocument::content).orElse("");
		var node = document.map(ContentDocument::node);
		return renderResolved(
				node.orElse(null),
				context,
				new ResolvedRenderInput(
						node.map(ContentNode::path).orElse("index.md"),
						sectionEntries,
						meta,
						rawContent,
						node),
				model -> extendTaxonomyModel(model, taxonomy, taxonomyValue, page));
	}

	private void extendTaxonomyModel(
			TemplateEngine.Model model,
			Taxonomy taxonomy,
			Optional<String> taxonomyValue,
			Page<ListNode> page) {
		model.values.put("taxonomy", taxonomy);
		model.values.put("taxonomy_values", db.getTaxonomies().values(taxonomy));
		taxonomyValue.ifPresent(value -> model.values.put("taxonomy_value", value));
		model.values.put("page", page);
	}

	@Override
	public String renderView(
			ContentDocument document,
			View view,
			RequestContext requestContext,
			Page<ListNode> page) throws IOException {
		return renderResolved(
				document.node(),
				requestContext,
				new ResolvedRenderInput(
						document.node().path(),
						Collections.emptyMap(),
						document.node().data(),
						"",
						Optional.of(document.node())),
				model -> model.values.put("page", page));
	}

	private String renderContent(final String rawContent, final RequestContext context, final TemplateEngine.Model model) {
		var pipeline = ContentPipelineFactory.create(context, model);
		
		return pipeline.process(rawContent);
	}

	@Override
	public String renderCollection(
			ContentNode collectionNode,
			CollectionItem item,
			String template,
			RequestContext context) throws IOException {
		var meta = new HashMap<>(item.meta());
		meta.put("template", template);
		return renderResolved(
				collectionNode,
				context,
				new ResolvedRenderInput(
						collectionNode.url(),
						Collections.emptyMap(),
						meta,
						item.content(),
						Optional.of(collectionNode)),
				model -> {
					model.values.put("collection_item", item);
					model.values.put("collection", collectionRepository.collection(item.collection()));
				});
	}

	private String renderResolved(
			ContentNode currentNode,
			RequestContext context,
			ResolvedRenderInput input,
			Consumer<TemplateEngine.Model> modelExtending) throws IOException {
		var uri = input.uri();
		var sectionEntries = input.sectionEntries();
		var meta = input.meta();
		var rawContent = input.rawContent();
		var contentNode = input.contentNode();
		TemplateEngine.Model model = new TemplateEngine.Model(
				contentNode.orElse(currentNode),
				context);

		modelExtending.accept(model);

		Namespace namespace = new Namespace();

		namespace.add(Constants.TemplateNamespaces.NODE, "meta", new MapAccess(meta));
        // sections will be removed
		namespace.add(Constants.TemplateNamespaces.NODE, "sections", sectionEntries);
		namespace.add(Constants.TemplateNamespaces.NODE, "uri", uri);
		namespace.add(Constants.TemplateNamespaces.NODE, "translation", new NodeTranslations(contentNode.orElse(null), siteProperties));
		
		namespace.add(Constants.TemplateNamespaces.NODE, "properties", new MapAccess((NodeProperties.createNodeProperties(contentNode.orElse(null), siteProperties))));
		
		NavigationFunction navigationFunction = createNavigationFunction(currentNode, context);
		namespace.add(Constants.TemplateNamespaces.CMS, "navigation", navigationFunction);
		
		NodeListFunctionBuilder nodeListFunction = createNodeListFunction(currentNode, context);
		namespace.add(Constants.TemplateNamespaces.CMS, "nodeList", nodeListFunction);
		
		QueryFunction queryFunction = createQueryFunction(currentNode, context);
		namespace.add(Constants.TemplateNamespaces.CMS, "query", queryFunction);
		namespace.add(Constants.TemplateNamespaces.CMS, "geo", new GeoFunction());
		
		MarkdownFunction markdownFunction = createMarkdownFunction(context);
		namespace.add(Constants.TemplateNamespaces.CMS, "markdown", markdownFunction);

		model.values.put("requestContext", context.get(RequestFeature.class));
		model.values.put("theme", context.get(RenderContext.class).theme());
		namespace.add(Constants.TemplateNamespaces.CMS, "mediaService", context.get(SiteMediaServiceFeature.class).mediaService());
		namespace.add(Constants.TemplateNamespaces.CMS, "taxonomies", context.get(InjectorFeature.class).injector().getInstance(TaxonomyFunction.class));

		namespace.add(Constants.TemplateNamespaces.SITE, "properties", siteProperties);
		namespace.add(Constants.TemplateNamespaces.SITE, "translation", new SiteTranslations(siteProperties));
		
		var theme = context.get(RenderContext.class).theme();
		if (theme.empty()) {
			model.values.put("messages", context.get(InjectorFeature.class).injector().getInstance(MessageSource.class));
		} else {
			model.values.put("messages", theme.getMessages());
		}
		
		namespace.add(Constants.TemplateNamespaces.CMS, "links", new LinkFunction(context));

		model.values.put("PREVIEW_MODE", isPreview(context));
		model.values.put("MANAGER", isManager(context));
		model.values.put("DEV_MODE", isDevMode(context));
		model.values.put("ENV", context.get(ServerPropertiesFeature.class).serverProperties().env());

		if (context.has(AuthFeature.class)) {
			model.values.put("USERNAME", context.get(AuthFeature.class).username());
		}

		context.get(TemplateHooks.class).getTemplateSupplier().getRegisterTemplateSupplier().forEach(service -> {
			model.values.put(service.name(), service.supplier());
			namespace.add(Constants.TemplateNamespaces.DEFAULT_MODULE_NAMESPACE, service.name(), service.supplier());
		});

		extendModel(model, namespace);

		var modelCopy = model.copy();
		modelCopy.values.putAll(namespace.getNamespaces());
		
		String content = renderContent(rawContent, context, modelCopy);
		model.values.put("content", content);
		namespace.add(Constants.TemplateNamespaces.NODE, "content", content);
		
		model.values.putAll(namespace.getNamespaces());

		var htmlContent = templates.get().render((String) meta.get("template"), model);
		
		HTMLPipeline htmlPipeline = new HTMLPipeline(context.get(HookSystemFeature.class).hookSystem());
		return htmlPipeline.process(htmlContent);
	}

	protected MarkdownFunction createMarkdownFunction(final RequestContext context) {
		return new MarkdownFunction(context.get(MarkdownRendererFeature.class).markdownRenderer());
	}

	protected QueryFunction createQueryFunction(final ContentNode currentNode, final RequestContext context) {

		Map<String, BiPredicate<Object, Object>> customOperators = new HashMap<>();

		customOperators.putAll( context.get(DBHooks.class).getQueryOperations().getOperations() );

		moduleManager
				.extensions(ContentQueryOperatorExtensionPoint.class)
				.forEach(extension -> customOperators.put(extension.getOperator(), extension.getPredicate()));

		var queryFn = new QueryFunction(
				contentRepository, currentNode, context, customOperators);
		queryFn.setContentType(siteProperties.defaultContentType());
		return queryFn;
	}

	protected NodeListFunctionBuilder createNodeListFunction(final ContentNode currentNode, final RequestContext context) {
		var nlFn = new NodeListFunctionBuilder(contentRepository, currentNode, context);
		nlFn.contentType(siteProperties.defaultContentType());
		return nlFn;
	}

	protected NavigationFunction createNavigationFunction(final ContentNode currentNode, final RequestContext context) {
		var navFn = new NavigationFunction(contentRepository, currentNode, context);
		navFn.contentType(siteProperties.defaultContentType());
		return navFn;
	}

	private boolean isPreview(final RequestContext context) {
		return context.has(IsPreviewFeature.class);
	}

	private boolean isManager(final RequestContext context) {
		if (context.has(IsPreviewFeature.class))  {
			return context.get(IsPreviewFeature.class).mode().equals(IsPreviewFeature.Mode.MANAGER);
		}
		return false;
	}
	
	private boolean isDevMode(final RequestContext context) {
		return context.has(IsDevModeFeature.class);
	}

	private void extendModel(final TemplateEngine.Model model, Namespace namespace) {
		moduleManager.extensions(TemplateModelExtendingExtensionPoint.class).forEach(extensionPoint -> {
			var modModel = extensionPoint.getModel();
			modModel.entrySet().forEach(entry -> namespace.add(
				extensionPoint.getNamespace(), 
				entry.getKey(), 
				entry.getValue()
			));
		});
	}

}
