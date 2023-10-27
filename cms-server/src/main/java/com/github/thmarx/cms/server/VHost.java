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
import com.github.thmarx.cms.ContentParser;
import com.github.thmarx.cms.ContentRenderer;
import com.github.thmarx.cms.ContentResolver;
import com.github.thmarx.cms.api.HostProperties;
import com.github.thmarx.cms.PropertiesLoader;
import com.github.thmarx.cms.api.CMSModuleContext;
import com.github.thmarx.cms.api.extensions.MarkdownRendererProviderExtentionPoint;
import com.github.thmarx.cms.eventbus.EventBus;
import com.github.thmarx.cms.eventbus.EventListener;
import com.github.thmarx.cms.eventbus.events.ContentChangedEvent;
import com.github.thmarx.cms.eventbus.events.TemplateChangedEvent;
import com.github.thmarx.cms.filesystem.FileSystem;
import com.github.thmarx.cms.extensions.ExtensionManager;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.markdown.MarkedMarkdownRenderer;
import com.github.thmarx.cms.template.TemplateEngine;
import com.github.thmarx.cms.template.freemarker.FreemarkerTemplateEngine;
import com.github.thmarx.cms.template.pebble.PebbleTemplateEngine;
import com.github.thmarx.cms.template.thymeleaf.ThymeleafTemplateEngine;
import com.github.thmarx.modules.api.ModuleManager;
import com.github.thmarx.modules.manager.ModuleAPIClassLoader;
import com.github.thmarx.modules.manager.ModuleManagerImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;

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
	private final EventBus eventBus;

	protected HostProperties properties;
	
	protected ModuleManager moduleManager;

	public VHost(final Path hostBase) {
		this.eventBus = new EventBus();
		this.fileSystem = new FileSystem(hostBase, eventBus);
	}

	public void shutdown() {
		try {
			fileSystem.shutdown();
			extensionManager.close();
		} catch (Exception ex) {
			log.error("", ex);
		}
	}

	public void init(Path modules) throws IOException {

		fileSystem.init();
		
		var props = fileSystem.resolve("site.yaml");
		properties = PropertiesLoader.hostProperties(props);

		var classLoader = new ModuleAPIClassLoader(ClassLoader.getSystemClassLoader(), 
				List.of("org.slf4j", "com.github.thmarx.cms", "com.github.thmarx.moduls")
		);
        this.moduleManager = ModuleManagerImpl.create(
				modules.toFile(), 
				fileSystem.resolve("modules_data").toFile(), 
				new CMSModuleContext(properties), 
				classLoader
		);
		properties.activeModules().forEach(module_id -> {
			try {
				moduleManager.activateModule(module_id);
			} catch (IOException ex) {
				log.error(null, ex);
			}
		});
		
		hostname = properties.hostname();

		contentBase = fileSystem.resolve("content/");
		assetBase = fileSystem.resolve("assets/");
		templateBase = fileSystem.resolve("templates/");

		extensionManager = new ExtensionManager(fileSystem);
		extensionManager.init();

		contentParser = new ContentParser(fileSystem);

		templateEngine = resolveTemplateEngine();

		contentRenderer = new ContentRenderer(contentParser, templateEngine, fileSystem);
		contentResolver = new ContentResolver(contentBase, contentRenderer, fileSystem);

		eventBus.register(ContentChangedEvent.class, (EventListener<ContentChangedEvent>) (ContentChangedEvent event) -> {
			log.debug("invalidate content cache");
			contentParser.clearCache();
		});
		eventBus.register(TemplateChangedEvent.class, (EventListener<TemplateChangedEvent>) (TemplateChangedEvent event) -> {
			log.debug("invalidate template cache");
			templateEngine.invalidateCache();
		});
	}

	protected TemplateEngine resolveTemplateEngine() {
		var engine = this.properties.templateEngine();
		return switch (engine) {
			case "thymeleaf" ->
				new ThymeleafTemplateEngine(fileSystem, contentParser);
			case "pebble" ->
				new PebbleTemplateEngine(fileSystem, contentParser);
			default ->
				new FreemarkerTemplateEngine(fileSystem, contentParser);
		};
	}

	protected MarkdownRenderer resolveMarkdownRenderer(final Context context) {
		var engine = this.properties.markdownEngine();
		
		List<MarkdownRendererProviderExtentionPoint> extensions = moduleManager.extensions(MarkdownRendererProviderExtentionPoint.class);
		Optional<MarkdownRendererProviderExtentionPoint> extOpt = extensions.stream().filter((ext) -> ext.getName().equals(engine)).findFirst();
		
		if (extOpt.isPresent()) {
			return extOpt.get().getRenderer();
		} else {
			return new MarkedMarkdownRenderer(context);
		}
	}

	
}