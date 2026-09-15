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

import com.condation.cms.api.exceptions.AccessNotAllowedException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemContentStoreTest {

	@TempDir
	Path tempDirectory;

	private Path contentRoot;
	private FileSystemContentStore store;

	@BeforeEach
	void setUp() throws Exception {
		contentRoot = Files.createDirectories(tempDirectory.resolve("content"));
		store = new FileSystemContentStore(new NIOReadOnlyFile(contentRoot, contentRoot));
	}

	@Test
	void getsResourcesFromTheExistingReadOnlyFileSystem() throws Exception {
		var page = contentRoot.resolve("articles/hello.md");
		Files.createDirectories(page.getParent());
		Files.writeString(page, "Grüße aus dem Store", StandardCharsets.UTF_8);

		var resource = store.get("/articles\\hello.md");

		Assertions.assertThat(resource).isPresent();
		Assertions.assertThat(resource.get().path()).isEqualTo("articles/hello.md");
		Assertions.assertThat(resource.get().directory()).isFalse();
		Assertions.assertThat(resource.get().content()).isEqualTo("Grüße aus dem Store");
		Assertions.assertThat(resource.get().lastModified()).isNotNull();
		Assertions.assertThat(store.exists("articles/hello.md")).isTrue();
		Assertions.assertThat(store.get("missing.md")).isEmpty();
	}

	@Test
	void listsOnlyDirectChildrenWithStoreRelativePaths() throws Exception {
		var articles = Files.createDirectories(contentRoot.resolve("articles/nested"));
		Files.writeString(contentRoot.resolve("articles/first.md"), "first");
		Files.writeString(articles.resolve("second.md"), "second");

		Assertions.assertThat(store.list("articles"))
				.extracting(resource -> resource.path())
				.containsExactlyInAnyOrder("articles/first.md", "articles/nested");
		Assertions.assertThat(store.list("articles/first.md")).isEmpty();
		Assertions.assertThat(store.list("missing")).isEmpty();
	}

	@Test
	void keepsPathTraversalProtectionOfTheExistingFileSystem() {
		Assertions.assertThatThrownBy(() -> store.get("../outside.md"))
				.isInstanceOf(AccessNotAllowedException.class);
	}
}
