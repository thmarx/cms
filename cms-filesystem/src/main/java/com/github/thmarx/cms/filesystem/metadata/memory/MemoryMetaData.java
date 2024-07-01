package com.github.thmarx.cms.filesystem.metadata.memory;

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

import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.ContentQuery;
import com.github.thmarx.cms.filesystem.metadata.AbstractMetaData;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 *
 * @author t.marx
 */
public class MemoryMetaData extends AbstractMetaData {
	
	@Override
	public void addFile(final String uri, final Map<String, Object> data, final LocalDate lastModified) {

		var parts = uri.split(Constants.SPLIT_PATH_PATTERN);
		final ContentNode node = new ContentNode(uri, parts[parts.length - 1], data, lastModified);

		nodes.put(uri, node);

		var folder = getFolder(uri);
		if (folder.isPresent()) {
			folder.get().children().put(node.name(), node);
		} else {
			tree.put(node.name(), node);
		}
	}

	void remove(String uri) {
		nodes.remove(uri);

		var folder = getFolder(uri);
		var parts = uri.split(Constants.SPLIT_PATH_PATTERN);
		var name = parts[parts.length - 1];
		if (folder.isPresent()) {
			folder.get().children().remove(name);
		} else {
			tree.remove(name);
		}
	}

	@Override
	public void open() throws IOException {
		if (nodes == null) {
			nodes = new ConcurrentHashMap<>();
			tree = new ConcurrentHashMap<>();
		}
	}

	@Override
	public void close() throws IOException {
		nodes = null;
		tree = null;
	}
	
	@Override
	public <T> ContentQuery<T> query(final BiFunction<ContentNode, Integer, T> nodeMapper) {
		return new MemoryQuery<T>(new ArrayList<>(nodes.values()), nodeMapper);
	}

	@Override
	public <T> ContentQuery<T> query(final String startURI, final BiFunction<ContentNode, Integer, T> nodeMapper) {

		final String uri;
		if (startURI.startsWith("/")) {
			uri = startURI.substring(1);
		} else {
			uri = startURI;
		}

		var filtered = getNodes().values().stream().filter(node -> node.uri().startsWith(uri)).toList();

		return new MemoryQuery<T>(filtered, nodeMapper);
	}

	
}
