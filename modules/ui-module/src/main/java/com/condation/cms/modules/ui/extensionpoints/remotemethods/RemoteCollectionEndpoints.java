package com.condation.cms.modules.ui.extensionpoints.remotemethods;

/*-
 * #%L
 * UI Module
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

import com.condation.cms.api.Constants;
import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.db.CursorPage;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.db.collection.CollectionItemId;
import com.condation.cms.api.db.collection.CollectionItemMetadata;
import com.condation.cms.api.eventbus.events.InvalidateContentCacheEvent;
import com.condation.cms.api.feature.features.EventBusFeature;
import com.condation.cms.api.feature.features.WorkflowFeature;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.ui.annotations.RemoteMethod;
import com.condation.cms.api.ui.extensions.UIRemoteMethodExtensionPoint;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.utils.MapUtil;
import com.condation.cms.content.utils.SlugUtil;
import com.condation.cms.content.template.functions.LinkFunction;
import com.condation.cms.core.content.io.YamlHeaderUpdater;
import com.condation.cms.modules.ui.utils.FormHelper;
import com.condation.cms.modules.ui.utils.MetaConverter;
import com.condation.cms.modules.ui.utils.NumberUtils;
import com.condation.modules.api.annotation.Extension;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Manager endpoints for listing and editing collection items. */
@Slf4j
@Extension(UIRemoteMethodExtensionPoint.class)
public class RemoteCollectionEndpoints extends AbstractRemoteMethodeExtension {

	private static final long DEFAULT_PAGE_SIZE = 10;
	private static final long MAX_PAGE_SIZE = 100;

	public record ItemDto(
			String id,
			String collection,
			String path,
			String title,
			String detailUrl,
			Map<String, Object> meta) {
	}

	public record EditableItemDto(
			String id,
			String collection,
			String path,
			String content,
			Map<String, Object> meta) {
	}

	public record CursorItemsDto(List<ItemDto> items, String nextCursor) {
	}

	@RemoteMethod(name = "collections.items", permissions = {Permissions.CONTENT_EDIT})
	public Object items(Map<String, Object> parameters) throws RPCException {
		var repository = getCollectionRepository(parameters);
		var collectionName = requiredString(parameters, Parameters.COLLECTION);
		ensureCollectionExists(repository.names(), collectionName);

		long page = Math.max(1, NumberUtils.toLong(parameters.getOrDefault("page", 1L)));
		long size = Math.clamp(
				NumberUtils.toLong(parameters.getOrDefault("size", DEFAULT_PAGE_SIZE)),
				1,
				MAX_PAGE_SIZE);
		var title = optionalString(parameters, "query");

		var query = repository.metadataQuery(collectionName);
		if (!title.isBlank()) {
			query.searchByTitle(title);
		}
		query.orderby(Constants.MetaFields.TITLE).asc();
		Page<CollectionItemMetadata> result = query.page(page, size);
		return new Page<>(
				result.getTotalItems(),
				result.getPageSize(),
				result.getTotalPages(),
				result.getPage(),
				result.getItems().stream().map(this::itemDto).toList());
	}

	@RemoteMethod(name = "collections.items.cursor", permissions = {Permissions.CONTENT_EDIT})
	public Object cursorItems(Map<String, Object> parameters) throws RPCException {
		var repository = getCollectionRepository(parameters);
		var collectionName = requiredString(parameters, Parameters.COLLECTION);
		ensureCollectionExists(repository.names(), collectionName);
		long size = Math.clamp(
				NumberUtils.toLong(parameters.getOrDefault("size", DEFAULT_PAGE_SIZE)),
				1,
				MAX_PAGE_SIZE);
		var title = optionalString(parameters, "query");
		CursorPage<CollectionItemMetadata> result;
		try {
			result = repository.metadataCursorPage(
					collectionName,
					optionalString(parameters, "cursor"),
					size,
					query -> {
						if (!title.isBlank()) {
							query.searchByTitle(title);
						}
						query.orderby(Constants.MetaFields.TITLE).asc();
					});
		} catch (UnsupportedOperationException exception) {
			throw new RPCException(501, exception.getMessage());
		}
		return new CursorItemsDto(
				result.items().stream().map(this::itemDto).toList(),
				result.nextCursor());
	}


