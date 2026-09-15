package com.condation.cms.content.views;

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
import com.condation.cms.api.content.ContentParser;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.mapper.ContentNodeMapper;
import com.condation.cms.api.markdown.MarkdownRenderer;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.content.DefaultContentParser;
import com.condation.cms.core.eventbus.DefaultEventBus;
import com.condation.cms.filesystem.FileDB;
import com.condation.cms.filesystem.FileSystemContentRepository;
import com.condation.cms.filesystem.FileSystemContentStore;
import com.condation.cms.filesystem.NIOReadOnlyFile;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 *
 * @author t.marx
 */
public class ViewParserTest {
	
	static FileDB db;
	static ContentRepository contentRepository;
	static ContentParser parser = new DefaultContentParser();
	static MarkdownRenderer markdownRenderer = TestHelper.getRenderer();
	static RequestContext requestContext;
	
	@BeforeEach
	void setup_test () throws IOException {
		requestContext = TestHelper.requestContext("", parser, markdownRenderer, new ContentNodeMapper(contentRepository));
	}
	
	@BeforeAll
	static void setup () throws IOException {
		
		
		var hostBase = Path.of("target/test-" + System.currentTimeMillis());
		TestDirectoryUtils.copyDirectory(Path.of("hosts/test"), hostBase);
		
		var config = new Configuration();
		var siteConfigMock = Mockito.mock(SiteConfiguration.class);
		var sitePropsMock = Mockito.mock(SiteProperties.class);
		Mockito.when(sitePropsMock.id()).thenReturn("test-site");
		Mockito.when(siteConfigMock.siteProperties()).thenReturn(sitePropsMock);
		config.add(SiteConfiguration.class, siteConfigMock);
		db = new FileDB(hostBase, new DefaultEventBus(), (file) -> {
			try {
				ReadOnlyFile cmsFile = new NIOReadOnlyFile(file, hostBase.resolve(Constants.Folders.CONTENT));
				return parser.parseMeta(cmsFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, config);
		db.init();
		contentRepository = new FileSystemContentRepository(
				db.getContent(), db.getFileSystem(),
				new FileSystemContentStore(db.getFileSystem()), parser);
	}
	@AfterAll
	static void close () throws Exception {
		db.close();
	}
	
	@Test
	public void test_query () throws Exception {
		final ReadOnlyFile currentNode = db.getReadOnlyFileSystem().resolve("content/query/view.yaml");		
		var view = ViewParser.parse(currentNode.getContent());
		Assertions.assertThat(view).isNotNull();
	
		
		try (var engine = Engine.newBuilder()
				.option("engine.WarnInterpreterOnly", "false")
				.build()) {
			try (var context = Context.newBuilder("js")
					.allowAllAccess(true)
					.allowHostClassLookup(className -> true)
					.allowHostAccess(HostAccess.ALL)
					.engine(engine).build()) {
				Map<String, List<String>> queryParams = new HashMap<>(Map.of("page", List.of("1")));
				
				long before = System.currentTimeMillis();
				
				var page = view.getNodes(
						contentRepository,
						new ContentNode("query/view.yaml", "/query/view", "view.yaml", Map.of()),
						context, queryParams, requestContext);
				
				System.out.println("took %d ms".formatted((System.currentTimeMillis() - before)));
				
				Assertions.assertThat(page)
						.isNotNull()
						.isNotEqualTo(Page.EMPTY);
				Assertions.assertThat(page.getItems()).hasSize(1);
			}
		}
		
	}
	
	@Test
	public void test_nodelist () throws Exception {
		final ReadOnlyFile currentNode = db.getReadOnlyFileSystem().resolve("content/view/view.yaml");		
		var view = ViewParser.parse(currentNode.getContent());
		Assertions.assertThat(view).isNotNull();
	
		
		try (var engine = Engine.newBuilder()
				.option("engine.WarnInterpreterOnly", "false")
				.build()) {
			try (var context = Context.newBuilder("js")
					.allowAllAccess(true)
					.allowHostClassLookup(className -> true)
					.allowHostAccess(HostAccess.ALL)
					.engine(engine).build()) {
				Map<String, List<String>> queryParams = new HashMap<>(Map.of("page", List.of("1")));
				var page = view.getNodes(
						contentRepository,
						new ContentNode("view/view.yaml", "/view/view", "view.yaml", Map.of()),
						context, queryParams, requestContext);
				
				Assertions.assertThat(page)
						.isNotNull()
						.isNotEqualTo(Page.EMPTY);
				Assertions.assertThat(page.getItems()).hasSize(2);
			}
		}
		
	}

	@Test
	public void test() throws IOException, URISyntaxException {
		var view = ViewParser.parse(new NIOReadOnlyFile(
				Path.of(ViewParser.class.getResource("view-nodelist.yaml").toURI()), 
				Path.of("./")).getContent());
		Assertions.assertThat(view.getTemplate()).isEqualTo("views/test.html");

		Assertions.assertThat(view.getContent().getNodelist().getFrom()).isEqualTo("./");
		Assertions.assertThat(view.getContent().getNodelist().getExcerpt()).isEqualTo("250");
		Assertions.assertThat(view.getContent().getNodelist().getIndex()).isEqualTo("false");
		Assertions.assertThat(view.getContent().getNodelist().getReverse()).isEqualTo("true");
		Assertions.assertThat(view.getContent().getNodelist().getSize()).isEqualTo("15");
		Assertions.assertThat(view.getContent().getNodelist().getSort()).isEqualTo("status");
		Assertions.assertThat(view.getContent().getNodelist().getPage()).isEqualTo("queryParams.getOrDefault('page', 1)");
	}
	
	
}
