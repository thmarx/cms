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

import com.condation.cms.api.Constants;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.db.CursorPage;
import com.condation.cms.api.db.NodeVisibility;
import com.condation.cms.api.db.collection.Collection;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.db.collection.CollectionItemId;
import com.condation.cms.api.db.collection.CollectionItemMetadata;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.repository.MutableCollectionRepository;
import com.condation.cms.api.utils.PathUtil;
import com.condation.cms.core.content.io.ContentFileParser;
import com.condation.cms.core.content.io.YamlHeaderUpdater;
import com.condation.cms.filesystem.metadata.persistent.CollectionMetaData;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/** Site-scoped collection repository backed by Markdown files and a persistent index. */
@Slf4j
public final class FileSystemCollectionRepository
		implements MutableCollectionRepository, AutoCloseable {

	private static final Pattern COLLECTION_NAME = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_-]*");
	private static final Duration CHANGE_QUIET_PERIOD = Duration.ofMillis(200);

	private final String siteId;
	private final Path hostBase;
	private final Path collectionsBase;
	private final Function<Path, Map<String, Object>> metaParser;
	private final Set<String> collectionNames = ConcurrentHashMap.newKeySet();

	private CollectionMetaData metaData;
	private MultiRootRecursiveWatcher watcher;
	private ContentChangeCoordinator changeCoordinator;

	public FileSystemCollectionRepository(
			String siteId,
			Path hostBase,
			Function<Path, Map<String, Object>> metaParser) {
		this.siteId = siteId;
		this.hostBase = hostBase;
		this.collectionsBase = hostBase.resolve(Constants.Folders.COLLECTIONS);
		this.metaParser = metaParser;
	}

	public void init() throws IOException {
		Files.createDirectories(collectionsBase);
		metaData = new CollectionMetaData(hostBase);
		metaData.open();
		changeCoordinator = new ContentChangeCoordinator(CHANGE_QUIET_PERIOD, this::processChanges);
		rebuild(false);

		watcher = new MultiRootRecursiveWatcher(siteId, List.of(collectionsBase));
		var publisher = java.util.Objects.requireNonNull(
				watcher.getPublisher(collectionsBase),
				"collections publisher must be available");
		publisher.subscribe(new MultiRootRecursiveWatcher.AbstractFileEventSubscriber() {
			@Override
			public void onNext(FileEvent item) {
				handleEvent(item);
				this.subscription.request(1);
			}
		});
		watcher.start();
	}

	@Override
	public Set<String> names() {
		return Set.copyOf(collectionNames);
	}

	@Override
	public CollectionAccess access(String collection) {
		validateExistingCollection(collection);
		return CollectionAccess.READ_WRITE;
	}

	@Override
	public Collection collection(String name) {
		validateCollectionName(name);
		return new FileCollection(name);
	}

	@Override
	public CursorPage<CollectionItemMetadata> metadataCursorPage(
			String collection,
			String cursor,
			long size,
			Consumer<ContentQuery<CollectionItemMetadata>> queryConfigurer) {
		validateCollectionName(collection);
		return metaData.cursorPage(collection, this::mapMetadata, cursor, size, queryConfigurer);
	}

	@Override
	public CollectionItem create(
			String collection,
			String id,
			Map<String, Object> metadata,
			String content) throws IOException {
		var target = writableItem(collection, id);
		if (Files.exists(target)) {
			throw new FileAlreadyExistsException(collection + "/" + id);
		}
		write(collection, id, target, metadata, content);
		return new CollectionItem(id, collection, itemPath(collection, id), content, metadata);
	}

	@Override
	public void save(
			String collection,
			String id,
			Map<String, Object> metadata,
			String content) throws IOException {
		write(collection, id, writableItem(collection, id), metadata, content);
	}

	@Override
	public void delete(String collection, String id) throws IOException {
		var target = writableItem(collection, id);
		if (!Files.isRegularFile(target)) {
			throw new java.nio.file.NoSuchFileException(collection + "/" + id);
		}
		Files.delete(target);
		refresh(collection, id);
	}

	private void write(
			String collection,
			String id,
			Path target,
			Map<String, Object> metadata,
			String content) throws IOException {
		Files.createDirectories(target.getParent());
		YamlHeaderUpdater.saveMarkdownFileWithHeader(target, metadata, content);
		refresh(collection, id);
	}

	private Path writableItem(String collection, String id) {
		validateExistingCollection(collection);
		CollectionItemId.requireValid(id);
		var root = collectionsBase.toAbsolutePath().normalize();
		var target = root.resolve(collection).resolve(id + ".md").normalize();
		if (!target.startsWith(root) || target.equals(root)) {
			throw new IllegalArgumentException("invalid collection item path");
		}
		return target;
	}

	void refresh(String collection, String id) {
		validateCollectionName(collection);
		CollectionItemId.requireValid(id);
		var file = collectionsBase.resolve(collection).resolve(id + ".md");
		try {
			if (Files.isRegularFile(file)) {
				index(file);
			} else {
				metaData.removeFile(itemPath(collection, id));
			}
		} catch (IOException ex) {
			throw new IllegalStateException("could not refresh collection item", ex);
		}
	}

	void handleEvent(FileEvent event) {
		if (event.type() == FileEvent.Type.OVERFLOW) {
			changeCoordinator.requestFullResync();
		} else {
			changeCoordinator.submit(event.file().toPath());
		}
	}

	void flushChanges() {
		changeCoordinator.flushNow();
	}

	public void reindex() {
		changeCoordinator.requestFullResync();
		changeCoordinator.flushNow();
	}

	private void processChanges(boolean fullResync, Set<Path> paths) {
		try {
			if (fullResync) {
				rebuild(true);
				return;
			}
			for (var path : paths) {
				processPath(path);
			}
		} catch (IOException ex) {
			log.error("error processing collection changes", ex);
		}
	}

	private void processPath(Path path) throws IOException {
		var relative = PathUtil.toRelativeEntry(path, collectionsBase);
		if (relative.isBlank() || relative.startsWith("../")) {
			return;
		}
		var parts = relative.split("/");
		if (parts.length == 1) {
			if (Files.isDirectory(path)) {
				scanCollection(path);
			} else if (!Files.exists(path)) {
				metaData.removeDirectory(parts[0]);
				collectionNames.remove(parts[0]);
			}
			return;
		}
		if (parts.length != 2 || !isValidItemFile(path) || !isValidCollectionName(parts[0])) {
			return;
		}
		if (Files.isRegularFile(path)) {
			index(path);
		} else if (!Files.exists(path)) {
			metaData.removeFile(relative);
		}
	}

	private void rebuild(boolean force) throws IOException {
		metaData.startBatch();
		try {
			if (force) {
				metaData.clear();
			}
			var staleCollections = metaData.collectionNames();
			collectionNames.clear();
			try (var collections = Files.list(collectionsBase)) {
				for (var iterator = collections.filter(Files::isDirectory).iterator(); iterator.hasNext();) {
					var collection = iterator.next();
					staleCollections.remove(collection.getFileName().toString());
					scanCollection(collection);
				}
			}
			staleCollections.forEach(metaData::removeDirectory);
		} finally {
			metaData.stopBatch();
		}
	}

	private void scanCollection(Path collection) throws IOException {
		var name = collection.getFileName().toString();
		if (!isValidCollectionName(name)) {
			log.warn("ignoring invalid collection name {}", name);
			return;
		}
		collectionNames.add(name);
		var stalePaths = metaData.paths(name);
		try (var files = Files.list(collection)) {
			for (var iterator = files.filter(Files::isRegularFile)
					.filter(FileSystemCollectionRepository::isValidItemFile).iterator(); iterator.hasNext();) {
				var file = iterator.next();
				var path = PathUtil.toRelativeEntry(file, collectionsBase);
				stalePaths.remove(path);
				var stamp = fileStamp(file);
				if (metaData.byPath(path).isEmpty()
						|| !metaData.fileStamp(path).filter(stamp::equals).isPresent()) {
					index(file, path, stamp);
				}
			}
		}
		stalePaths.forEach(metaData::removeFile);
	}

	private void index(Path file) throws IOException {
		var path = PathUtil.toRelativeEntry(file, collectionsBase);
		index(file, path, fileStamp(file));
	}

	private void index(Path file, String path, String stamp) throws IOException {
		collectionNames.add(path.substring(0, path.indexOf('/')));
		var attributes = Files.readAttributes(file, BasicFileAttributes.class);
		var modified = LocalDate.ofInstant(
				attributes.lastModifiedTime().toInstant(), ZoneId.systemDefault());
		metaData.addFile(path, metaParser.apply(file), modified, stamp);
	}

	private static String fileStamp(Path file) throws IOException {
		var attributes = Files.readAttributes(file, BasicFileAttributes.class);
		return attributes.lastModifiedTime().toMillis() + ":" + attributes.size();
	}

	private CollectionItem map(ContentNode node, int ignoredExcerptLength) {
		var path = node.path();
		var separator = path.indexOf('/');
		var collection = path.substring(0, separator);
		var filename = path.substring(separator + 1);
		var id = filename.substring(0, filename.length() - 3);
		return new CollectionItem(
				id, collection, path, readMarkdownBody(collectionsBase.resolve(path)), node.data());
	}

	private CollectionItemMetadata mapMetadata(ContentNode node, int ignoredExcerptLength) {
		var path = node.path();
		var separator = path.indexOf('/');
		var collection = path.substring(0, separator);
		var filename = path.substring(separator + 1);
		var id = filename.substring(0, filename.length() - 3);
		return new CollectionItemMetadata(id, collection, path, node.data());
	}

	private static String readMarkdownBody(Path file) {
		try {
			return new ContentFileParser(file.toString()).getContent();
		} catch (IOException ex) {
			throw new UncheckedIOException("could not read collection item " + file, ex);
		}
	}

	private static boolean isValidItemFile(Path file) {
		var filename = file.getFileName().toString();
		return filename.endsWith(".md")
				&& CollectionItemId.isValid(filename.substring(0, filename.length() - 3));
	}

	private static String itemPath(String collection, String id) {
		return collection + "/" + id + ".md";
	}

	private static boolean isValidCollectionName(String name) {
		return name != null && COLLECTION_NAME.matcher(name).matches();
	}

	private static void validateCollectionName(String name) {
		if (!isValidCollectionName(name)) {
			throw new IllegalArgumentException("invalid collection name: " + name);
		}
	}

	private void validateExistingCollection(String collection) {
		validateCollectionName(collection);
		if (!collectionNames.contains(collection)) {
			throw new IllegalArgumentException("collection not found: " + collection);
		}
	}

	@Override
	public void close() throws IOException {
		if (watcher != null) {
			watcher.stop();
		}
		if (changeCoordinator != null) {
			changeCoordinator.close();
		}
		if (metaData != null) {
			metaData.close();
		}
	}

	private class FileCollection implements Collection {

		private final String name;

		private FileCollection(String name) {
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public Optional<CollectionItem> item(String id) {
			CollectionItemId.requireValid(id);
			return metaData.byPath(itemPath(name, id))
					.filter(NodeVisibility::isVisible)
					.map(node -> FileSystemCollectionRepository.this.map(node, 0));
		}

		@Override
		public ContentQuery<CollectionItem> query() {
			return metaData.query(name, FileSystemCollectionRepository.this::map);
		}

		@Override
		public ContentQuery<CollectionItemMetadata> metadataQuery() {
			return metaData.query(name, FileSystemCollectionRepository.this::mapMetadata);
		}
	}
}
