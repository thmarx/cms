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
import com.condation.cms.api.Constants;
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.configuration.Configuration;
import com.condation.cms.api.configuration.configs.SiteConfiguration;
import com.condation.cms.api.content.ContentParser;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.feature.features.ConfigurationFeature;
import com.condation.cms.api.feature.features.ContentNodeMapperFeature;
import com.condation.cms.api.feature.features.ContentParserFeature;
import com.condation.cms.api.feature.features.HookSystemFeature;
import com.condation.cms.api.feature.features.MarkdownRendererFeature;
import com.condation.cms.api.feature.features.SitePropertiesFeature;
import com.condation.cms.api.mapper.ContentNodeMapper;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.request.RequestContextScope;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.content.DefaultContentParser;
import com.condation.cms.content.markdown.module.CMSMarkdownRenderer;
import com.condation.cms.content.template.functions.navigation.NavigationFunction;
import com.condation.cms.core.eventbus.DefaultEventBus;
import com.condation.cms.filesystem.FileDB;
import com.condation.cms.filesystem.FileSystemContentRepository;
import com.condation.cms.filesystem.FileSystemContentStore;
import com.condation.cms.filesystem.NIOReadOnlyFile;
import com.condation.cms.hooksystem.CMSHookSystem;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author thorstenmarx
 */
@ExtendWith(MockitoExtension.class)
public class NavigationFunctionITest {

	private static FileDB db;
	private static ContentRepository contentRepository;

	@Mock
	private Request request;

	private RequestContext requestContext;

	private static ContentParser contentParser = new DefaultContentParser();

	@BeforeAll
	public static void setup() throws Exception {

		var hostBase = Path.of("src/test/resources/site");
		var config = new Configuration();
		var siteConfigMock = Mockito.mock(SiteConfiguration.class);
		var sitePropsMock = Mockito.mock(SiteProperties.class);
		Mockito.when(sitePropsMock.id()).thenReturn("test-site");
		Mockito.when(siteConfigMock.siteProperties()).thenReturn(sitePropsMock);
		config.add(SiteConfiguration.class, siteConfigMock);
		db = new FileDB(hostBase, new DefaultEventBus(), (file) -> {
			try {
				ReadOnlyFile cmsFile = new NIOReadOnlyFile(file, hostBase.resolve(Constants.Folders.CONTENT));
				return contentParser.parseMeta(cmsFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, config);
		db.init();
		contentRepository = new FileSystemContentRepository(
				db.getContent(), db.getFileSystem(),
				new FileSystemContentStore(db.getFileSystem()), contentParser);
	}

	@BeforeEach
	public void setupRequestContext() {
		requestContext = new RequestContext();
		var siteProperties = Mockito.mock(SiteProperties.class);
		Mockito.when(siteProperties.contextPath()).thenReturn("/");
		Mockito.when(siteProperties.defaultContentType()).thenReturn(Constants.ContentTypes.JSON);
		var siteConfiguration = new SiteConfiguration(siteProperties);

		var configuration = new Configuration();
		configuration.add(SiteConfiguration.class, siteConfiguration);
		ConfigurationFeature configFeature = new ConfigurationFeature(configuration);

		requestContext.add(ConfigurationFeature.class, configFeature);
		requestContext.add(SitePropertiesFeature.class, new SitePropertiesFeature(siteProperties));
		requestContext.add(ContentParserFeature.class, new ContentParserFeature(contentParser));
		requestContext.add(ContentNodeMapperFeature.class, new ContentNodeMapperFeature(new ContentNodeMapper(contentRepository)));
		requestContext.add(MarkdownRendererFeature.class, new MarkdownRendererFeature(new CMSMarkdownRenderer()));
		requestContext.add(HookSystemFeature.class, new HookSystemFeature(new CMSHookSystem()));

		Mockito.lenient().when(request.getAttribute("_requestContext")).thenReturn(requestContext);
	}

	@AfterAll
	public static void shutdown() throws Exception {
		db.close();
	}

	@Test
	void test_root() {
		ScopedValue.where(RequestContextScope.REQUEST_CONTEXT, requestContext).run(() -> {
			var currentNode = contentRepository.get("index.md").orElseThrow();
			NavigationFunction fn = new NavigationFunction(
					contentRepository, currentNode, requestContext);

			var nodes = fn.json().list(".");

			Assertions.assertThat(nodes)
					.isNotEmpty()
					.hasSize(3);
		});
	}
}
