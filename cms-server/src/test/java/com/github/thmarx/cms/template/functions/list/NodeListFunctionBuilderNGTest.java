package com.github.thmarx.cms.template.functions.list;

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

import com.github.thmarx.cms.api.content.Node;
import com.github.thmarx.cms.api.content.Page;
import com.github.thmarx.cms.content.ContentParser;
import com.github.thmarx.cms.TestHelper;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.eventbus.DefaultEventBus;
import com.github.thmarx.cms.filesystem.FileSystem;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author t.marx
 */
public class NodeListFunctionBuilderNGTest {
	
	static NodeListFunctionBuilder nodeList;
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
		nodeList = new NodeListFunctionBuilder(fileSystem, fileSystem.resolve("content/").resolve("index.md"), parser, markdownRenderer);
	}
	@AfterAll
	static void close () {
		fileSystem.shutdown();
	}

	@Test
	public void test_blog_entry() {
		Page<Node> page = nodeList.from("/blog/*").page(1).size(10).list();
		Assertions.assertThat(page.getItems()).hasSize(2);
	}
	
	@Test
	public void test_blog_entry_sorted() {
		Page<Node> page = nodeList.from("/blog/*")
				.page(1).size(10)
				.sort("published")
				.list();
		Assertions.assertThat(page.getItems()).hasSize(2);
		Assertions.assertThat(page.getItems().get(0).name()).isEqualTo("September");
		Assertions.assertThat(page.getItems().get(1).name()).isEqualTo("Oktober");
	}
	
	@Test
	public void test_blog_entry_sorted_reverse() {
		Page<Node> page = nodeList.from("/blog/*")
				.page(1).size(10)
				.sort("published")
				.reverse(true)
				.list();
		Assertions.assertThat(page.getItems()).hasSize(2);
		Assertions.assertThat(page.getItems().get(0).name()).isEqualTo("Oktober");
		Assertions.assertThat(page.getItems().get(1).name()).isEqualTo("September");
	}
	
	@Test
	public void test_prodcuts_with_index() {
		Page<Node> page = nodeList.from("/products").page(1).size(10).index(true).list();
		Assertions.assertThat(page.getItems()).hasSize(2);
	}
	
	@Test
	public void test_prodcuts_without_index() {
		Page<Node> page = nodeList.from("/products").page(1).size(10).index(false).list();
		Assertions.assertThat(page.getItems()).hasSize(1);
	}
	
	
	@Test
	void list_root () {
		Page<Node> page = nodeList.from("/nodelist").page(1).size(10).list();
		var nodeUris = page.getItems().stream().map(Node::path).collect(Collectors.toList());
		Assertions.assertThat(nodeUris)
				.containsExactlyInAnyOrder(
						"/nodelist", 
						"/nodelist/folder1", 
						"/nodelist/folder2"
				);
	}
	
	@Test
	void list_folder1 () {
		Page<Node> page = nodeList.from("/nodelist/folder1").page(1).size(10).list();
		var nodeUris = page.getItems().stream().map(Node::path).collect(Collectors.toList());
		Assertions.assertThat(nodeUris)
				.containsExactlyInAnyOrder(
						"/nodelist/folder1", 
						"/nodelist/folder1/test"
				);
	}
	
	@Test
	void list_asterix () {
		Page<Node> page = nodeList.from("/nodelist/*").page(1).size(10).list();
		var nodeUris = page.getItems().stream().map(Node::path).collect(Collectors.toList());
		Assertions.assertThat(nodeUris)
				.containsExactlyInAnyOrder(
						"/nodelist/folder1", 
						"/nodelist/folder1/test",
						"/nodelist/folder2",
						"/nodelist/folder2/test"
				);
	}
	
	@Test
	void test_from_subfolder () {
		var nodeList = new NodeListFunctionBuilder(fileSystem, fileSystem.resolve("content/nodelist2/index.md"), parser, markdownRenderer);
		Page<Node> page = nodeList.from("./sub_folder/*").page(1).size(10).list();
		var nodeUris = page.getItems().stream().map(Node::path).collect(Collectors.toList());
		Assertions.assertThat(nodeUris)
				.containsExactlyInAnyOrder(
						"/nodelist2/sub_folder/folder1", 
						"/nodelist2/sub_folder/folder1/test",
						"/nodelist2/sub_folder/folder2",
						"/nodelist2/sub_folder/folder2/test"
				);
	}
}