	@RemoteMethod(name = "collections.item.get", permissions = {Permissions.CONTENT_EDIT})
	public Object get(Map<String, Object> parameters) throws RPCException {
		var item = item(parameters);
		return new EditableItemDto(
				item.id(),
				item.collection(),
				item.path(),
				item.content(),
				item.meta());
	}

	@RemoteMethod(name = "collections.item.save", permissions = {Permissions.CONTENT_EDIT})
	public synchronized Object save(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableCollectionRepository(parameters);
		var item = item(parameters);
		ensureWritable(repository, item.collection());
		try {
			var meta = new HashMap<>(item.meta());
			var rawMeta = typedMeta(parameters.get("meta"));
			YamlHeaderUpdater.mergeFlatMapIntoNestedMap(meta, MetaConverter.convertMeta(rawMeta));
			normalizeAndValidateSlug(repository, item.collection(), item.id(), meta);
			var content = parameters.containsKey(Parameters.CONTENT)
					? FormHelper.getContent(parameters.get(Parameters.CONTENT))
					: item.content();
			repository.save(item.collection(), item.id(), meta, content);
			invalidateContentCache();
			return Map.of("saved", true);
		} catch (IOException | RuntimeException ex) {
			log.error("could not save collection item {}/{}", item.collection(), item.id(), ex);
			throw new RPCException(0, ex.getMessage());
		}
	}

	@RemoteMethod(name = "collections.item.create", permissions = {Permissions.CONTENT_EDIT})
	public synchronized Object create(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableCollectionRepository(parameters);
		var collectionName = requiredString(parameters, Parameters.COLLECTION);
		var id = requiredItemId(parameters);
		ensureCollectionExists(repository.names(), collectionName);
		ensureWritable(repository, collectionName);
		if (repository.get(collectionName, id).isPresent()) {
			throw new RPCException(409, "collection item already exists");
		}

		var meta = new HashMap<String, Object>();
		YamlHeaderUpdater.mergeFlatMapIntoNestedMap(
				meta,
				MetaConverter.convertMeta(typedMeta(parameters.get("meta"))));
		normalizeAndValidateSlug(repository, collectionName, id, meta);
		meta.putIfAbsent(Constants.MetaFields.TITLE, id);
		meta.put("createdAt", Date.from(Instant.now()));
		meta.put("createdBy", getUserName());
		meta.put(
				Constants.MetaFields.STATUS,
				getContext().get(WorkflowFeature.class).workflow().getStatusProvider().newNodeStatus());
		var content = FormHelper.getContent(parameters.get(Parameters.CONTENT));

		try {
			var createdItem = repository.create(collectionName, id, meta, content);
			invalidateContentCache();
			return itemDto(createdItem);
		} catch (IOException | RuntimeException exception) {
			log.error("could not create collection item {}/{}", collectionName, id, exception);
			throw new RPCException(0, exception.getMessage());
		}
	}

	@RemoteMethod(name = "collections.item.delete", permissions = {Permissions.CONTENT_EDIT})
	public Object delete(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableCollectionRepository(parameters);
		var collectionName = requiredString(parameters, Parameters.COLLECTION);
		var id = requiredItemId(parameters);
		ensureCollectionExists(repository.names(), collectionName);
		ensureWritable(repository, collectionName);
		if (repository.get(collectionName, id).isEmpty()) {
			throw new RPCException(404, "collection item not found");
		}

		try {
			repository.delete(collectionName, id);
			invalidateContentCache();
			return Map.of("deleted", true);
		} catch (IOException | RuntimeException exception) {
			log.error("could not delete collection item {}/{}", collectionName, id, exception);
			throw new RPCException(0, exception.getMessage());
		}
	}

