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
import java.io.IOException;
import java.util.Map;

/** Backend-neutral mutations for managed content. */
public interface MutableContentRepository extends ContentRepository {

	void save(String path, Map<String, Object> metadata, String content) throws IOException;

	void createDirectory(String path) throws IOException;

	void delete(String path) throws IOException;

	void deleteRecursively(String path) throws IOException;

	void move(String sourcePath, String targetPath) throws IOException;

	boolean resourceExists(String path);

	void setVariantSelectorId(ContentNode node, String selectorId) throws IOException;
}
