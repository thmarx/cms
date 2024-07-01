package com.github.thmarx.cms.filesystem;

/*-
 * #%L
 * cms-filesystem
 * %%
 * Copyright (C) 2023 - 2024 Marx-Software
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

import com.github.thmarx.cms.filesystem.metadata.query.ExtendableQuery;
import java.io.IOException;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author t.marx
 */
public abstract class AbstractFileSystemTest {
	
	protected abstract FileSystem getFileSystem ();
	
	@Test
	public void test_seconday_index() throws IOException {

//		var dimension = fileSystem.createDimension("featured", (ContentNode node) -> node.data().containsKey("featured") ? (Boolean) node.data().get("featured") : false, Boolean.class);

//		Assertions.assertThat(dimension.filter(Boolean.TRUE)).hasSize(2);
//		Assertions.assertThat(dimension.filter(Boolean.FALSE)).hasSize(1);
	}

	@Test
	public void test_query() throws IOException {
		var nodes = getFileSystem().query((node, i) -> node).where("featured", true).get();
		Assertions.assertThat(nodes).hasSize(2);
	}
	
	@Test
	public void test_query_in() throws IOException {
		var nodes = getFileSystem().query((node, i) -> node).whereIn("name", "test1", "test2").get();
		Assertions.assertThat(nodes).hasSize(2);
	}
	
	@Test
	public void test_query_not_in() throws IOException {
		var nodes = getFileSystem().query((node, i) -> node).whereNotIn("name", "test1", "test2").get();
		Assertions.assertThat(nodes).hasSize(0);
	}
	@Test
	public void test_query_contains() throws IOException {
		var nodes = getFileSystem().query((node, i) -> node).whereContains("taxonomy.tags", "eins").get();
		Assertions.assertThat(nodes).hasSize(1);
	}
	
	@Test
	public void test_query_contains_not() throws IOException {
		var nodes = getFileSystem().query((node, i) -> node).whereNotContains("taxonomy.tags", "eins").get();
		Assertions.assertThat(nodes).hasSize(1);
	}
	
	@Test
	public void test_lt_lte() throws IOException {
		var nodes = getFileSystem().query((node, i) -> node).where("number2", "<", 5).get();
		Assertions.assertThat(nodes).hasSize(0);
		
		nodes = getFileSystem().query((node, i) -> node).where("number2", "lte", 5).get();
		Assertions.assertThat(nodes).hasSize(1);
	}

	@Test
	public void test_query_with_start_uri() throws IOException {

		var nodes = getFileSystem().query("/test", (node, i) -> node).where("featured", true).get();
		
		Assertions.assertThat(nodes).hasSize(1);
		Assertions.assertThat(nodes.getFirst().uri()).isEqualTo("test/test1.md");
	}
	
	@Test
	public void test_custom_operation() throws IOException {

		var query = getFileSystem().query((node, i) -> node);
		var nodes = query.get();
		Assertions.assertThat(nodes).hasSize(3);
		
		query = getFileSystem().query((node, i) -> node);
		((ExtendableQuery)query).addCustomOperators("none", (value1, value2) -> false);
		nodes = query.where("featured", "none").get();
		Assertions.assertThat(nodes).hasSize(0);
	}
}
