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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author t.marx
 */
public class ContentParserNGTest {
	
	public ContentParserNGTest() {
	}

	@Test
	public void testSomeMethod() throws IOException {
		var contentParser = new DefaultContentParser();
		
		var expectedMD = """
                   
                   Und hier der Inhalt
                   """;
		
		var content = contentParser.parse(Path.of("hosts/test/content/test.md"));
		
		Assertions.assertThat(content.meta()).containsKeys("title", "tags", "template");
		Assertions.assertThat(content.meta().get("title")).isEqualTo("StartseiteView");
		Assertions.assertThat(content.meta().get("tags")).isInstanceOf(List.class)
				.asList().containsExactly("eins", "zwei", "drei");
		Assertions.assertThat(content.content()).isEqualToIgnoringWhitespace(expectedMD);
	}
	
	@Test
	public void test_date() throws IOException {
		var contentParser = new DefaultContentParser();
		
		var content = contentParser.parse(Path.of("hosts/test/content/test.md"));
		
		Assertions.assertThat(content.meta().get("date")).isNotNull().isInstanceOf(Date.class);
		Assertions.assertThat((Date)content.meta().get("date"))
				.isAfter("2023-12-01")
				.isBefore("2023-12-03");
		
		Assertions.assertThat(content.meta().get("datetime")).isNotNull().isInstanceOf(Date.class);
		Assertions.assertThat((Date)content.meta().get("datetime"))
				.isAfter("2023-12-02T12:10:12")
				.isBefore("2023-12-02T15:10:12");
		
	}
	
	@Test
	public void test_tags() throws IOException {
		var contentParser = new DefaultContentParser();
		
		var content = contentParser.parse(Path.of("hosts/test/content/tags.md"));
		
		Assertions.assertThat(content.meta()).containsKey("tags");
		Assertions.assertThat(content.meta().get("tags")).isInstanceOf(List.class);
		Assertions.assertThat(content.meta().get("tags")).asList().containsExactly("eins", "zwei", "drei");
	}
}
