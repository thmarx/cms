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

import com.github.thmarx.cms.api.model.ListNode;
import com.github.thmarx.cms.api.db.Page;
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.DB;
import com.github.thmarx.cms.api.feature.features.ContentNodeMapperFeature;
import com.github.thmarx.cms.api.feature.features.ContentParserFeature;
import com.github.thmarx.cms.api.feature.features.MarkdownRendererFeature;
import com.github.thmarx.cms.api.request.RequestContext;
import com.github.thmarx.cms.template.functions.AbstractCurrentNodeFunction;
import java.nio.file.Path;
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

	public NodeListFunction(DB db, Path currentNode, RequestContext context) {
		super(
				db,
				currentNode,
				context.get(ContentParserFeature.class).contentParser(),
				context.get(MarkdownRendererFeature.class).markdownRenderer(),
				context.get(ContentNodeMapperFeature.class).contentNodeMapper(),
				context);
	}

	public NodeListFunction(DB db, Path currentNode, RequestContext context, boolean excludeIndexMd) {
		this(db, currentNode, context);
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

		Path baseNode = null;
		String path = start;
		// first select base node
		if (start.startsWith("/")) {
			baseNode = db.getFileSystem().resolve("content/");
			path = start.substring(1);
		} else if (start.equals(".")) {
			baseNode = currentNode.getParent();
			path = "";
		} else if (start.startsWith("./")) {
			baseNode = currentNode.getParent();
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
			final Path contentBase = db.getFileSystem()
					.resolve("content/");
			List<ContentNode> relevantPaths = getPaths(baseNode, path);

			List<ContentNode> allContentNodes = new ArrayList<>();
			relevantPaths.forEach((metaNode) -> {

				List<ContentNode> children = db.getContent().listContent(contentBase, metaNode.uri());
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
						navNodes.add(contentNodeMapper.toListNode(node, context, excerptLength));
					});

			int totalPages = (int) Math.ceil((float) total / size);
			return new Page<>(total, navNodes.size(), totalPages, page, navNodes);

		} else {
			return getNodesFromBase(baseNode, path, page, size, comparator, nodeFilter);
		}
	}

	private List<ContentNode> getPaths(final Path base, final String path) {
		Set<ContentNode> relevantPaths = new HashSet<>();
		var parts = path.split(Constants.SPLIT_PATH_PATTERN);

		var part = parts[0];
		List<ContentNode> nodes;
		if ("*".equals(part)) {
			nodes = db.getContent().listDirectories(base, "");
		} else {
			nodes = db.getContent().listDirectories(base, part);
		}
		if (parts.length > 1) {
			nodes.forEach((node) -> {
				var newPath = Arrays.copyOfRange(parts, 1, parts.length);
				var subnodes = getPaths(base.resolve(part), String.join("/", newPath));
				relevantPaths.addAll(subnodes);
			});
		}
		if (parts.length == 1) {
			relevantPaths.addAll(nodes);
		}

		return new ArrayList<>(relevantPaths);
	}

	public Page<ListNode> getNodesFromBase(final Path base, final String start, final int page, final int pageSize, 
			final Comparator<ContentNode> comparator, final Predicate<ContentNode> nodeFilter) {
		try {
			List<ListNode> nodes = new ArrayList<>();
			final List<ContentNode> navNodes = db.getContent()
					.listContent(base, start)
					.stream().filter(nodeFilter)
					.toList();
			final Path contentBase = db.getFileSystem().resolve("content/");
			long total = navNodes.stream().filter(nodeNameFilter).count();
			int skipCount = (page - 1) * pageSize;

			navNodes.stream().filter(nodeNameFilter)
					.sorted(comparator)
					.skip(skipCount)
					.limit(pageSize)
					.forEach(node -> {
						nodes.add(contentNodeMapper.toListNode(node, context));
					});

			int totalPages = (int) Math.ceil((float) total / pageSize);
			return new Page<ListNode>(total, pageSize, totalPages, page, nodes);
		} catch (Exception ex) {
			log.error(null, ex);
		}
		return Page.EMPTY;
	}

}
