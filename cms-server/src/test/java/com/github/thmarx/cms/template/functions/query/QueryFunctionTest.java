package com.github.thmarx.cms.template.functions.query;

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
import com.github.thmarx.cms.template.functions.navigation.NavigationFunction;
import java.io.IOException;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author t.marx
 */
public class QueryFunctionTest {

	static QueryFunction query;
	private static FileSystem fileSystem;
	static MarkdownRenderer markdownRenderer = TestHelper.getRenderer();

	@BeforeAll
	static void init() throws IOException {
		var contentParser = new ContentParser();
		fileSystem = new FileSystem(Path.of("hosts/test"), new DefaultEventBus(), (file) -> {
			try {
				return contentParser.parseMeta(file);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		fileSystem.init();
		query = new QueryFunction(fileSystem, Path.of("hosts/test/content/nav/index.md"), new ContentParser(),
				markdownRenderer);
	}

	@Test
	public void testSomeMethod() {

		Assertions.assertThat(query.toUrl("index.md")).isEqualTo("/");
		Assertions.assertThat(query.toUrl("test.md")).isEqualTo("/test");
		Assertions.assertThat(query.toUrl("demo/test.md")).isEqualTo("/demo/test");
		Assertions.assertThat(query.toUrl("demo/index.md")).isEqualTo("/demo");

	}

}