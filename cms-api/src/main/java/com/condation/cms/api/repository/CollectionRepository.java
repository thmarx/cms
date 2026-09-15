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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.db.CursorPage;
import com.condation.cms.api.db.collection.Collection;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.db.collection.CollectionItemMetadata;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Backend-neutral access to named collection data. */
public interface CollectionRepository {

	Set<String> names();

	default boolean exists(String collection) {
		return names().contains(collection);
	}

	CollectionAccess access(String collection);

	Collection collection(String name);

	default Optional<CollectionItem> get(String collection, String id) {
		return collection(collection).item(id);
	}

	default ContentQuery<CollectionItem> query(String collection) {
		return collection(collection).query();
	}

	default ContentQuery<CollectionItemMetadata> metadataQuery(String collection) {
		return collection(collection).metadataQuery();
	}

	CursorPage<CollectionItemMetadata> metadataCursorPage(
			String collection,
			String cursor,
			long size,
			Consumer<ContentQuery<CollectionItemMetadata>> queryConfigurer);
}
