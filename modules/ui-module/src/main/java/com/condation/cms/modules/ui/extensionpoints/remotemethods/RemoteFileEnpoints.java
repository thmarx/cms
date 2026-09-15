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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.Constants;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.eventbus.events.ReIndexContentMetaDataEvent;
import com.condation.cms.api.feature.features.EventBusFeature;
import com.condation.cms.api.ui.extensions.UIRemoteMethodExtensionPoint;
import com.condation.cms.api.utils.FileUtils;
import com.condation.cms.api.utils.SectionUtil;
import com.condation.modules.api.annotation.Extension;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.condation.cms.api.ui.annotations.RemoteMethod;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.utils.PathUtil;
import com.condation.cms.modules.ui.utils.UIPathUtil;
import com.condation.cms.core.content.io.ContentFileParser;
import com.condation.cms.core.content.io.YamlHeaderUpdater;
import java.nio.file.Path;

/**
 *
 * @author thorstenmarx
 */
@Slf4j
@Extension(UIRemoteMethodExtensionPoint.class)
public class RemoteFileEnpoints extends AbstractRemoteMethodeExtension {

	private static final String SUCCESS = "success";

	@RemoteMethod(name = "files.list", permissions = {Permissions.CONTENT_EDIT})
	public Object list(Map<String, Object> parameters) throws RPCException {
		var uri = (String) parameters.getOrDefault("uri", "");
		if (uri == null) {
			uri = "";
		}
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		var type = (String) parameters.get("type");
		if (CONTENT.equals(type)) {
			return listContent(parameters, uri);
		}
		final DB db = getDB(parameters);
		var contentBase = getBase(db.getFileSystem(), type);

		var contentFile = contentBase.resolve(uri);

		Map<String, Object> result = new HashMap<>();
		result.put("uri", uri);

		List<File> files = new ArrayList<>();
		if (contentFile.isDirectory()) {
			try {
				if (contentFile.hasParent()) {
					var parent = contentFile.getParent();
					files.add(new Directory("..", parent.uri()));
				}
				contentFile.children().stream()
						.filter(child -> !SectionUtil.isSectionEntry(child.getFileName()))
						.filter(child -> !".variants".equals(child.getFileName()))
						.map(this::map)
						.forEach(files::add);
			} catch (IOException ex) {
				log.error("", ex);
				throw new RPCException(0, ex.getMessage());
			}
		}
		files.sort((f1, f2) -> {
			if (f1.directory() && !f2.directory()) {
				return -1;
			} else if (!f1.directory() && f2.directory()) {
				return 1;
			} else {
				return f1.displayName().compareToIgnoreCase(f2.displayName());
			}
		});

		result.put("files", files);

		return result;
	}

	@RemoteMethod(name = "files.delete", permissions = {Permissions.CONTENT_EDIT})
	public Object delete(Map<String, Object> parameters) throws RPCException {
		Map<String, Object> result = new HashMap<>();

		try {
			var uri = (String) parameters.getOrDefault("uri", "");
			var name = (String) parameters.getOrDefault("name", "");
			var type = (String) parameters.get("type");
			if (CONTENT.equals(type)) {
				var repository = getMutableContentRepository(parameters);
				var path = join(uri, name);
				var node = repository.get(path);
				var sections = node.isPresent() ? repository.sections(node.get()) : List.<com.condation.cms.api.repository.Section>of();
				repository.deleteRecursively(path);
				for (var section : sections) {
					repository.deleteRecursively(section.id());
				}
				return result;
			}
			final DB db = getDB(parameters);
			var contentBase = getBase(db.getFileSystem(), type);

			var contentFile = contentBase.resolve(uri).resolve(name);

			var writableBase = getWritableBase(db.getFileSystem(), type);

			log.debug("deleting file {}", contentFile.uri());
			if (contentFile.isDirectory()) {
				FileUtils.deleteFolder(writableBase.resolve(uri).resolve(name));
			} else if ("assets".equals(type)) {
				Files.deleteIfExists(writableBase.resolve(uri).resolve(name));
			}
		} catch (Exception e) {
			log.error("", e);
			throw new RPCException(0, e.getMessage());
		}

		return result;
	}

