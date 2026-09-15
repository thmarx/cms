package com.condation.cms.api.db.collection;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.condation.cms.api.db.ContentQuery;
import java.util.Optional;

/** A named collection, independent of its persistence backend. */
public interface Collection {

	String name();

	Optional<CollectionItem> item(String id);

	ContentQuery<CollectionItem> query();

	/** Queries collection metadata without opening the Markdown body files. */
	ContentQuery<CollectionItemMetadata> metadataQuery();
}
