package com.github.thmarx.cms.server.jetty.modules;

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
import com.github.thmarx.cms.api.ServerProperties;
import com.github.thmarx.cms.api.SiteProperties;
import com.github.thmarx.cms.api.eventbus.EventBus;
import com.github.thmarx.cms.api.extensions.HookSystemRegisterExtentionPoint;
import com.github.thmarx.cms.api.extensions.MarkdownRendererProviderExtentionPoint;
import com.github.thmarx.cms.api.extensions.TemplateEngineProviderExtentionPoint;
import com.github.thmarx.cms.api.feature.features.DBFeature;
import com.github.thmarx.cms.api.feature.features.EventBusFeature;
import com.github.thmarx.cms.api.feature.features.ServerPropertiesFeature;
import com.github.thmarx.cms.api.feature.features.SitePropertiesFeature;
import com.github.thmarx.cms.api.feature.features.ThemeFeature;
import com.github.thmarx.cms.api.hooks.HookSystem;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.api.module.CMSModuleContext;
import com.github.thmarx.cms.api.template.TemplateEngine;
import com.github.thmarx.cms.api.theme.Theme;
import com.github.thmarx.cms.filesystem.FileDB;
import com.github.thmarx.modules.api.ModuleManager;
import com.github.thmarx.modules.manager.ModuleAPIClassLoader;
import com.github.thmarx.modules.manager.ModuleManagerImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class ModulesModule extends AbstractModule {

	private final Path modulesPath;

	@Override
	protected void configure() {
	}

	@Provides
	@Singleton
	public ModuleManager moduleManager(Injector injector, CMSModuleContext context) {
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
		return ModuleManagerImpl.builder()
				.activateModulesOnStartup(false)
				.setClassLoader(classLoader)
				.setInjector((instance) -> injector.injectMembers(instance))
				.setModulesDataPath(injector.getInstance(FileDB.class).getFileSystem().resolve("modules_data").toFile())
				.setModulesPath(modulesPath.toFile())
				.setContext(context)
				.build();
	}

	@Provides
	@Singleton
	public CMSModuleContext moduleContext(SiteProperties siteProperties, ServerProperties serverProperties, FileDB db, EventBus eventBus, Theme theme) {
		final CMSModuleContext cmsModuleContext = new CMSModuleContext();
		cmsModuleContext.add(SitePropertiesFeature.class, new SitePropertiesFeature(siteProperties));
		cmsModuleContext.add(ServerPropertiesFeature.class, new ServerPropertiesFeature(serverProperties));
		cmsModuleContext.add(DBFeature.class, new DBFeature(db));
		cmsModuleContext.add(EventBusFeature.class, new EventBusFeature(eventBus));
		cmsModuleContext.add(ThemeFeature.class, new ThemeFeature(theme));
		
		return cmsModuleContext;
	}

	/**
	 * The markedjs markdown renderer is implemented using graaljs, so we need a fresh instance for every request
	 * @param siteProperties
	 * @param moduleManager
	 * @return 
	 */
	@Provides
	public MarkdownRenderer markdownRenderer(SiteProperties siteProperties, ModuleManager moduleManager) {
		var engine = siteProperties.markdownEngine();

		List<MarkdownRendererProviderExtentionPoint> extensions = moduleManager.extensions(MarkdownRendererProviderExtentionPoint.class);
		Optional<MarkdownRendererProviderExtentionPoint> extOpt = extensions.stream().filter((ext) -> ext.getName().equals(engine)).findFirst();

		if (extOpt.isPresent()) {
			return extOpt.get().getRenderer();
		} else {
			throw new RuntimeException("no markdown renderer found");
		}
	}

	private String getTemplateEngine(SiteProperties siteProperties, Theme theme) {
		var engine = siteProperties.templateEngine();

		var theme_engine = theme.properties().templateEngine();
		if (theme_engine != null && engine != null && !theme_engine.equals(engine)) {
			throw new RuntimeException("site template engine does not match theme template engine");
		}

		return theme_engine != null ? theme_engine : engine;
	}

	@Provides
	@Singleton
	public TemplateEngine resolveTemplateEngine(SiteProperties siteProperties, Theme theme, ModuleManager moduleManager) {
		var engine = getTemplateEngine(siteProperties, theme);

		List<TemplateEngineProviderExtentionPoint> extensions = moduleManager.extensions(TemplateEngineProviderExtentionPoint.class);
		Optional<TemplateEngineProviderExtentionPoint> extOpt = extensions.stream().filter((ext) -> ext.getName().equals(engine)).findFirst();

		if (extOpt.isPresent()) {
			return extOpt.get().getTemplateEngine();
		} else {
			throw new RuntimeException("no template engine found");
		}
	}
	
	/**
	 * new HookSystem for each request
	 * @return 
	 */
	@Provides
	public HookSystem hookSystem(final ModuleManager moduleManager) {
		var hookSystem = new HookSystem();
		
		moduleManager.extensions(HookSystemRegisterExtentionPoint.class).forEach(extensionPoint -> extensionPoint.register(hookSystem));
		
		return hookSystem;
	}
}
