package com.github.thmarx.cms;

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
import com.github.thmarx.cms.api.content.ContentParser;
import com.github.thmarx.cms.api.hooks.HookSystem;
import com.github.thmarx.cms.api.mapper.ContentNodeMapper;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.api.request.RequestContext;
import com.github.thmarx.cms.api.feature.features.HookSystemFeature;
import com.github.thmarx.cms.api.feature.features.InjectorFeature;
import com.github.thmarx.cms.api.feature.features.RequestFeature;
import com.github.thmarx.cms.api.feature.features.SiteMediaServiceFeature;
import com.github.thmarx.cms.api.feature.features.SitePropertiesFeature;
import com.github.thmarx.cms.content.ShortCodes;
import com.github.thmarx.cms.media.FileMediaService;
import com.github.thmarx.cms.request.RenderContext;
import com.github.thmarx.cms.request.RequestExtensions;
import com.github.thmarx.cms.theme.DefaultTheme;
import com.google.inject.Injector;
import java.util.Map;
import org.mockito.Mockito;

/**
 *
 * @author t.marx
 */
public abstract class TestHelper {

	public static MarkdownRenderer getRenderer() {
		return new TestMarkdownRenderer();
	}

	public static RequestContext requestContext() {
		return requestContext("");
	}

	public static RequestContext requestContext(String uri) {
		var markdownRenderer = TestHelper.getRenderer();
		RequestContext context = new RequestContext();
		context.add(RequestFeature.class, new RequestFeature(uri, Map.of()));
		context.add(RequestExtensions.class, new RequestExtensions(null, null));
		context.add(RenderContext.class, new RenderContext(markdownRenderer, new ShortCodes(Map.of()), DefaultTheme.EMPTY));

		context.add(SiteMediaServiceFeature.class, new SiteMediaServiceFeature(new FileMediaService(null)));
		context.add(InjectorFeature.class, new InjectorFeature(Mockito.mock(Injector.class)));
		context.add(HookSystemFeature.class, new HookSystemFeature(new HookSystem()));
		
		context.add(SitePropertiesFeature.class, new SitePropertiesFeature(new SiteProperties(Map.of(
				"context_path", "/"
		))));

		return context;
	}

	public static RequestContext requestContext(String uri, ContentParser contentParser, MarkdownRenderer markdownRenderer, ContentNodeMapper contentMapper) {

		RequestContext context = requestContext(uri);
		context.add(ContentParser.class, contentParser);
		context.add(MarkdownRenderer.class, markdownRenderer);
		context.add(ContentNodeMapper.class, contentMapper);
		context.add(HookSystemFeature.class, new HookSystemFeature(new HookSystem()));
		context.add(SitePropertiesFeature.class, new SitePropertiesFeature(new SiteProperties(Map.of(
				"context_path", "/"
		))));

		return context;
	}
}