	@RemoteMethod(name = "files.rename", permissions = {Permissions.CONTENT_EDIT})
	public Object renameFile(Map<String, Object> parameters) throws RPCException {
		Map<String, Object> result = new HashMap<>();

		try {
			var uri = (String) parameters.getOrDefault("uri", "");
			var name = (String) parameters.getOrDefault("name", "");
			var newName = (String) parameters.get("newName");
			var type = (String) parameters.get("type");

			if (newName == null || newName.isBlank()) {
				throw new IllegalArgumentException("newName must not be null or blank");
			}
			if (CONTENT.equals(type)) {
				var repository = getMutableContentRepository(parameters);
				var source = join(uri, name);
				if (name.endsWith(".md")) {
					var node = repository.get(source).orElseThrow(
							() -> new RPCException("Source content not found: " + source));
					var document = repository.load(node).orElseThrow();
					var metadata = new HashMap<>(node.data());
					metadata.put(Constants.MetaFields.TITLE, newName.trim());
					repository.save(source, metadata, document.content());
					result.put(SUCCESS, true);
					result.put("newName", name);
					result.put("title", newName.trim());
				} else {
					repository.move(source, join(uri, newName));
					result.put(SUCCESS, true);
					result.put("newName", newName);
				}
				return result;
			}

			final DB db = getDB(parameters);
			var contentBase = getBase(db.getFileSystem(), type);
			
			// check if both paths are in host directory
			contentBase.resolve(uri).resolve(name);
			contentBase.resolve(uri).resolve(newName);
			
			var writableBase = getWritableBase(db.getFileSystem(), type);

			var sourcePath = writableBase.resolve(uri).resolve(name);
			var targetPath = writableBase.resolve(uri).resolve(newName);

			if (!Files.exists(sourcePath)) {
				throw new RPCException("Source file not found: " + sourcePath);
			}
			if (!"assets".equals(type) && name.endsWith(".md") && !Files.isDirectory(sourcePath)) {
				var parser = new ContentFileParser(sourcePath.toString());
				var metadata = parser.getHeader() == null
						? new HashMap<String, Object>()
						: new HashMap<>(parser.getHeader());
				metadata.put(Constants.MetaFields.TITLE, newName.trim());
				YamlHeaderUpdater.saveMarkdownFileWithHeader(
						sourcePath,
						metadata,
						parser.getContent()
				);
				getContext().get(EventBusFeature.class).eventBus().syncPublish(
						new ReIndexContentMetaDataEvent(PathUtil.toRelativeFile(sourcePath, writableBase))
				);
				db.getFileSystem().flushContentChanges();
				result.put(SUCCESS, true);
				result.put("newName", name);
				result.put("title", newName.trim());
				return result;
			}

			log.debug("renaming from {} to {}", sourcePath, targetPath);
			if (Files.exists(targetPath)) {
				throw new RPCException("Target file already exists: " + targetPath);
			}

			Files.move(sourcePath, targetPath);

			result.put(SUCCESS, true);
			result.put("newName", newName);

		} catch (Exception e) {
			log.error("Error during rename", e);
			throw new RPCException(0, e.getMessage());
		}

		return result;
	}

	@RemoteMethod(name = "folders.create", permissions = {Permissions.CONTENT_EDIT})
	public Object createFolder(Map<String, Object> parameters) throws RPCException {
		Map<String, Object> result = new HashMap<>();

		try {
			var name = (String) parameters.getOrDefault("name", "");
			var uri = (String) parameters.getOrDefault("uri", "");
			var type = (String) parameters.get("type");
			if (CONTENT.equals(type)) {
				getMutableContentRepository(parameters).createDirectory(join(uri, UIPathUtil.slugify(name)));
				return result;
			}
			final DB db = getDB(parameters);
			var contentBase = getWritableBase(db.getFileSystem(), type);

			name = UIPathUtil.slugify(name);
			
			Path newFile = contentBase.resolve(uri).resolve(name);
			if (newFile.isAbsolute()) {
				throw new RPCException(1, "absolut path is not supported");
			} else if (Files.exists(newFile)) {
				throw new RPCException(1, "directory already exists");
			} else if (!PathUtil.isChild(contentBase, newFile)) {
				throw new RPCException(1, "invalid path");
			}
			Files.createDirectories(newFile);
		} catch (Exception e) {
			log.error("", e);
			throw new RPCException(0, e.getMessage());
		}

		return result;
	}

