package com.condation.cms.content.template.functions.list;

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
import com.condation.cms.api.db.Page;
import com.condation.cms.api.feature.features.ContentNodeMapperFeature;
import com.condation.cms.api.model.ListNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.content.template.functions.AbstractCurrentNodeFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
class NodeListFunction extends AbstractCurrentNodeFunction {

	private boolean excludeIndexMd = false;

	private Predicate<ContentNode> nodeNameFilter = (node) -> {
		var filename = node.name();
		if (excludeIndexMd && "index.md".equals(filename)) {
			return false;
		}
		return true;
	};

	public NodeListFunction(ContentRepository contentRepository,
			ContentNode currentNode, RequestContext context) {
		super(
				currentNode,
				contentRepository,
				context);
	}

	public NodeListFunction(ContentRepository contentRepository,
			ContentNode currentNode, RequestContext context, boolean excludeIndexMd) {
		this(contentRepository, currentNode, context);
		this.excludeIndexMd = excludeIndexMd;
	}

	public Page<ListNode> list(String start, int page, int size, final Comparator<ContentNode> comparator, final Predicate<ContentNode> nodeFilter) {
		return list(start, page, size, Constants.DEFAULT_EXCERPT_LENGTH, comparator, nodeFilter);
	}

	public Page<ListNode> list(String start, int page, int size, int excerptLength, final Comparator<ContentNode> comparator, final Predicate<ContentNode> nodeFilter) {
		return getNodes(start, page, size, excerptLength, comparator, nodeFilter);
	}
	
	public Page<ListNode> list(String start, int page, int size, final Comparator<ContentNode> comparator) {
		return list(start, page, size, Constants.DEFAULT_EXCERPT_LENGTH, comparator);
	}

	public Page<ListNode> list(String start, int page, int size, int excerptLength, final Comparator<ContentNode> comparator) {
		return getNodes(start, page, size, excerptLength, comparator, (node) -> true);
	}
	
	Page<ListNode> getNodes(final String start, int page, int size, int excerptLength, final Comparator<ContentNode> comparator,
			final Predicate<ContentNode> nodeFilter) {

		String baseNode = null;
		String path = start;
		// first select base node
		if (start.startsWith("/")) {
			baseNode = "";
			path = start.substring(1);
		} else if (start.equals(".")) {
			baseNode = currentDirectory();
			path = "";
		} else if (start.startsWith("./")) {
			baseNode = currentDirectory();
			path = start.substring(2);
		}

		if (baseNode == null) {
			return Page.EMPTY;
		}

		/*
		path:
		blog/2023-12/
		blog\/*\/*
		 */
		if (path.contains("*")) {
			List<ContentNode> relevantPaths = getPaths(baseNode, path);

			List<ContentNode> allContentNodes = new ArrayList<>();
			relevantPaths.forEach((metaNode) -> {

				List<ContentNode> children = contentRepository.children(metaNode.path());
				allContentNodes.addAll(children);
			});

			
			long total = allContentNodes.stream()
					.filter(nodeNameFilter)
					.filter(nodeFilter)
					.count();
			int skipCount = (page - 1) * size;

			List<ListNode> navNodes = new ArrayList<>();
			allContentNodes.stream()
					.filter(nodeNameFilter)
					.filter(nodeFilter)
					.sorted(comparator)
					.skip(skipCount)
					.limit(size)
					.forEach(node -> {
						navNodes.add(context.get(ContentNodeMapperFeature.class).contentNodeMapper()
								.toListNode(node, context, excerptLength));
					});

			int totalPages = (int) Math.ceil((float) total / size);
			return new Page<>(total, navNodes.size(), totalPages, page, navNodes);

		} else {
			return getNodesFromBase(baseNode, path, page, size, comparator, nodeFilter);
		}
	}

	private List<ContentNode> getPaths(final String base, final String path) {
		Set<ContentNode> relevantPaths = new HashSet<>();
		var parts = path.split(Constants.SPLIT_PATH_PATTERN);

		var part = parts[0];
		List<ContentNode> nodes;
		if ("*".equals(part)) {
			nodes = contentRepository.directories(repositoryPath(base, ""));
		} else {
			nodes = contentRepository.directories(repositoryPath(base, part));
		}
		if (parts.length > 1) {
			var remainingPath = String.join("/", Arrays.copyOfRange(parts, 1, parts.length));
			if ("*".equals(part)) {
				nodes.forEach(node -> relevantPaths.addAll(getPaths(node.path(), remainingPath)));
			} else if (!nodes.isEmpty()) {
				relevantPaths.addAll(getPaths(repositoryPath(base, part), remainingPath));
			}
		}
		if (parts.length == 1) {
			relevantPaths.addAll(nodes);
		}

		return new ArrayList<>(relevantPaths);
	}

	public Page<ListNode> getNodesFromBase(final String base, final String start, final int page, final int pageSize,
			final Comparator<ContentNode> comparator, final Predicate<ContentNode> nodeFilter) {
		try {
			List<ListNode> nodes = new ArrayList<>();
			final List<ContentNode> navNodes = contentRepository
					.children(repositoryPath(base, start))
					.stream().filter(nodeFilter)
					.toList();
			long total = navNodes.stream().filter(nodeNameFilter).count();
			int skipCount = (page - 1) * pageSize;

			navNodes.stream().filter(nodeNameFilter)
					.sorted(comparator)
					.skip(skipCount)
					.limit(pageSize)
					.forEach(node -> {
						nodes.add(context.get(ContentNodeMapperFeature.class).contentNodeMapper()
								.toListNode(node, context));
					});

			int totalPages = (int) Math.ceil((float) total / pageSize);
			return new Page<ListNode>(total, pageSize, totalPages, page, nodes);
		} catch (Exception ex) {
			log.error(null, ex);
		}
		return Page.EMPTY;
	}

}
