package com.condation.cms.filesystem;

/*-
 * #%L
 * CMS FileSystem
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

import com.condation.cms.api.configuration.configs.CollectionConfiguration;
import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.db.CursorPage;
import com.condation.cms.api.db.collection.Collection;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.db.collection.CollectionItemMetadata;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.repository.MutableCollectionRepository;
import com.condation.cms.core.serivce.ServiceRegistry;
import com.condation.cms.core.serivce.impl.SiteCollectionRepositoryService;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Adds lazy, read-only cross-site references to a site's local collection repository. */
final class ReferencedCollectionRepository implements MutableCollectionRepository {

	private final String siteId;
	private final MutableCollectionRepository localRepository;
	private final CollectionConfiguration configuration;

	ReferencedCollectionRepository(
			String siteId,
			MutableCollectionRepository localRepository,
			CollectionConfiguration configuration) {
		this.siteId = siteId;
		this.localRepository = localRepository;
		this.configuration = configuration;
	}

	@Override
	public Collection collection(String name) {
		var sourceSite = sourceSite(name);
		return sourceSite == null
				? localRepository.collection(name)
				: sourceRepository(sourceSite, name).collection(name);
	}

	@Override
	public CursorPage<CollectionItemMetadata> metadataCursorPage(
			String collection,
			String cursor,
			long size,
			Consumer<ContentQuery<CollectionItemMetadata>> queryConfigurer) {
		var sourceSite = sourceSite(collection);
		return sourceSite == null
				? localRepository.metadataCursorPage(collection, cursor, size, queryConfigurer)
				: sourceRepository(sourceSite, collection).metadataCursorPage(
						collection, cursor, size, queryConfigurer);
	}

	@Override
	public Set<String> names() {
		var names = new HashSet<>(localRepository.names());
		configuration.collections().values().stream()
				.filter(definition -> definition.sourceSite()
						.filter(sourceSite -> !siteId.equals(sourceSite))
						.isPresent())
				.map(definition -> definition.name())
				.forEach(names::add);
		return Set.copyOf(names);
	}

	@Override
	public CollectionAccess access(String collection) {
		validateCollection(collection);
		return sourceSite(collection) == null
				? localRepository.access(collection)
				: CollectionAccess.READ_ONLY;
	}

	@Override
	public CollectionItem create(
			String collection,
			String id,
			Map<String, Object> metadata,
			String content) throws IOException {
		ensureWritable(collection);
		return localRepository.create(collection, id, metadata, content);
	}

	@Override
	public void save(
			String collection,
			String id,
			Map<String, Object> metadata,
			String content) throws IOException {
		ensureWritable(collection);
		localRepository.save(collection, id, metadata, content);
	}

	@Override
	public void delete(String collection, String id) throws IOException {
		ensureWritable(collection);
		localRepository.delete(collection, id);
	}

	private void ensureWritable(String collection) {
		if (access(collection) != CollectionAccess.READ_WRITE) {
			throw new UnsupportedOperationException(
					"referenced collection is read-only: " + collection);
		}
	}

	private void validateCollection(String collection) {
		if (!names().contains(collection)) {
			throw new IllegalArgumentException("collection not found: " + collection);
		}
	}

	private String sourceSite(String collection) {
		return configuration.collection(collection)
				.flatMap(definition -> definition.sourceSite())
				.filter(source -> !siteId.equals(source))
				.orElse(null);
	}

	private CollectionRepository sourceRepository(String sourceSite, String collection) {
		var source = ServiceRegistry.getInstance().get(sourceSite, SiteCollectionRepositoryService.class)
				.orElseThrow(() -> new IllegalStateException(
						"collection source site is not available: " + sourceSite));
		var sourceCollections = source.repository();
		if (sourceCollections.access(collection) != CollectionAccess.READ_WRITE) {
			throw new IllegalStateException(
					"referenced collections must point to a local collection: "
					+ sourceSite + "/" + collection);
		}
		return sourceCollections;
	}
}
