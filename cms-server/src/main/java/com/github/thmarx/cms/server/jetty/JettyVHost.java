package com.github.thmarx.cms.server.jetty;

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
import com.github.thmarx.cms.api.SiteProperties;
import com.github.thmarx.cms.api.configuration.Configuration;
import com.github.thmarx.cms.api.eventbus.EventBus;
import com.github.thmarx.cms.api.eventbus.events.SitePropertiesChanged;
import com.github.thmarx.cms.api.theme.Theme;
import com.github.thmarx.cms.media.MediaManager;
import com.github.thmarx.cms.request.RequestContextFactory;
import com.github.thmarx.cms.server.jetty.handler.JettyContentHandler;
import com.github.thmarx.cms.server.jetty.handler.JettyExtensionHandler;
import com.github.thmarx.cms.server.VHost;
import com.github.thmarx.cms.server.jetty.handler.JettyMediaHandler;
import com.github.thmarx.cms.server.jetty.handler.JettyModuleMappingHandler;
import com.github.thmarx.cms.server.jetty.handler.JettyRouteHandler;
import com.github.thmarx.cms.server.jetty.handler.JettyTaxonomyHandler;
import com.github.thmarx.cms.server.jetty.handler.JettyViewHandler;
import com.github.thmarx.modules.api.ModuleManager;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.PathMappingsHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

/**
 *
 * @author t.marx
 */
@Slf4j
public class JettyVHost extends VHost {

	public JettyVHost(Path hostBase, Configuration configuration) {
		super(hostBase, configuration);
	}

	public Handler httpHandler() {

		var contentHandler = injector.getInstance(JettyContentHandler.class);
		var taxonomyHandler = injector.getInstance(JettyTaxonomyHandler.class);
		var viewHandler = injector.getInstance(JettyViewHandler.class);
		var routeHandler = injector.getInstance(JettyRouteHandler.class);
		var defaultHandlerSequence = new Handler.Sequence(
				routeHandler, 
				viewHandler, 
				taxonomyHandler, 
				contentHandler
		);

		log.debug("create assets handler for site");
		ResourceHandler assetsHandler = injector.getInstance(Key.get(ResourceHandler.class, Names.named("site")));

		ResourceHandler faviconHandler = new ResourceHandler();
		faviconHandler.setDirAllowed(false);
		var assetBase = this.injector.getInstance(Key.get(Path.class, Names.named("assets")));
		faviconHandler.setBaseResource(new FileFolderPathResource(assetBase.resolve("favicon.ico")));

		PathMappingsHandler pathMappingsHandler = new PathMappingsHandler();
		pathMappingsHandler.addMapping(PathSpec.from("/"), defaultHandlerSequence);
		pathMappingsHandler.addMapping(PathSpec.from("/assets/*"), assetsHandler);
		pathMappingsHandler.addMapping(PathSpec.from("/favicon.ico"), faviconHandler);

		var assetsMediaManager = this.injector.getInstance(Key.get(MediaManager.class, Names.named("site")));
		injector.getInstance(EventBus.class).register(SitePropertiesChanged.class, assetsMediaManager);
		final JettyMediaHandler mediaHandler = this.injector.getInstance(Key.get(JettyMediaHandler.class, Names.named("site")));

		pathMappingsHandler.addMapping(PathSpec.from("/media/*"), mediaHandler);

		ContextHandler defaultContextHandler = new ContextHandler(
				pathMappingsHandler, 
				injector.getInstance(SiteProperties.class).contextPath()
		);
		defaultContextHandler.setVirtualHosts(injector.getInstance(SiteProperties.class).hostnames());

		var moduleHandler = new JettyModuleMappingHandler(
				injector.getInstance(ModuleManager.class), getActiveModules()
		);
		moduleHandler.init();
		ContextHandler moduleContextHandler = new ContextHandler(moduleHandler, appendContextIfNeeded("/module"));

		var extensionHandler = new JettyExtensionHandler(
				injector.getInstance(RequestContextFactory.class)
		);
		ContextHandler extensionContextHandler = new ContextHandler(extensionHandler, appendContextIfNeeded("/extension"));

		ContextHandlerCollection contextCollection = new ContextHandlerCollection(
				defaultContextHandler,
				moduleContextHandler,
				extensionContextHandler
		);

		if (!injector.getInstance(Theme.class).empty()) {
			contextCollection.addHandler(themeContextHandler());
		}

		JettyLoggingFilterHandler logContextHandler = new JettyLoggingFilterHandler(contextCollection, injector.getInstance(SiteProperties.class));

		GzipHandler gzipHandler = new GzipHandler(logContextHandler);
		gzipHandler.setMinGzipSize(1024);
		gzipHandler.addIncludedMimeTypes("text/plain");
		gzipHandler.addIncludedMimeTypes("text/html");
		gzipHandler.addIncludedMimeTypes("text/css");
		gzipHandler.addIncludedMimeTypes("application/javascript");

		return gzipHandler;
	}
	
	private String appendContextIfNeeded (final String path) {
		var contextPath = injector.getInstance(SiteProperties.class).contextPath();
		
		if ("/".equals(contextPath)) {
			return path;
		}
		
		return contextPath + path;
	}

	private ContextHandler themeContextHandler() {
		final MediaManager themeAssetsMediaManager = this.injector.getInstance(Key.get(MediaManager.class, Names.named("theme")));
		injector.getInstance(EventBus.class).register(SitePropertiesChanged.class, themeAssetsMediaManager);
		JettyMediaHandler mediaHandler = this.injector.getInstance(Key.get(JettyMediaHandler.class, Names.named("theme")));
		ResourceHandler assetsHandler = this.injector.getInstance(Key.get(ResourceHandler.class, Names.named("theme")));

		PathMappingsHandler pathMappingsHandler = new PathMappingsHandler();
		pathMappingsHandler.addMapping(PathSpec.from("/assets/*"), assetsHandler);
		pathMappingsHandler.addMapping(PathSpec.from("/media/*"), mediaHandler);

		return new ContextHandler(pathMappingsHandler, appendContextIfNeeded("/themes/" + injector.getInstance(Theme.class).getName()));
	}
}
