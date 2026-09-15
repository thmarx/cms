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

import com.condation.cms.api.repository.CollectionAccess;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemCollectionRepositoryTest {

	@TempDir
	Path tempDirectory;

	private FileSystemCollectionRepository repository;

	@BeforeEach
	void setUp() throws Exception {
		Files.createDirectories(tempDirectory.resolve("collections/blog"));
		repository = new FileSystemCollectionRepository(
				"test-site", tempDirectory, _ -> Map.of("status", "published"));
		repository.init();
	}

	@AfterEach
	void tearDown() throws Exception {
		repository.close();
	}

	@Test
	void createsSavesAndDeletesItemsBehindTheRepositoryBoundary() throws Exception {
		repository.create("blog", "entry", Map.of("title", "First"), "First body");
		var item = tempDirectory.resolve("collections/blog/entry.md");
		Assertions.assertThat(item)
				.exists()
				.content()
				.contains("title: First", "First body");
		Assertions.assertThat(repository.get("blog", "entry"))
				.isPresent()
				.get()
				.extracting(result -> result.content())
				.isEqualTo("First body");

		repository.save("blog", "entry", Map.of("title", "Changed"), "Changed body");
		Assertions.assertThat(item).content().contains("title: Changed", "Changed body");

		repository.delete("blog", "entry");
		Assertions.assertThat(item).doesNotExist();
		Assertions.assertThat(repository.get("blog", "entry")).isEmpty();
	}

	@Test
	void rejectsOverwritesTraversalAndUnknownCollections() throws Exception {
		repository.create("blog", "entry", Map.of(), "body");

		Assertions.assertThatThrownBy(
				() -> repository.create("blog", "entry", Map.of(), "other"))
				.isInstanceOf(FileAlreadyExistsException.class);
		Assertions.assertThatThrownBy(
				() -> repository.save("blog", "../entry", Map.of(), "other"))
				.isInstanceOf(IllegalArgumentException.class);
		Assertions.assertThatThrownBy(
				() -> repository.save("missing", "entry", Map.of(), "other"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not found");
		Assertions.assertThat(repository.access("blog")).isEqualTo(CollectionAccess.READ_WRITE);
	}
}
