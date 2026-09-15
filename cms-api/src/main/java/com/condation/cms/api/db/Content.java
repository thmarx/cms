package com.condation.cms.api.db;

/*-
 * #%L
 * CMS Api
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

import com.condation.cms.api.db.cms.ReadOnlyFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.jspecify.annotations.NonNull;

/**
 *
 * @author thmar
 */
public interface Content {
	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#isVisible(ContentNode)}
	 * after resolving the node via {@link com.condation.cms.api.repository.ContentRepository#get(String)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	boolean isVisible (String uri);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#isVisible(ContentNode)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	boolean isVisible (ContentNode node);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#sections(ContentNode)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	List<ContentNode>  listSectionEntries(final ReadOnlyFile contentFile);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#children(String)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	List<ContentNode> listContent(final ReadOnlyFile base, final String start);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#directories(String)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	List<ContentNode> listDirectories(final ReadOnlyFile base, final String start);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#get(String)}
	 */
	@Deprecated(since = "8.4.0")
	Optional<ContentNode> byUri (final String uri);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#get(String)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	Optional<ContentNode> byPath (final String path);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#findByUrl(String)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	Optional<ContentNode> byUrl (final String url);

	Optional<Map<String,Object>> getMeta(final String path);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#query(BiFunction)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	public <T> ContentQuery<T> query(final BiFunction<ContentNode, Integer, T> nodeMapper);

	/**
	 * @deprecated use {@link com.condation.cms.api.repository.ContentRepository#query(String, BiFunction)}
	 */
	@Deprecated(since = "8.4.0", forRemoval = false)
	public <T> ContentQuery<T> query(final String startURI, final BiFunction<ContentNode, Integer, T> nodeMapper);
	
	default List<ContentNode> searchByTitle (@NonNull String input) {
		return searchByTitle(input, VariantSearchMode.ALL);
	}

	List<ContentNode> searchByTitle(
			@NonNull String input,
			@NonNull VariantSearchMode variantSearchMode
	);
}
