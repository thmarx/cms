package com.github.thmarx.cms.content;

/*-
 * #%L
 * cms-server
 * %%
 * Copyright (C) 2023 Marx-Software
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.SiteProperties;
import com.github.thmarx.cms.api.content.ContentParser;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.DB;
import com.github.thmarx.cms.api.db.Page;
import com.github.thmarx.cms.api.db.taxonomy.Taxonomy;
import com.github.thmarx.cms.api.extensions.TemplateModelExtendingExtentionPoint;
import com.github.thmarx.cms.api.messages.MessageSource;
import com.github.thmarx.cms.api.request.RequestContext;
import com.github.thmarx.cms.api.feature.features.InjectorFeature;
import com.github.thmarx.cms.api.feature.features.IsDevModeFeature;
import com.github.thmarx.cms.api.feature.features.IsPreviewFeature;
import com.github.thmarx.cms.api.feature.features.RequestFeature;
import com.github.thmarx.cms.api.feature.features.SiteMediaServiceFeature;
import com.github.thmarx.cms.api.template.TemplateEngine;
import com.github.thmarx.cms.api.utils.PathUtil;
import com.github.thmarx.cms.filesystem.functions.list.NodeListFunctionBuilder;
import com.github.thmarx.cms.filesystem.functions.navigation.NavigationFunction;
import com.github.thmarx.cms.api.utils.SectionUtil;
import com.github.thmarx.cms.api.model.ListNode;
import com.github.thmarx.cms.api.feature.features.HookSystemFeature;
import com.github.thmarx.cms.content.template.LinkFunction;
import com.github.thmarx.cms.content.views.model.View;
import com.github.thmarx.cms.filesystem.functions.query.QueryFunction;
import com.github.thmarx.cms.filesystem.functions.taxonomy.TaxonomyFunction;
import com.github.thmarx.cms.request.RenderContext;
import com.github.thmarx.cms.request.RequestExtensions;
import com.github.thmarx.modules.api.ModuleManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class ContentRenderer {

	private final ContentParser contentParser;
	private final Supplier<TemplateEngine> templates;
	private final DB db;
	private final SiteProperties siteProperties;
	private final ModuleManager moduleManager;

	public String render(final Path contentFile, final RequestContext context) throws IOException {
		return render(contentFile, context, Collections.emptyMap());
	}

	public String render(final Path contentFile, final RequestContext context, final Map<String, List<ContentRenderer.Section>> sections) throws IOException {
		var content = contentParser.parse(contentFile);

		var markdownContent = content.content();
		markdownContent = context.get(RenderContext.class).shortCodes().replace(markdownContent);

		return render(contentFile, context, sections, content.meta(), markdownContent, (model) -> {
		});
	}

	public String renderTaxonomy(final Taxonomy taxonomy, Optional<String> taxonomyValue, final RequestContext context, final Map<String, Object> meta, final Page<ListNode> page) throws IOException {
		var contentFile = db.getFileSystem().resolve("content").resolve("index.md");

		return render(contentFile, context, Collections.emptyMap(), meta, "", (model) -> {
			model.values.put("taxonomy", taxonomy);
			model.values.put("taxonomy_values", db.getTaxonomies().values(taxonomy));
			if (taxonomyValue.isPresent()) {
				model.values.put("taxonomy_value", taxonomyValue.get());
			}
			model.values.put("page", page);
			
		});
	}
	
	public String renderView(final Path viewFile, final View view, final ContentNode contentNode, final RequestContext requestContext, final Page<ListNode> page) throws IOException {
		return render(viewFile, requestContext, Collections.emptyMap(), 
				contentNode.data(), "", (model) -> {
					model.values.put("page", page);
//			model.values.put("nodes", view.getNodes(
//				db, 
//				viewFile, 
//				contentParser, 
//				requestContext.get(RenderContext.class).markdownRenderer(), 
//				requestContext.get(RequestExtensions.class).getContext(), 
//				requestContext.get(RequestFeature.class).queryParameters(), requestContext)
//			);
			
		});
	}

	public String render(final Path contentFile, final RequestContext context,
			final Map<String, List<ContentRenderer.Section>> sections,
			final Map<String, Object> meta, final String markdownContent, final Consumer<TemplateEngine.Model> modelExtending
	) throws IOException {
		var uri = PathUtil.toRelativeFile(contentFile, db.getFileSystem().resolve("content/"));
		Optional<ContentNode> contentNode = db.getContent().byUri(uri);

		TemplateEngine.Model model = new TemplateEngine.Model(contentFile, contentNode.isPresent() ? contentNode.get() : null);
		
		modelExtending.accept(model);
		
		model.values.put("meta", meta);
		model.values.put("content", context.get(RenderContext.class).markdownRenderer().render(markdownContent));
		model.values.put("sections", sections);

		model.values.put("navigation", createNavigationFunction(contentFile, context));
		model.values.put("nodeList", createNodeListFunction(contentFile, context));
		model.values.put("query", createQueryFunction(contentFile, context));
		model.values.put("requestContext", context.get(RequestFeature.class));
		model.values.put("theme", context.get(RenderContext.class).theme());
		model.values.put("site", siteProperties);
		model.values.put("mediaService", context.get(SiteMediaServiceFeature.class).mediaService());

		model.values.put("taxonomyFN", context.get(InjectorFeature.class).injector().getInstance(TaxonomyFunction.class));
		model.values.put("messages", context.get(InjectorFeature.class).injector().getInstance(MessageSource.class));
		
		model.values.put("hooks", context.get(HookSystemFeature.class).hookSystem());
		
		model.values.put("links", new LinkFunction(context));
		
		model.values.put("PREVIEW_MODE", isPreview(context));
		model.values.put("DEV_MODE", isDevMode(context));

		context.get(RequestExtensions.class).getRegisterTemplateSupplier().forEach(service -> {
			model.values.put(service.name(), service.supplier());
		});

		context.get(RequestExtensions.class).getRegisterTemplateFunctions().forEach(service -> {
			model.values.put(service.name(), service.function());
		});

		extendModel(model);

		return templates.get().render((String) meta.get("template"), model);
	}

	protected QueryFunction createQueryFunction(final Path contentFile, final RequestContext context) {
		var queryFn = new QueryFunction(db, contentFile, context);
		queryFn.setContentType(siteProperties.defaultContentType());
		return queryFn;
	}

	protected NodeListFunctionBuilder createNodeListFunction(final Path contentFile, final RequestContext context) {
		var nlFn = new NodeListFunctionBuilder(db, contentFile, context);
		nlFn.contentType(siteProperties.defaultContentType());
		return nlFn;
	}

	protected NavigationFunction createNavigationFunction(final Path contentFile, final RequestContext context) {
		var navFn = new NavigationFunction(db, contentFile, context);
		navFn.contentType(siteProperties.defaultContentType());
		return navFn;
	}

	private boolean isPreview(final RequestContext context) {
		if (context.has(IsPreviewFeature.class)) {
			return context.has(IsPreviewFeature.class);
		}
		return false;
	}

	private boolean isDevMode(final RequestContext context) {
		if (context.has(IsDevModeFeature.class)) {
			return context.has(IsDevModeFeature.class);
		}
		return false;
	}

	private void extendModel(final TemplateEngine.Model model) {
		moduleManager.extensions(TemplateModelExtendingExtentionPoint.class).forEach(extensionPoint -> extensionPoint.extendModel(model));
	}

	public Map<String, List<Section>> renderSections(final List<ContentNode> sectionNodes, final RequestContext context) throws IOException {

		if (sectionNodes.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<String, List<Section>> sections = new HashMap<>();

		final Path contentBase = db.getFileSystem().resolve("content/");
		sectionNodes.forEach(node -> {
			try {
				var sectionPath = contentBase.resolve(node.uri());
				var content = render(sectionPath, context);
				var name = SectionUtil.getSectionName(node.name());
				var index = SectionUtil.getSectionIndex(node.name());

				if (!sections.containsKey(name)) {
					sections.put(name, new ArrayList<>());
				}

				sections.get(name).add(new Section(name, index, content));
			} catch (Exception ex) {
				log.error("error render section", ex);
			}

		});

		sections.values().forEach(list -> list.sort((s1, s2) -> Integer.compare(s1.index, s2.index)));

		return sections;
	}

	public static record Section(String name, int index, String content) {

		public Section(String name, String content) {
			this(name, Constants.DEFAULT_SECTION_ORDERED_INDEX, content);
		}
	}
}
