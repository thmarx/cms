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
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.content.ContentParser;
import com.github.thmarx.cms.filesystem.FileSystem;
import com.github.thmarx.cms.filesystem.MetaData;
import com.github.thmarx.cms.filesystem.query.Query;
import com.github.thmarx.cms.template.functions.AbstractCurrentNodeFunction;
import com.github.thmarx.cms.api.content.Node;
import com.github.thmarx.cms.utils.NodeUtil;
import java.nio.file.Path;
import java.util.function.BiFunction;

/**
 *
 * @author t.marx
 */
public class QueryFunction extends AbstractCurrentNodeFunction {

	BiFunction<MetaData.MetaNode, Integer, Node> nodeMapper = null;

	public QueryFunction(FileSystem fileSystem, Path currentNode, ContentParser contentParser, MarkdownRenderer markdownRenderer) {
		super(fileSystem, currentNode, contentParser, markdownRenderer);
	}
	
	private BiFunction<MetaData.MetaNode, Integer, Node> nodeMapper() {
		if (nodeMapper == null) {
			nodeMapper = (node, excerptLength) -> {
				var name = NodeUtil.getName(node);
				var temp_path = fileSystem.resolve("content/").resolve(node.uri());
				var url = toUrl(node.uri());
				var md = parse(temp_path);
				var excerpt = markdownRenderer.excerpt(md.get().content(), excerptLength);
				final Node navNode = new Node(name, url, excerpt, node.data());

				return navNode;
			};
		}

		return nodeMapper;
	}

	public Query create() {
		return fileSystem.query(nodeMapper());
	}

	public Query create(final String startUri) {
		return fileSystem.query(startUri, nodeMapper());
	}

	protected String toUrl(String uri) {
		if (uri.endsWith("index.md")) {
			uri = uri.replace("index.md", "");
		}
		if (uri.endsWith(".md")) {
			uri = uri.substring(0, uri.lastIndexOf(".md"));
		}

		if (!uri.startsWith("/")) {
			uri = "/" + uri;
		}
		if (uri.length() > 1 && uri.endsWith("/")) {
			uri = uri.substring(0, uri.length() - 1);
		}

		return uri;
	}
}
