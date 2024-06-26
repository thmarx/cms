package com.github.thmarx.cms.server.configs;

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
import com.github.thmarx.cms.api.theme.Theme;
import com.github.thmarx.cms.media.SiteMediaManager;
import com.github.thmarx.cms.server.handler.auth.JettyAuthenticationHandler;
import com.github.thmarx.cms.server.jetty.FileFolderPathResource;
import com.github.thmarx.cms.server.handler.content.JettyContentHandler;
import com.github.thmarx.cms.server.handler.extensions.JettyExtensionHandler;
import com.github.thmarx.cms.server.handler.media.JettyMediaHandler;
import com.github.thmarx.cms.server.handler.module.JettyHttpHandlerHandler;
import com.github.thmarx.cms.server.handler.module.JettyRouteHandler;
import com.github.thmarx.cms.server.handler.module.JettyRoutesHandler;
import com.github.thmarx.cms.server.handler.content.JettyTaxonomyHandler;
import com.github.thmarx.cms.server.handler.content.JettyViewHandler;
import com.github.thmarx.cms.server.handler.extensions.JettyExtensionRouteHandler;
import com.github.thmarx.cms.utils.SiteUtils;
import com.github.thmarx.modules.api.ModuleManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.server.handler.ResourceHandler;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
public class SiteHandlerModule extends AbstractModule {

	@Override
	protected void configure() {
		
		bind(JettyViewHandler.class).in(Singleton.class);
		bind(JettyContentHandler.class).in(Singleton.class);
		bind(JettyTaxonomyHandler.class).in(Singleton.class);
		bind(JettyRouteHandler.class).in(Singleton.class);
		bind(JettyRoutesHandler.class).in(Singleton.class);
		
		bind(JettyExtensionHandler.class).in(Singleton.class);
		bind(JettyExtensionRouteHandler.class).in(Singleton.class);
		
		bind(JettyAuthenticationHandler.class).in(Singleton.class);
	}
	
	@Provides
	@Singleton
	public JettyHttpHandlerHandler moduleHandler(Theme theme, ModuleManager moduleManager, SiteProperties siteProperties) throws IOException {
		return new JettyHttpHandlerHandler(moduleManager, SiteUtils.getActiveModules(siteProperties, theme));
	}
	
	@Provides
	@Singleton
	@Named("site")
	public JettyMediaHandler mediaHandler(SiteMediaManager mediaManager) throws IOException {
		return new JettyMediaHandler(mediaManager);
	}

	@Provides
	@Singleton
	@Named("site")
	public ResourceHandler resourceHander (@Named("assets") Path assetBase, ServerProperties serverProperties) throws IOException {
		ResourceHandler assetsHandler = new ResourceHandler();
		assetsHandler.setDirAllowed(false);
		assetsHandler.setBaseResource(new FileFolderPathResource(assetBase));
		if (serverProperties.dev()) {
			assetsHandler.setCacheControl("no-cache");
		} else {
			assetsHandler.setCacheControl("max-age=" + TimeUnit.HOURS.toSeconds(24));
		}
		
		return assetsHandler;
	}
}
