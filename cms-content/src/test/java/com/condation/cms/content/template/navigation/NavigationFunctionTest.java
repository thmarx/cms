package com.condation.cms.content.template.navigation;

/*-
 * #%L
 * CMS Content
 * %%
 * Copyright (C) 2023 - 2026 CondationCMS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */


import com.condation.cms.content.template.functions.navigation.NavigationFunction;
import com.condation.cms.api.content.ContentParser;
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.db.Content;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.DBFileSystem;
import com.condation.cms.api.feature.features.ContentNodeMapperFeature;
import com.condation.cms.api.feature.features.ContentParserFeature;
import com.condation.cms.api.feature.features.HookSystemFeature;
import com.condation.cms.api.feature.features.MarkdownRendererFeature;
import com.condation.cms.api.feature.features.SitePropertiesFeature;
import com.condation.cms.api.hooks.FilterContext;
import com.condation.cms.api.hooks.HookSystem;
import com.condation.cms.api.mapper.ContentNodeMapper;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.markdown.MarkdownRenderer;
import com.condation.cms.api.model.NavNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.filesystem.NIOReadOnlyFile;
import com.condation.cms.hooksystem.CMSHookSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;

/**
 *
 * @author t.marx
 */
@ExtendWith(MockitoExtension.class)
public class NavigationFunctionTest {

	RequestContext requestContext;
	HookSystem hookSystem;
	
	@Mock
	DB db;
	@Mock
	DBFileSystem fileSystem;
	@Mock
	Content content;
	@Mock
	ContentParser contentParser;
	@Mock
	MarkdownRenderer markdownRenderer;
	@Mock
	ContentNodeMapper contentNodeMapper;
	@Mock
	ContentRepository contentRepository;
	
	NavigationFunction sut;
	
	@BeforeEach
	public void setup () {
		
		var contentBase = Path.of("content/");
		
		Mockito.lenient().when(db.getFileSystem()).thenReturn(fileSystem);
		Mockito.lenient().when(db.getContent()).thenReturn(content);
		Mockito.lenient().when(fileSystem.resolve("content/")).thenReturn(contentBase);
		Mockito.lenient().when(fileSystem.contentBase()).thenReturn(new NIOReadOnlyFile(contentBase, contentBase.getParent())
		);
		Mockito.lenient().when(content.byUri("current")).thenReturn(Optional.empty());
		
		
		hookSystem = new CMSHookSystem();
		requestContext = new RequestContext();
		requestContext.add(HookSystemFeature.class, new HookSystemFeature(hookSystem));
		requestContext.add(ContentParserFeature.class, new ContentParserFeature(contentParser));
		requestContext.add(MarkdownRendererFeature.class, new MarkdownRendererFeature(markdownRenderer));
		requestContext.add(ContentNodeMapperFeature.class, new ContentNodeMapperFeature(contentNodeMapper));
		var siteProperties = mock(SiteProperties.class);
		Mockito.lenient().when(siteProperties.contextPath()).thenReturn("/");
		requestContext.add(SitePropertiesFeature.class, new SitePropertiesFeature(siteProperties));
		
		sut = new NavigationFunction(contentRepository,
				new ContentNode("current/index.md", "/current", "index.md", Map.of()),
				requestContext);
	}
	
	@Test
	public void test_hook_path() {
		var hookCalled = new AtomicBoolean(false);
		hookSystem.registerFilter("system/navigation/test/path", (FilterContext<List<NavNode>> parameters) -> {
			hookCalled.set(true);
			
			var nodes = parameters.value();
			
			nodes.add(0, new NavNode("test", "test", false));
			
			return nodes;
		});
		
		var nodes = sut.named("test").path();
		
		Assertions.assertThat(hookCalled).isTrue();
		Assertions.assertThat(nodes).containsExactly(
				new NavNode("test", "test", false),
				new NavNode("index.md", "/current", true)
		);
	}
	
	@Test
	public void test_hook_list() {
	}
}
