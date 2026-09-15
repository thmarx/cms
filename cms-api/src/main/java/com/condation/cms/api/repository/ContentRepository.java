package com.condation.cms.api.repository;

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

import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.variants.Variant;
import com.condation.cms.api.variants.VariantContext;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Backend-neutral access to indexed CMS content.
 *
 * A repository exposes content semantics such as indexed queries and the
 * relation between a page and its sections. It deliberately does not expose
 * how those entries are physically stored.
 *
 * @author thmar
 */
public interface ContentRepository {

	Optional<ContentNode> get(String path);

	Optional<ContentNode> findByUrl(String url);

	Optional<ContentDocument> load(ContentNode node) throws IOException;

	List<ContentNode> children(String path);

	List<ContentNode> directories(String path);

	boolean isVisible(ContentNode node);

	List<Section> sections(ContentNode owner) throws IOException;

	List<Section> sections(ContentNode owner, String sectionName) throws IOException;

	List<Variant> variants(ContentNode node);

	Optional<Variant> variant(ContentNode node, String variantId);

	VariantContext variantContext(ContentNode node);

	default Optional<String> variantSelectorId(ContentNode node) {
		return Optional.empty();
	}

	ContentQuery<ContentNode> query();

	ContentQuery<ContentNode> query(String startPath);

	<T> ContentQuery<T> query(BiFunction<ContentNode, Integer, T> nodeMapper);

	<T> ContentQuery<T> query(String startPath, BiFunction<ContentNode, Integer, T> nodeMapper);
}
