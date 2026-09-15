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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.condation.cms.api.db.DBFileSystem;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.repository.ContentResource;
import com.condation.cms.api.repository.ContentStore;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link ContentStore} adapter for the existing CMS file system.
 *
 * @author thmar
 */
public final class FileSystemContentStore implements ContentStore {

	private final ReadOnlyFile contentRoot;

	public FileSystemContentStore(DBFileSystem fileSystem) {
		this(fileSystem.contentBase());
	}

	public FileSystemContentStore(ReadOnlyFile contentRoot) {
		this.contentRoot = Objects.requireNonNull(contentRoot);
	}

	@Override
	public Optional<ContentResource> get(String path) {
		var normalizedPath = normalize(path);
		var file = contentRoot.resolve(normalizedPath);
		if (!file.exists()) {
			return Optional.empty();
		}
		return Optional.of(new FileSystemContentResource(normalizedPath, file));
	}

	@Override
	public Stream<ContentResource> list(String path) throws IOException {
		var normalizedPath = normalize(path);
		var directory = contentRoot.resolve(normalizedPath);
		if (!directory.exists() || !directory.isDirectory()) {
			return Stream.empty();
		}

		return directory.children().stream()
				.map(child -> new FileSystemContentResource(childPath(normalizedPath, child), child))
				.map(ContentResource.class::cast);
	}

	private static String childPath(String parentPath, ReadOnlyFile child) {
		return parentPath.isEmpty()
				? child.getFileName()
				: parentPath + "/" + child.getFileName();
	}

	private static String normalize(String path) {
		Objects.requireNonNull(path, "path");
		var normalized = path.trim().replace('\\', '/');
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		while (normalized.endsWith("/") && !normalized.isEmpty()) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return ".".equals(normalized) ? "" : normalized;
	}

	private record FileSystemContentResource(String path, ReadOnlyFile file) implements ContentResource {

		@Override
		public boolean directory() {
			return file.isDirectory();
		}

		@Override
		public Instant lastModified() throws IOException {
			return Instant.ofEpochMilli(file.getLastModifiedTime());
		}

		@Override
		public String content(Charset charset) throws IOException {
			return file.getContent(charset);
		}
	}
}