	private CollectionItem item(Map<String, Object> parameters) throws RPCException {
		var repository = getCollectionRepository(parameters);
		var collectionName = requiredString(parameters, Parameters.COLLECTION);
		var id = requiredItemId(parameters);
		ensureCollectionExists(repository.names(), collectionName);
		try {
			return repository.get(collectionName, id)
					.orElseThrow(() -> new RPCException(404, "collection item not found"));
		} catch (IllegalArgumentException ex) {
			throw new RPCException(400, ex.getMessage());
		}
	}

	private ItemDto itemDto(CollectionItem item) {
		return itemDto(item.id(), item.collection(), item.path(), item.meta());
	}

	private ItemDto itemDto(CollectionItemMetadata item) {
		return itemDto(item.id(), item.collection(), item.path(), item.meta());
	}

	private ItemDto itemDto(
			String id,
			String collection,
			String path,
			Map<String, Object> meta) {
		var title = meta.get(Constants.MetaFields.TITLE);
		return new ItemDto(
				id,
				collection,
				path,
				title == null || title.toString().isBlank() ? id : title.toString(),
				detailUrl(new CollectionItem(id, collection, path, "", meta)),
				meta);
	}

	private String detailUrl(CollectionItem item) {
		try {
			return new LinkFunction(getRequestContext()).collectionUrl(item);
		} catch (IllegalArgumentException | IllegalStateException _) {
			return null;
		}
	}

	private void invalidateContentCache() {
		getContext().get(EventBusFeature.class).eventBus().publish(new InvalidateContentCacheEvent());
	}

	private static void ensureCollectionExists(java.util.Set<String> names, String name) throws RPCException {
		if (!names.contains(name)) {
			throw new RPCException(404, "collection not found: " + name);
		}
	}

	private static void ensureWritable(CollectionRepository repository, String name) throws RPCException {
		if (repository.access(name) != CollectionAccess.READ_WRITE) {
			throw new RPCException(403, "referenced collection is read-only: " + name);
		}
	}

	private static void normalizeAndValidateSlug(
			CollectionRepository repository,
			String collectionName,
			String itemId,
			Map<String, Object> meta) throws RPCException {
		var value = MapUtil.getValue(meta, "slug");
		if (value == null) {
			return;
		}
		if (!(value instanceof CharSequence)) {
			throw new RPCException(400, "collection item slug must be text");
		}

		var slug = SlugUtil.slugify(value.toString());
		if (slug.isBlank()) {
			throw new RPCException(400, "collection item slug must not be blank");
		}

		var duplicate = repository.metadataQuery(collectionName)
				.where("slug", slug)
				.page(1, 2)
				.getItems().stream()
				.filter(item -> !item.id().equals(itemId))
				.findAny()
				.isPresent();
		if (duplicate) {
			throw new RPCException(409, "collection item slug already exists: " + slug);
		}
		meta.put("slug", slug);
	}

	private static String requiredString(Map<String, Object> parameters, String name) throws RPCException {
		var value = optionalString(parameters, name);
		if (value.isBlank()) {
			throw new RPCException(400, name + " must not be blank");
		}
		return value;
	}

	private static String requiredItemId(Map<String, Object> parameters) throws RPCException {
		var id = requiredString(parameters, "id");
		try {
			return CollectionItemId.requireValid(id);
		} catch (IllegalArgumentException ex) {
			throw new RPCException(400, ex.getMessage());
		}
	}

	private static String optionalString(Map<String, Object> parameters, String name) {
		return parameters.get(name) instanceof String value ? value.trim() : "";
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Map<String, Object>> typedMeta(Object value) {
		return value instanceof Map<?, ?> map
				? (Map<String, Map<String, Object>>) map
				: Map.of();
	}
}
