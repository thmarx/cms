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
import com.github.thmarx.cms.api.ServerProperties;
import com.github.thmarx.cms.media.MediaManager;
import com.github.thmarx.cms.server.jetty.handler.JettyDefaultHandler;
import com.github.thmarx.cms.server.jetty.handler.JettyExtensionHandler;
import com.github.thmarx.cms.server.VHost;
import com.github.thmarx.cms.server.jetty.handler.JettyMediaHandler;
import com.github.thmarx.cms.server.jetty.handler.JettyModuleMappingHandler;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

	public JettyVHost(Path hostBase, ServerProperties serverProperties) {
		super(hostBase, serverProperties);
	}

	public Handler httpHandler() {
		
		var defaultHandler = new JettyDefaultHandler(contentResolver, requestContextFactory);

		log.debug("create assets handler for {}", assetBase.toString());
		ResourceHandler assetsHandler = new ResourceHandler();
		assetsHandler.setDirAllowed(false);
		assetsHandler.setBaseResource(new FileFolderPathResource(assetBase));
		if (serverProperties.dev()) {
			assetsHandler.setCacheControl("no-cache");
		} else {
			assetsHandler.setCacheControl("max-age=" + TimeUnit.HOURS.toSeconds(24));
		}

		ResourceHandler faviconHandler = new ResourceHandler();
		faviconHandler.setDirAllowed(false);
		faviconHandler.setBaseResource(new FileFolderPathResource(assetBase.resolve("favicon.ico")));

		PathMappingsHandler pathMappingsHandler = new PathMappingsHandler();
		pathMappingsHandler.addMapping(PathSpec.from("/"), defaultHandler);
		pathMappingsHandler.addMapping(PathSpec.from("/assets/*"), assetsHandler);
		pathMappingsHandler.addMapping(PathSpec.from("/favicon.ico"), faviconHandler);
		
		JettyMediaHandler mediaHandler = new JettyMediaHandler(new MediaManager(assetBase, fileSystem.resolve("temp"), getTheme(), siteProperties));
		pathMappingsHandler.addMapping(PathSpec.from("/media/*"), mediaHandler);

		ContextHandler defaultContextHandler = new ContextHandler(pathMappingsHandler, "/");
		defaultContextHandler.setVirtualHosts(List.of(siteProperties.hostname()));

		var moduleHandler = new JettyModuleMappingHandler(moduleManager, siteProperties);
		moduleHandler.init();
		ContextHandler moduleContextHandler = new ContextHandler(moduleHandler, "/module");
		var extensionHandler = new JettyExtensionHandler(requestContextFactory);
		ContextHandler extensionContextHandler = new ContextHandler(extensionHandler, "/extension");

		ContextHandlerCollection contextCollection = new ContextHandlerCollection(
				defaultContextHandler,
				moduleContextHandler,
				extensionContextHandler
		);
		
		if (!getTheme().empty()) {
			contextCollection.addHandler(themeContextHandler());
		}

		GzipHandler gzipHandler = new GzipHandler(contextCollection);
		gzipHandler.setMinGzipSize(1024);
		gzipHandler.addIncludedMimeTypes("text/plain");
		gzipHandler.addIncludedMimeTypes("text/html");
		gzipHandler.addIncludedMimeTypes("text/css");
		gzipHandler.addIncludedMimeTypes("application/javascript");

		return gzipHandler;
	}
	
	private ContextHandler themeContextHandler () {
		ResourceHandler assetsHandler = new ResourceHandler();
		assetsHandler.setDirAllowed(false);
		assetsHandler.setBaseResource(new FileFolderPathResource(getTheme().assetsPath()));
		if (serverProperties.dev()) {
			assetsHandler.setCacheControl("no-cache");
		} else {
			assetsHandler.setCacheControl("max-age=" + TimeUnit.HOURS.toSeconds(24));
		}
		PathMappingsHandler pathMappingsHandler = new PathMappingsHandler();
		pathMappingsHandler.addMapping(PathSpec.from("/assets/*"), assetsHandler);
		
		JettyMediaHandler mediaHandler = new JettyMediaHandler(new MediaManager(getTheme().assetsPath(), fileSystem.resolve("temp"), getTheme(), siteProperties));
		pathMappingsHandler.addMapping(PathSpec.from("/media/*"), mediaHandler);
		
		return new ContextHandler(pathMappingsHandler, "/themes/" + getTheme().getName());
	}
}
