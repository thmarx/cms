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

import com.condation.cms.api.db.collection.CollectionItem;
import java.io.IOException;
import java.util.Map;

/** Backend-neutral mutations for collections owned by a site. */
public interface MutableCollectionRepository extends CollectionRepository {

	CollectionItem create(String collection, String id, Map<String, Object> metadata, String content)
			throws IOException;

	void save(String collection, String id, Map<String, Object> metadata, String content)
			throws IOException;

	void delete(String collection, String id) throws IOException;
}
