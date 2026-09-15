package com.condation.cms.template.functions.query;

/*-
 * #%L
 * integration-tests
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


import com.condation.cms.TestDirectoryUtils;
import com.condation.cms.TestHelper;
import com.condation.cms.api.Constants;
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.configuration.Configuration;
import com.condation.cms.api.configuration.configs.SiteConfiguration;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.mapper.ContentNodeMapper;
import com.condation.cms.api.markdown.MarkdownRenderer;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.content.DefaultContentParser;
import com.condation.cms.content.template.functions.query.QueryFunction;
import com.condation.cms.core.eventbus.DefaultEventBus;
import com.condation.cms.filesystem.FileDB;
import com.condation.cms.filesystem.FileSystemContentRepository;
import com.condation.cms.filesystem.FileSystemContentStore;
import com.condation.cms.filesystem.NIOReadOnlyFile;
import java.io.IOException;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 *
 * @author t.marx
 */
public class QueryFunctionTest {

	static QueryFunction query;
	private static FileDB db;
	private static ContentRepository contentRepository;
	static MarkdownRenderer markdownRenderer = TestHelper.getRenderer();

	@AfterAll
	static void close () throws Exception {
		db.close();
	}
	@BeforeAll
	static void init() throws IOException {
		var hostBase =  Path.of("target/test-" + System.nanoTime());
		TestDirectoryUtils.copyDirectory(Path.of("hosts/test"), hostBase);
		
		var contentParser = new DefaultContentParser();
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
		defaultContentParser = new DefaultContentParser();
		query = new QueryFunction(contentRepository,
				contentRepository.get("nav/index.md").orElseThrow(),
				TestHelper.requestContext("/", defaultContentParser, markdownRenderer, new ContentNodeMapper(contentRepository)));
	}
	protected static DefaultContentParser defaultContentParser;

	@Test
	public void testSomeMethod() {

		Assertions.assertThat(query.toUrl("index.md")).isEqualTo("/");
		Assertions.assertThat(query.toUrl("test.md")).isEqualTo("/test");
		Assertions.assertThat(query.toUrl("demo/test.md")).isEqualTo("/demo/test");
		Assertions.assertThat(query.toUrl("demo/index.md")).isEqualTo("/demo");

	}

}
