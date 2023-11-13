package com.github.thmarx.cms.server;

/*-
 * #%L
 * cms-server
 * %%
 * Copyright (C) 2023 Marx-Software
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import com.github.thmarx.cms.content.ContentParser;
import com.github.thmarx.cms.content.ContentRenderer;
import com.github.thmarx.cms.content.ContentResolver;
import com.github.thmarx.cms.api.SiteProperties;
import com.github.thmarx.cms.PropertiesLoader;
import com.github.thmarx.cms.api.CMSModuleContext;
import com.github.thmarx.cms.api.ServerProperties;
import com.github.thmarx.cms.api.eventbus.EventBus;
import com.github.thmarx.cms.api.extensions.MarkdownRendererProviderExtentionPoint;
import com.github.thmarx.cms.api.extensions.TemplateEngineProviderExtentionPoint;
import com.github.thmarx.cms.eventbus.DefaultEventBus;
import com.github.thmarx.cms.api.eventbus.EventListener;
import com.github.thmarx.cms.api.eventbus.events.ContentChangedEvent;
import com.github.thmarx.cms.api.eventbus.events.TemplateChangedEvent;
import com.github.thmarx.cms.filesystem.FileSystem;
import com.github.thmarx.cms.extensions.ExtensionManager;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.api.template.TemplateEngine;
import com.github.thmarx.cms.api.theme.Theme;
import com.github.thmarx.cms.module.RenderContentFunction;
import com.github.thmarx.cms.theme.DefaultTheme;
import com.github.thmarx.modules.api.ModuleManager;
import com.github.thmarx.modules.manager.ModuleAPIClassLoader;
import com.github.thmarx.modules.manager.ModuleManagerImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public class VHost {

	private final FileSystem fileSystem;

	protected ContentRenderer contentRenderer;
	protected ContentResolver contentResolver;
	protected ContentParser contentParser;
	protected TemplateEngine templateEngine;
	protected ExtensionManager extensionManager;

	protected Path contentBase;
	protected Path assetBase;
	protected Path templateBase;

	@Getter
	private String hostname;
	
	@Getter
	private Theme theme;

	@Getter
	private final EventBus eventBus;

	protected SiteProperties siteProperties;

	protected ModuleManager moduleManager;

	protected final ServerProperties serverProperties;

	public VHost(final Path hostBase, final ServerProperties serverProperties) {
		this.eventBus = new DefaultEventBus();
		this.fileSystem = new FileSystem(hostBase, eventBus);
		this.serverProperties = serverProperties;
	}

	public void shutdown() {
		try {
			fileSystem.shutdown();
			extensionManager.close();
		} catch (Exception ex) {
			log.error("", ex);
		}
	}

	private Theme loadTheme() throws IOException {

		if (siteProperties.theme() != null) {
			Path themeFolder = serverProperties.getThemesFolder().resolve(siteProperties.theme());
			return DefaultTheme.load(themeFolder);
		}

		return DefaultTheme.EMPTY;
	}

	public void init(Path modules) throws IOException {

		fileSystem.init();

		var props = fileSystem.resolve("site.yaml");
		siteProperties = PropertiesLoader.hostProperties(props);

		theme = loadTheme();

		var classLoader = new ModuleAPIClassLoader(ClassLoader.getSystemClassLoader(),
				List.of(
						"org.slf4j",
						"com.github.thmarx.cms",
						"org.apache.logging",
						"org.graalvm.polyglot",
						"org.graalvm.js",
						"org.eclipse.jetty",
						"jakarta.servlet"
				));

		this.moduleManager = ModuleManagerImpl.create(modules.toFile(),
				fileSystem.resolve("modules_data").toFile(),
				new CMSModuleContext(siteProperties, serverProperties, fileSystem, eventBus,
						new RenderContentFunction(() -> contentResolver, () -> extensionManager, (context) -> resolveMarkdownRenderer()),
						theme
				),
				classLoader
		);

		hostname = siteProperties.hostname();

		contentBase = fileSystem.resolve("content/");
		assetBase = fileSystem.resolve("assets/");
		templateBase = fileSystem.resolve("templates/");

		extensionManager = new ExtensionManager(fileSystem, theme);
		extensionManager.init();

		contentParser = new ContentParser(fileSystem);

		contentRenderer = new ContentRenderer(contentParser, () -> resolveTemplateEngine(), fileSystem, siteProperties, () -> moduleManager, theme);
		contentResolver = new ContentResolver(contentBase, contentRenderer, fileSystem);

		this.moduleManager.initModules();
		
		List<String> activeModules = new ArrayList<>();
		activeModules.addAll(siteProperties.activeModules());
		if (!theme.empty()) {
			activeModules.addAll(theme.properties().activeModules());	
		}
		
		activeModules.stream()
				.filter(module_id -> moduleManager.getModuleIds().contains(module_id))
				.forEach(module_id -> {
					try {
						log.debug("activate module {}", module_id);
						moduleManager.activateModule(module_id);
					} catch (IOException ex) {
						log.error(null, ex);
					}
				});

		moduleManager.getModuleIds().stream()
				.filter(id -> !activeModules.contains(id))
				.forEach((module_id) -> {
					try {
						log.debug("deactivate module {}", module_id);
						moduleManager.deactivateModule(module_id);
					} catch (IOException ex) {
						log.error(null, ex);
					}
				});

		eventBus.register(ContentChangedEvent.class, (EventListener<ContentChangedEvent>) (ContentChangedEvent event) -> {
			log.debug("invalidate content cache");
			contentParser.clearCache();
		});
		eventBus.register(TemplateChangedEvent.class, (EventListener<TemplateChangedEvent>) (TemplateChangedEvent event) -> {
			log.debug("invalidate template cache");
			resolveTemplateEngine().invalidateCache();
		});
	}

	protected TemplateEngine resolveTemplateEngine() {
		if (this.templateEngine == null) {
			var engine = this.siteProperties.templateEngine();

			List<TemplateEngineProviderExtentionPoint> extensions = moduleManager.extensions(TemplateEngineProviderExtentionPoint.class);
			Optional<TemplateEngineProviderExtentionPoint> extOpt = extensions.stream().filter((ext) -> ext.getName().equals(engine)).findFirst();

			if (extOpt.isPresent()) {
				this.templateEngine = extOpt.get().getTemplateEngine();
			} else {
				throw new RuntimeException("no template engine found");
			}
		}

		return this.templateEngine;
	}

	protected MarkdownRenderer resolveMarkdownRenderer() {
		var engine = this.siteProperties.markdownEngine();

		List<MarkdownRendererProviderExtentionPoint> extensions = moduleManager.extensions(MarkdownRendererProviderExtentionPoint.class);
		Optional<MarkdownRendererProviderExtentionPoint> extOpt = extensions.stream().filter((ext) -> ext.getName().equals(engine)).findFirst();

		if (extOpt.isPresent()) {
			return extOpt.get().getRenderer();
		} else {
			throw new RuntimeException("no markdown renderer found");
		}
	}
}
