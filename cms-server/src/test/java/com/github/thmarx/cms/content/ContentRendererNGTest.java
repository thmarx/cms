package com.github.thmarx.cms.content;

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

import com.github.thmarx.cms.MockModuleManager;
import com.github.thmarx.cms.TestHelper;
import com.github.thmarx.cms.TestTemplateEngine;
import com.github.thmarx.cms.api.SiteProperties;
import com.github.thmarx.cms.api.configuration.Configuration;
import com.github.thmarx.cms.eventbus.DefaultEventBus;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.api.template.TemplateEngine;
import com.github.thmarx.cms.filesystem.FileDB;
import com.github.thmarx.cms.template.TemplateEngineTest;
import com.github.thmarx.modules.api.ModuleManager;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author t.marx
 */
@ExtendWith(MockitoExtension.class)
public class ContentRendererNGTest extends TemplateEngineTest {
	
	static ContentRenderer contentRenderer;
	static MarkdownRenderer markdownRenderer;
	
	static ModuleManager moduleManager = new MockModuleManager();
	static FileDB db;
	
	@BeforeAll
	public static void beforeClass () throws IOException {
		var contentParser = new DefaultContentParser();
		var config = new Configuration(Path.of("hosts/test/"));
		db = new FileDB(Path.of("hosts/test/"), new DefaultEventBus(), (file) -> {
			try {
				return contentParser.parseMeta(file);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, config);
		db.init();
		markdownRenderer = TestHelper.getRenderer();
		TemplateEngine templates = new TestTemplateEngine(db);
		
		contentRenderer = new ContentRenderer(contentParser, 
				() -> templates, 
				db, 
				new SiteProperties(Map.of()), 
				moduleManager);
	}
	@AfterAll
	public static void shutdown () throws Exception {
		db.close();
	}

	@Test
	public void testSomeMethod() throws IOException {
		var expectedHTML = """
                     <html>
                     	<head>
                     		<title>StartseiteView</title>
                     	</head>
						<body>
                            Und hier der Inhalt
						</body>
                     </html>
                     """;
		var content = contentRenderer.render(Path.of("hosts/test/content/test.md"), TestHelper.requestContext());
		
		Assertions.assertThat(content).isEqualToIgnoringWhitespace(expectedHTML);
	}
	
	@Test
	public void test_subfolder() throws IOException {
		var expectedHTML = """
                     <html>
                     	<head>
                     		<title>ProduktSeite</title>
                     	</head>
						<body>
							Das ist ein Produkt!
                     	</body>
                     </html>
                     """;
		var content = contentRenderer.render(Path.of("hosts/test/content/products/test.md"), TestHelper.requestContext());
		
		Assertions.assertThat(content).isEqualToIgnoringWhitespace(expectedHTML);
	}
}
