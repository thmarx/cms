package com.github.thmarx.cms.server;

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
import com.github.thmarx.cms.content.ContentResolver;
import com.github.thmarx.cms.api.SiteProperties;
import com.github.thmarx.cms.api.configuration.Config;
import com.github.thmarx.cms.api.module.CMSModuleContext;
import com.github.thmarx.cms.api.configuration.Configuration;
import com.github.thmarx.cms.api.configuration.configs.SiteConfiguration;
import com.github.thmarx.cms.api.content.ContentParser;
import com.github.thmarx.cms.api.eventbus.EventBus;
import com.github.thmarx.cms.api.eventbus.EventListener;
import com.github.thmarx.cms.api.eventbus.events.ConfigurationFileChanged;
import com.github.thmarx.cms.api.eventbus.events.ContentChangedEvent;
import com.github.thmarx.cms.api.eventbus.events.SitePropertiesChanged;
import com.github.thmarx.cms.api.eventbus.events.TemplateChangedEvent;
import com.github.thmarx.cms.extensions.ExtensionManager;
import com.github.thmarx.cms.api.feature.features.ContentRenderFeature;
import com.github.thmarx.cms.api.template.TemplateEngine;
import com.github.thmarx.cms.api.theme.Theme;
import com.github.thmarx.cms.filesystem.FileDB;
import com.github.thmarx.cms.module.RenderContentFunction;
import com.github.thmarx.cms.request.RequestContextFactory;
import com.github.thmarx.cms.server.jetty.modules.ModulesModule;
import com.github.thmarx.cms.server.jetty.modules.SiteHandlerModule;
import com.github.thmarx.cms.server.jetty.modules.SiteModule;
import com.github.thmarx.cms.server.jetty.modules.ThemeModule;
import com.github.thmarx.modules.api.ModuleManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public class VHost {

	protected final Configuration configuration;

	private final Path hostBase;

	@Getter
	protected Injector injector;

	public VHost(final Path hostBase, final Configuration configuration) {
		this.hostBase = hostBase;
		this.configuration = configuration;
	}

	public void shutdown() {
		try {
			injector.getInstance(FileDB.class).close();
			injector.getInstance(ExtensionManager.class).close();
		} catch (Exception ex) {
			log.error("", ex);
		}
	}

	public void reloadConfiguration(Class<? extends Config> configToReload) {
		configuration.reload(configToReload);
		if (SiteConfiguration.class.equals(configToReload)) {
			injector.getInstance(EventBus.class).publish(new SitePropertiesChanged());
		}
	}

	public List<String> hostnames() {
		return injector.getInstance(SiteProperties.class).hostnames();
	}

	public void init(Path modulesPath) throws IOException {
		this.injector = Guice.createInjector(new SiteModule(hostBase, configuration),
				new ModulesModule(modulesPath), new SiteHandlerModule(), new ThemeModule());

		final CMSModuleContext cmsModuleContext = injector.getInstance(CMSModuleContext.class);
		var moduleManager = injector.getInstance(ModuleManager.class);
		var contentResolver = injector.getInstance(ContentResolver.class);
		var requestContextFactory = injector.getInstance(RequestContextFactory.class);

		cmsModuleContext.add(
				ContentRenderFeature.class,
				new ContentRenderFeature(new RenderContentFunction(() -> contentResolver, () -> requestContextFactory))
		);

		moduleManager.initModules();
		List<String> activeModules = getActiveModules();
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

		injector.getInstance(EventBus.class).register(ContentChangedEvent.class, (EventListener<ContentChangedEvent>) (ContentChangedEvent event) -> {
			log.debug("invalidate content cache");
			injector.getInstance(ContentParser.class).clearCache();
		});
		injector.getInstance(EventBus.class).register(TemplateChangedEvent.class, (EventListener<TemplateChangedEvent>) (TemplateChangedEvent event) -> {
			log.debug("invalidate template cache");
			injector.getInstance(TemplateEngine.class).invalidateCache();
		});
		injector.getInstance(EventBus.class).register(ConfigurationFileChanged.class,
				(event) -> reloadConfiguration(event.clazz()));
	}

	protected List<String> getActiveModules() {
		List<String> activeModules = new ArrayList<>();
		activeModules.addAll(injector.getInstance(SiteProperties.class).activeModules());
		var theme = injector.getInstance(Theme.class);
		if (!theme.empty()) {
			activeModules.addAll(theme.properties().activeModules());
		}
		return activeModules;
	}
}
