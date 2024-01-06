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

import com.github.thmarx.cms.api.db.Page;
import com.github.thmarx.cms.content.DefaultContentParser;
import com.github.thmarx.cms.TestHelper;
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.configuration.Configuration;
import com.github.thmarx.cms.api.mapper.ContentNodeMapper;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.eventbus.DefaultEventBus;
import com.github.thmarx.cms.filesystem.FileDB;
import com.github.thmarx.cms.api.model.ListNode;
import com.github.thmarx.cms.filesystem.functions.list.NodeListFunctionBuilder;
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
	static FileDB db;
	
	static DefaultContentParser parser = new DefaultContentParser();
	static MarkdownRenderer markdownRenderer = TestHelper.getRenderer();
	
	@BeforeAll
	static void setup () throws IOException {
		var config = new Configuration(Path.of("hosts/test/"));
		db = new FileDB(Path.of("hosts/test"), new DefaultEventBus(), (file) -> {
			try {
				return parser.parseMeta(file);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, config);
		db.init();
		
		nodeList = new NodeListFunctionBuilder(db, db.getFileSystem().resolve("content/").resolve("index.md"), 
				TestHelper.requestContext("/", parser, markdownRenderer, new ContentNodeMapper(db, parser)));
	}
	@AfterAll
	static void close () throws Exception {
		db.close();
	}

	@Test
	public void test_blog_entry() {
		Page<ListNode> page = nodeList.from("/blog/*").page(1).size(10).list();
		Assertions.assertThat(page.getItems()).hasSize(2);
	}
	
	@Test
	public void test_blog_entry_sorted() {
		Page<ListNode> page = nodeList.from("/blog/*")
				.page(1).size(10)
				.sort(Constants.MetaFields.PUBLISH_DATE)
				.list();
		Assertions.assertThat(page.getItems()).hasSize(2);
		Assertions.assertThat(page.getItems().get(0).name()).isEqualTo("September");
		Assertions.assertThat(page.getItems().get(1).name()).isEqualTo("Oktober");
	}
	
	@Test
	public void test_blog_entry_sorted_reverse() {
		Page<ListNode> page = nodeList.from("/blog/*")
				.page(1).size(10)
				.sort(Constants.MetaFields.PUBLISH_DATE)
				.reverse(true)
				.list();
		Assertions.assertThat(page.getItems()).hasSize(2);
		Assertions.assertThat(page.getItems().get(0).name()).isEqualTo("Oktober");
		Assertions.assertThat(page.getItems().get(1).name()).isEqualTo("September");
	}
	
	@Test
	public void test_prodcuts_with_index() {
		Page<ListNode> page = nodeList.from("/products").page(1).size(10).index(true).list();
		Assertions.assertThat(page.getItems()).hasSize(2);
	}
	
	@Test
	public void test_prodcuts_without_index() {
		Page<ListNode> page = nodeList.from("/products").page(1).size(10).index(false).list();
		Assertions.assertThat(page.getItems()).hasSize(1);
	}
	
	
	@Test
	void list_root () {
		Page<ListNode> page = nodeList.from("/nodelist").page(1).size(10).list();
		var nodeUris = page.getItems().stream().map(ListNode::path).collect(Collectors.toList());
		Assertions.assertThat(nodeUris)
				.containsExactlyInAnyOrder(
						"/nodelist", 
						"/nodelist/folder1", 
						"/nodelist/folder2"
				);
	}
	
	@Test
	void list_folder1 () {
		Page<ListNode> page = nodeList.from("/nodelist/folder1").page(1).size(10).list();
		var nodeUris = page.getItems().stream().map(ListNode::path).collect(Collectors.toList());
		Assertions.assertThat(nodeUris)
				.containsExactlyInAnyOrder(
						"/nodelist/folder1", 
						"/nodelist/folder1/test"
				);
	}
	
	@Test
	void list_asterix () {
		Page<ListNode> page = nodeList.from("/nodelist/*").page(1).size(10).list();
		var nodeUris = page.getItems().stream().map(ListNode::path).collect(Collectors.toList());
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
		var nodeList = new NodeListFunctionBuilder(db, db.getFileSystem().resolve("content/nodelist2/index.md"), 
				TestHelper.requestContext("/", parser, markdownRenderer, new ContentNodeMapper(db, parser)));
		Page<ListNode> page = nodeList.from("./sub_folder/*").page(1).size(10).list();
		var nodeUris = page.getItems().stream().map(ListNode::path).collect(Collectors.toList());
		Assertions.assertThat(nodeUris)
				.containsExactlyInAnyOrder(
						"/nodelist2/sub_folder/folder1", 
						"/nodelist2/sub_folder/folder1/test",
						"/nodelist2/sub_folder/folder2",
						"/nodelist2/sub_folder/folder2/test"
				);
	}
	
	@Test
	void test_json () {
		var nodeList = new NodeListFunctionBuilder(db, db.getFileSystem().resolve("content/index.md"), 
				TestHelper.requestContext("/", parser, markdownRenderer, new ContentNodeMapper(db, parser)));
		Page<ListNode> page = nodeList.from("./json").page(1).size(10).list();
		Assertions.assertThat(page.getItems()).hasSize(1);
		Assertions.assertThat(page.getItems().getFirst().name()).isEqualTo("HTML");
		
		page = nodeList.from("./json").page(1).size(10).json().list();
		
		Assertions.assertThat(page.getItems()).hasSize(1);
		Assertions.assertThat(page.getItems().getFirst().name()).isEqualTo("JSON");
	}
}
