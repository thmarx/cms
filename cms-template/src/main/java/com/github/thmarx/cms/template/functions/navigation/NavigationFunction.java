package com.github.thmarx.cms.template.functions.navigation;

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
import com.github.thmarx.cms.api.model.NavNode;
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.content.ContentParser;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.DB;
import com.github.thmarx.cms.api.feature.features.ContentNodeMapperFeature;
import com.github.thmarx.cms.api.feature.features.ContentParserFeature;
import com.github.thmarx.cms.api.feature.features.MarkdownRendererFeature;
import com.github.thmarx.cms.api.mapper.ContentNodeMapper;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.api.request.RequestContext;
import com.github.thmarx.cms.api.utils.PathUtil;
import com.github.thmarx.cms.template.functions.AbstractCurrentNodeFunction;
import com.github.thmarx.cms.api.utils.NodeUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public class NavigationFunction extends AbstractCurrentNodeFunction {

	private static final int DEFAULT_DEPTH = 1;

	private String contentType = Constants.DEFAULT_CONTENT_TYPE;

	public NavigationFunction(DB db, Path currentNode, RequestContext context) {
		super(
				db,
				currentNode,
				context.get(ContentParserFeature.class).contentParser(),
				context.get(MarkdownRendererFeature.class).markdownRenderer(),
				context.get(ContentNodeMapperFeature.class).contentNodeMapper(),
				context);
	}

	/**
	 * Returns the path from root to the current node.
	 *
	 * @return List of orderd nodes from root to current
	 */
	public List<NavNode> path() {
		List<NavNode> nodes = new ArrayList<>();
		var contentBase = db.getFileSystem().resolve("content/");
		var node = currentNode;
		while (!node.equals(contentBase.getParent())) {

			var uri = PathUtil.toRelativeFile(node, contentBase);
			final Optional<ContentNode> contentNode = db.getContent().byUri(uri);
			if (contentNode.isPresent()) {
				var metaNode = contentNode.get();
				var name = NodeUtil.getName(metaNode);

				var path = contentBase.resolve(metaNode.uri());
				final NavNode navNode = new NavNode(name, getUrl(path), isCurrentNode(path));
				if (!nodes.contains(navNode)) {
					nodes.add(navNode);
				}
			}

			node = node.getParent();
		}

		return nodes.reversed();
	}

	public NavigationFunction contentType(final String contentType) {
		this.contentType = contentType;
		return this;
	}

	public NavigationFunction json() {
		this.contentType = Constants.ContentTypes.JSON;
		return this;
	}

	public NavigationFunction html() {
		this.contentType = Constants.ContentTypes.HTML;
		return this;
	}

	public List<NavNode> list(final String start) {
		return getNodes(start, DEFAULT_DEPTH);
	}

	public List<NavNode> list(final String start, final long depth) {
		return getNodes(start, (int) depth);
	}

	private List<NavNode> getNodes(final String start, final int depth) {
		if (start.startsWith("/")) { // root
			return getNodesFromBase(db.getFileSystem().resolve("content/"), start.substring(1), depth);
		} else if (start.equals(".")) { // current
			return getNodesFromBase(currentNode.getParent(), "", depth);
		} else if (start.startsWith("./")) { // subfolder of current
			return getNodesFromBase(currentNode.getParent(), start.substring(2), depth);
		}
		return Collections.emptyList();
	}

	private List<NavNode> getSubNodesFromBaseRemoveCurrent(final Path base, final String start, final int depth, final String toRemovePath) {
		List<NavNode> nodes = getNodesFromBase(base, start, depth);

		return nodes.stream().filter(node -> !node.path().equals(toRemovePath)).toList();
	}

	public List<NavNode> getNodesFromBase(final Path base, final String start, final int depth) {
		if (depth == 0) {
			return Collections.emptyList();
		}
		try {
			final List<ContentNode> navNodes = new ArrayList(
					db.getContent().listContent(base, start)
							.stream()
							.filter(NodeUtil::getMenuVisibility)
							.filter(NodeUtil.contentTypeFiler(contentType))
							.toList()
			);

			navNodes.sort((node1, node2) -> {
				var position1 = NodeUtil.getMenuPosition(node1);
				var position2 = NodeUtil.getMenuPosition(node2);

				int compare = Double.compare(position1, position2);

				if (compare == 0) {
					var name1 = NodeUtil.getName(node1);
					var name2 = NodeUtil.getName(node2);

					return name1.compareTo(name2);
				}

				return compare;
			});

			final List<NavNode> nodes = new ArrayList<>();
			final Path contentBase = db.getFileSystem().resolve("content/");
			navNodes.forEach((node) -> {
				var name = NodeUtil.getName(node);
				var path = contentBase.resolve(node.uri());
				var node_url = getUrl(path);
				final NavNode navNode = new NavNode(
						name,
						node_url,
						isCurrentNode(path),
						getSubNodesFromBaseRemoveCurrent(path.getParent(), "./", (depth - 1), node_url)
				);
				nodes.add(navNode);
			});
			return nodes;
		} catch (Exception ex) {
			log.error(null, ex);
		}
		return Collections.emptyList();
	}

	private boolean isCurrentNode(final Path node) {
		Path nodeIndex;
		if ("index.md".equals(node.getFileName().toString())) {
			nodeIndex = node;
		} else {
			nodeIndex = node.resolve("index.md");
		}
		return node.equals(currentNode) || currentNode.equals(nodeIndex);
	}
}
