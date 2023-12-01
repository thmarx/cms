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
import com.github.thmarx.cms.api.extensions.TemplateModelExtendingExtentionPoint;
import com.github.thmarx.cms.filesystem.FileSystem;
import com.github.thmarx.cms.filesystem.MetaData;
import com.github.thmarx.cms.api.template.TemplateEngine;
import com.github.thmarx.cms.template.functions.list.NodeListFunctionBuilder;
import com.github.thmarx.cms.template.functions.navigation.NavigationFunction;
import com.github.thmarx.cms.api.utils.SectionUtil;
import com.github.thmarx.cms.content.views.model.View;
import com.github.thmarx.cms.request.RequestContext;
import com.github.thmarx.cms.template.functions.query.QueryFunction;
import com.github.thmarx.modules.api.ModuleManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private final FileSystem fileSystem;
	private final SiteProperties siteProperties;
	private final Supplier<ModuleManager> moduleManager;
	
	public String render (final Path contentFile, final RequestContext context) throws IOException {
		return render(contentFile, context, Collections.emptyMap());
	}
	
	public String renderView (final Path contentFile, final View view, final RequestContext context) throws IOException {
		TemplateEngine.Model model = new TemplateEngine.Model(contentFile);
		model.values.put("meta", view.getMeta());
		
		model.values.put("navigation", new NavigationFunction(this.fileSystem, contentFile, contentParser, context.renderContext().markdownRenderer()));
		model.values.put("nodeList", new NodeListFunctionBuilder(fileSystem, contentFile, contentParser, context.renderContext().markdownRenderer()));
		model.values.put("query", new QueryFunction(fileSystem, contentFile, contentParser, context.renderContext().markdownRenderer()));
		model.values.put("requestContext", context);
		model.values.put("theme", context.renderContext().theme());
		model.values.put("site", siteProperties);
		
		model.values.put("nodes", view.getNodes(
				fileSystem, 
				contentFile, 
				contentParser, 
				context.renderContext().markdownRenderer(), 
				context.extensions().getContext(), 
				context.queryParameters()));
		
		context.extensions().getRegisterTemplateSupplier().forEach(service -> {
			model.values.put(service.name(), service.supplier());
		});

		context.extensions().getRegisterTemplateFunctions().forEach(service -> {
			model.values.put(service.name(), service.function());
		});
		
		extendModel(model);
		
		return templates.get().render(view.getTemplate(), model);
	}
	
	public String render (final Path contentFile, final RequestContext context, final Map<String, List<ContentRenderer.Section>> sections) throws IOException {
		var content = contentParser.parse(contentFile);
		
		var markdownContent = content.content();
		markdownContent = context.renderContext().contentTags().replace(markdownContent);
		
		TemplateEngine.Model model = new TemplateEngine.Model(contentFile);
		model.values.put("meta", content.meta());
		model.values.put("content", context.renderContext().markdownRenderer().render(markdownContent));
		model.values.put("sections", sections);
		
		model.values.put("navigation", new NavigationFunction(this.fileSystem, contentFile, contentParser, context.renderContext().markdownRenderer()));
		model.values.put("nodeList", new NodeListFunctionBuilder(fileSystem, contentFile, contentParser, context.renderContext().markdownRenderer()));
		model.values.put("query", new QueryFunction(fileSystem, contentFile, contentParser, context.renderContext().markdownRenderer()));
		model.values.put("requestContext", context);
		model.values.put("theme", context.renderContext().theme());
		model.values.put("site", siteProperties);
		
		context.extensions().getRegisterTemplateSupplier().forEach(service -> {
			model.values.put(service.name(), service.supplier());
		});

		context.extensions().getRegisterTemplateFunctions().forEach(service -> {
			model.values.put(service.name(), service.function());
		});
		
		extendModel(model);
		
		return templates.get().render((String)content.meta().get("template"), model);
	}
	
	private void extendModel (final TemplateEngine.Model model) {
		moduleManager.get().extensions(TemplateModelExtendingExtentionPoint.class).forEach(extensionPoint -> extensionPoint.extendModel(model));
	}
	
	public Map<String, List<Section>> renderSections (final List<MetaData.MetaNode> sectionNodes, final RequestContext context) throws IOException {
		
		if (sectionNodes.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, List<Section>> sections = new HashMap<>();

		final Path contentBase = fileSystem.resolve("content/");
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

	public static record Section (String name, int index, String content) {
		public Section (String name, String content) {
			this(name, Constants.DEFAULT_SECTION_ORDERED_INDEX, content);
		}
	}
}
