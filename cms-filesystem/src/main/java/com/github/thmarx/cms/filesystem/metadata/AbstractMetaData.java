package com.github.thmarx.cms.filesystem.metadata;

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

import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.filesystem.MetaData;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 *
 * @author t.marx
 */
public abstract class AbstractMetaData implements MetaData {
	
	protected ConcurrentMap<String, ContentNode> nodes;

	protected ConcurrentMap<String, ContentNode> tree;
	
	@Override
	public void clear() {
		if (nodes != null) {
			nodes.clear();
			tree.clear();
		}
	}
	
	@Override
	public ConcurrentMap<String, ContentNode> getNodes() {
		if (nodes == null) {
			return new ConcurrentHashMap<>();
		}
		return new ConcurrentHashMap<>(nodes);
	}

	@Override
	public ConcurrentMap<String, ContentNode> getTree() {
		if (tree == null) {
			return new ConcurrentHashMap<>();
		}
		return new ConcurrentHashMap<>(tree);
	}
	
	public static boolean isVisible (ContentNode node) {
		return node != null 
				// check if some parent is hidden
				&& !node.uri().startsWith(".") && !node.uri().contains("/.")
				&& node.isPublished() 
				&& !node.isHidden() 
				&& !node.isSection();
	}
	
	@Override
	public Optional<ContentNode> byUri(String uri) {
		if (!nodes.containsKey(uri)) {
			return Optional.empty();
		}
		return Optional.of(nodes.get(uri));
	}
	
	@Override
	public Optional<ContentNode> findFolder(String uri) {
		return getFolder(uri);
	}

	protected Optional<ContentNode> getFolder(String uri) {
		var parts = uri.split(Constants.SPLIT_PATH_PATTERN);

		final AtomicReference<ContentNode> folder = new AtomicReference<>(null);
		Stream.of(parts).forEach(part -> {
			if (part.endsWith(".md")) {
				return;
			}
			if (folder.get() == null) {
				folder.set(getTree().get(part));
			} else {
				folder.set(folder.get().children().get(part));
			}
		});
		return Optional.ofNullable(folder.get());
	}
	
	@Override
	public void createDirectory(String uri) {
		if (Strings.isNullOrEmpty(uri)) {
			return;
		}
		var parts = uri.split(Constants.SPLIT_PATH_PATTERN);
		ContentNode n = new ContentNode(uri, parts[parts.length - 1], Map.of(), true);

		Optional<ContentNode> parentFolder;
		if (parts.length == 1) {
			//parentFolder = getFolder(uri);
			parentFolder = Optional.empty();
		} else {
			var parentPath = Arrays.copyOfRange(parts, 0, parts.length - 1);
			var parentUri = String.join("/", parentPath);
			parentFolder = getFolder(parentUri);
		}

		if (parentFolder.isPresent()) {
			parentFolder.get().children().put(n.name(), n);
		} else {
			tree.put(n.name(), n);
		}
	}

	@Override
	public List<ContentNode> listChildren(String uri) {
		if ("".equals(uri)) {
			return getTree().values().stream()
					.filter(node -> !node.isHidden())
					.map(this::mapToIndex)
					.filter(node -> node != null)
					.filter(AbstractMetaData::isVisible)
					.toList();

		} else {
			Optional<ContentNode> findFolder = findFolder(uri);
			if (findFolder.isPresent()) {
				return findFolder.get().children().values()
						.stream()
						.filter(node -> !node.isHidden())
						.map(this::mapToIndex)
						.filter(node -> node != null)
						.filter(AbstractMetaData::isVisible)
						.toList();
			}
		}
		return Collections.emptyList();
	}

	protected ContentNode mapToIndex(ContentNode node) {
		if (node.isDirectory()) {
			var tempNode = node.children().entrySet().stream().filter((entry)
					-> entry.getKey().equals("index.md")
			).findFirst();
			if (tempNode.isPresent()) {
				return tempNode.get().getValue();
			}
			return null;
		} else {
			return node;
		}
	}
}
