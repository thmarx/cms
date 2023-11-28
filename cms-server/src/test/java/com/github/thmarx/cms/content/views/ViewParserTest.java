package com.github.thmarx.cms.content.views;

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
import com.github.thmarx.cms.TestHelper;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.content.ContentParser;
import com.github.thmarx.cms.eventbus.DefaultEventBus;
import com.github.thmarx.cms.filesystem.FileSystem;
import com.github.thmarx.cms.api.content.Page;
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
import org.junit.jupiter.api.Test;

/**
 *
 * @author t.marx
 */
public class ViewParserTest {
	
	static FileSystem fileSystem;
	static ContentParser parser = new ContentParser();
	static MarkdownRenderer markdownRenderer = TestHelper.getRenderer();
	
	@BeforeAll
	static void setup () throws IOException {
		
		fileSystem = new FileSystem(Path.of("hosts/test"), new DefaultEventBus(), (file) -> {
			try {
				return parser.parseMeta(file);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		fileSystem.init();
	}
	@AfterAll
	static void close () {
		fileSystem.shutdown();
	}
	
	@Test
	public void test_query () throws Exception {
		final Path currentNode = fileSystem.resolve("content/query/view.yaml");		
		var view = ViewParser.parse(currentNode, Map.of());		
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
				
				var page = view.getNodes(fileSystem, currentNode, parser, markdownRenderer, context, queryParams);
				
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
		final Path currentNode = fileSystem.resolve("content/view/view.yaml");		
		var view = ViewParser.parse(currentNode, Map.of());		
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
				var page = view.getNodes(fileSystem, currentNode, parser, markdownRenderer, context, queryParams);
				
				Assertions.assertThat(page)
						.isNotNull()
						.isNotEqualTo(Page.EMPTY);
				Assertions.assertThat(page.getItems()).hasSize(2);
			}
		}
		
	}

	@Test
	public void test() throws IOException, URISyntaxException {
		var view = ViewParser.parse(Path.of(ViewParser.class.getResource("view-nodelist.yaml").toURI()), Map.of());
		Assertions.assertThat(view.getTemplate()).isEqualTo("views/test.html");

		Assertions.assertThat(view.getContent().getNodelist().getFrom()).isEqualTo("./");
		Assertions.assertThat(view.getContent().getNodelist().getExcerpt()).isEqualTo("250");
		Assertions.assertThat(view.getContent().getNodelist().getIndex()).isEqualTo("false");
		Assertions.assertThat(view.getContent().getNodelist().getReverse()).isEqualTo("true");
		Assertions.assertThat(view.getContent().getNodelist().getSize()).isEqualTo("15");
		Assertions.assertThat(view.getContent().getNodelist().getSort()).isEqualTo("published");
		Assertions.assertThat(view.getContent().getNodelist().getPage()).isEqualTo("queryParams.getOrDefault('page', 1)");
	}
	
	
}
