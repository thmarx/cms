package com.condation.cms.content.template.functions.navigation;

/*-
 * #%L
 * CMS Content
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
import com.condation.cms.api.Constants;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.ContentNodeMapperFeature;
import com.condation.cms.api.feature.features.HookSystemFeature;
import com.condation.cms.api.hooks.HookSystem;
import com.condation.cms.api.hooks.Hooks;
import com.condation.cms.api.model.NavNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.utils.NodeUtil;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.cms.content.template.functions.AbstractCurrentNodeFunction;
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
	private static final String INDEX_MD = "index.md";
	private static final String SLASH_INDEX_MD = "/" + INDEX_MD;

	private String contentType = Constants.DEFAULT_CONTENT_TYPE;

	private String name = null;

	private final HookSystem hookSystem;

	public NavigationFunction(ContentRepository contentRepository,
			ContentNode currentNode, RequestContext context) {
		super(
				currentNode,
				contentRepository,
				context);
		hookSystem = context.get(HookSystemFeature.class).hookSystem();
	}

	public NavigationFunction named(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the path from root to the current node.
	 *
	 * @return List of ordered nodes from root to current
	 */
	public List<NavNode> path() {
		List<NavNode> navNodes = new ArrayList<>();
		var node = currentNode;
		while (node != null) {
			var nodeName = NodeUtil.getName(node);
			final NavNode navNode = new NavNode(nodeName,
					HTTPUtil.modifyUrl(node.url(), context), isCurrentNode(node));
			if (!navNodes.contains(navNode)) {
				navNodes.add(navNode);
			}
			node = parentNode(node).orElse(null);
		}

		navNodes = navNodes.reversed();

		if (name != null) {
			navNodes = hookSystem.doFilter(Hooks.NAVIGATION_PATH.hook(name), navNodes);
		}

		return navNodes;
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
		List<NavNode> navNodes = Collections.emptyList();
		if (start.startsWith("/")) { // root
			navNodes = getNodesFromBase("", start.substring(1), depth);
		} else if (start.equals(".")) { // current
			navNodes = getNodesFromBase(currentDirectory(), "", depth);
		} else if (start.startsWith("./")) { // subfolder of current
			navNodes = getNodesFromBase(currentDirectory(), start.substring(2), depth);
			
		}
		if (name != null) {
			navNodes = hookSystem.doFilter(Hooks.NAVIGATION_LIST.hook(name), navNodes);
		}
		return navNodes;
	}

	private List<NavNode> getSubNodesFromBaseRemoveCurrent(final String base, final String start, final int depth, final String toRemovePath) {
		List<NavNode> nodes = getNodesFromBase(base, start, depth);

		return nodes.stream().filter(node -> !node.path().equals(toRemovePath)).toList();
	}

	private List<NavNode> getNodesFromBase(final String base, final String start, final int depth) {
		if (depth == 0) {
			return Collections.emptyList();
		}
		try {
			final List<ContentNode> navNodes = new ArrayList(
					contentRepository.children(repositoryPath(base, start))
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
			navNodes.forEach((node) -> {
				var name = NodeUtil.getName(node);
				var node_url = HTTPUtil.modifyUrl(node.url(), context);
				final NavNode navNode = new NavNode(
						name,
						node_url,
						isCurrentNode(node),
						getSubNodesFromBaseRemoveCurrent(childBase(node), "./", (depth - 1), node_url)
				);
				nodes.add(navNode);
			});
			return nodes;
		} catch (Exception ex) {
			log.error(null, ex);
		}
		return Collections.emptyList();
	}

	private boolean isCurrentNode(final ContentNode node) {
		return currentNode != null && (node.equals(currentNode)
				|| normalize(node.url()).equals(normalize(currentNode.url())));
	}

	private String childBase(ContentNode node) {
		var path = normalize(node.path());
		return path.endsWith(INDEX_MD)
				? path.substring(0, path.length() - INDEX_MD.length())
				: path;
	}

	private Optional<ContentNode> parentNode(ContentNode node) {
		var path = normalize(node.path());
		var directory = node.isDirectory() ? path : parentPath(path);
		if (path.endsWith(SLASH_INDEX_MD)) {
			directory = parentPath(path.substring(0, path.length() - SLASH_INDEX_MD.length()));
		} else if (INDEX_MD.equals(path)) {
			return Optional.empty();
		}
		if (directory.isEmpty()) {
			return contentRepository.get(INDEX_MD).filter(parent -> !parent.equals(node));
		}
		var parentDirectory = directory;
		return contentRepository.get(parentDirectory + SLASH_INDEX_MD)
				.or(() -> contentRepository.get(parentDirectory))
				.filter(parent -> !parent.equals(node));
	}

	private static String parentPath(String path) {
		var separator = path.lastIndexOf('/');
		return separator < 0 ? "" : path.substring(0, separator);
	}
}
