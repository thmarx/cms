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

import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.ContentQuery;
import com.github.thmarx.cms.filesystem.index.IndexProviding;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 *
 * @author t.marx
 */
public interface MetaData extends IndexProviding {
	
	public enum Type {
		MEMORY, PERSISTENT
	}
	
	void open () throws IOException;
	void close () throws IOException;

	void addFile(final String uri, final Map<String, Object> data, final LocalDate lastModified);

	Optional<ContentNode> byUri(final String uri);

	void createDirectory(final String uri);

	Optional<ContentNode> findFolder(String uri);

	List<ContentNode> listChildren(String uri);
	
	void clear ();
	
	Map<String, ContentNode> nodes();

	Map<String, ContentNode> tree();
	
	<T> ContentQuery<T> query(final BiFunction<ContentNode, Integer, T> nodeMapper);
	<T> ContentQuery<T> query(final String startURI, final BiFunction<ContentNode, Integer, T> nodeMapper);
}