	@RemoteMethod(name = "files.create", permissions = {Permissions.CONTENT_EDIT})
	public Object createFile(Map<String, Object> parameters) throws RPCException {
		Map<String, Object> result = new HashMap<>();

		try {
			var uri = (String) parameters.getOrDefault("uri", "");
			var name = (String) parameters.getOrDefault("name", "");
			var type = (String) parameters.get("type");
			if (CONTENT.equals(type)) {
				var path = join(uri, UIPathUtil.slugify(name));
				getMutableContentRepository(parameters).save(path, Map.of(), "");
				return result;
			}
			final DB db = getDB(parameters);
			var contentBase = getWritableBase(db.getFileSystem(), type);

			name = UIPathUtil.slugify(name);
			
			Path newFile = contentBase.resolve(uri).resolve(name);
			if (newFile.isAbsolute()) {
				throw new RPCException(1, "absolut path is not supported");
			} else if (Files.exists(newFile)) {
				throw new RPCException(1, "file already exists");
			} else if (!PathUtil.isChild(contentBase, newFile)) {
				throw new RPCException(1, "invalid path");
			}
			Files.createDirectories(newFile.getParent());
			Files.createFile(newFile);
		} catch (Exception e) {
			log.error("", e);
			throw new RPCException(0, e.getMessage());
		}

		return result;
	}

	private boolean isMedia (String filename) {
		var name = filename.toLowerCase();
		return name.endsWith(".jpg")
				|| name.endsWith(".jpeg")
				|| name.endsWith(".webp")
				|| name.endsWith(".avif")
				|| name.endsWith(".png")
				|| name.endsWith(".svg")
				|| name.endsWith(".gif");
	}

	private Object listContent(Map<String, Object> parameters, String uri) {
		var repository = getContentRepository(parameters);
		List<File> files = repository.children(uri).stream()
				.filter(node -> !node.isSectionEntry())
				.filter(node -> !".variants".equals(node.name()))
				.map(node -> (File) (node.isDirectory()
						? new Directory(node.name(), node.path())
						: new Content(node.name(), node.path(), node.url(),
								node.getMetaValue(Constants.MetaFields.TITLE, node.name()))))
				.collect(java.util.stream.Collectors.toCollection(ArrayList::new));
		var parent = parentPath(uri);
		if (!uri.isBlank()) {
			files.add(new Directory("..", parent));
		}
		files.sort((first, second) -> {
			if (first.directory() != second.directory()) {
				return first.directory() ? -1 : 1;
			}
			return first.displayName().compareToIgnoreCase(second.displayName());
		});
		return Map.of("uri", uri, "files", files);
	}

	private static String join(String parent, String child) {
		return parent == null || parent.isBlank() ? child : parent + "/" + child;
	}

	private static String parentPath(String path) {
		var separator = path.lastIndexOf('/');
		return separator < 0 ? "" : path.substring(0, separator);
	}
	
	private File map(ReadOnlyFile readOnlyFile) {
		if (readOnlyFile.isDirectory()) {
			return new Directory(
						readOnlyFile.getFileName(),
						readOnlyFile.uri()
			);
		} else if (isMedia(readOnlyFile.getFileName())) {
			return new Media(
					readOnlyFile.getFileName(), 
					readOnlyFile.uri()
			);
		} else {
			return new Content(
					readOnlyFile.getFileName(),
					readOnlyFile.uri(),
					readOnlyFile.uri(),
					readOnlyFile.getFileName()
			);
		}
	}
	
	public record Content(String name, String uri, String url, String title) implements File {
		@Override
		public String displayName() {
			return title;
		}
	}
	
	public record Media(String name, String uri) implements File {
		@Override
		public boolean media() {
			return true;
		}		
	}
	
	public record Directory (String name, String uri) implements File {
		@Override
		public boolean directory() {
			return true;
		}
	}
	
	public static interface File {
		String name ();
		String uri ();
		default String displayName () {
			return name();
		}
		default boolean directory () {
			return false;
		}
		default boolean content () {
			return name().endsWith(".md");
		}
		default boolean media () {
			return false;
		}
	}
}
