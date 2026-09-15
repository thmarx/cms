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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.condation.cms.api.db.Content;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.db.DBFileSystem;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.content.ContentParser;
import com.condation.cms.api.repository.ContentResource;
import com.condation.cms.api.repository.ContentStore;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemContentRepositoryTest {

	@TempDir
	Path contentBase;

	private Content content;
	private DBFileSystem fileSystem;
	private ContentParser contentParser;
	private ContentStore contentStore;
	private ReadOnlyFile contentRoot;
	private FileSystemContentRepository repository;

	@BeforeEach
	void setUp() {
		content = mock(Content.class);
		fileSystem = mock(DBFileSystem.class);
		contentRoot = mock(ReadOnlyFile.class);
		contentParser = mock(ContentParser.class);
		contentStore = mock(ContentStore.class);
		when(fileSystem.contentBase()).thenReturn(contentRoot);
		when(fileSystem.resolve(com.condation.cms.api.Constants.Folders.CONTENT))
				.thenReturn(contentBase);
		repository = new FileSystemContentRepository(content, fileSystem, contentStore, contentParser);
	}

	@Test
	void delegatesNodeLookupToExistingContentImplementation() {
		var page = node("articles/page.md");
		when(content.byPath(page.path())).thenReturn(java.util.Optional.of(page));

		Assertions.assertThat(repository.get(page.path())).contains(page);
	}

	@Test
	void loadsParsedContentThroughTheContentStore() throws Exception {
		var page = node("articles/page.md");
		var resource = resource(page.path(), false, "---\ntitle: Page\n---\nbody");
		when(contentStore.get(page.path())).thenReturn(Optional.of(resource));
		when(contentParser.parse(resource)).thenReturn(new ContentParser.Content("body", page.data()));

		Assertions.assertThat(repository.load(page))
				.hasValueSatisfying(document -> {
					Assertions.assertThat(document.node()).isEqualTo(page);
					Assertions.assertThat(document.content()).isEqualTo("body");
				});
		verify(contentParser).parse(resource);
	}

	@Test
	void exposesSectionsAsDomainObjectsWithoutLeakingFilenameMatchingToCallers() throws Exception {
		var owner = node("articles/page.md");
		var ownerFile = mock(ReadOnlyFile.class);
		var hero = node("articles/page.hero.md");
		var secondHero = node("articles/page.hero.secondary.md");
		var sidebar = node("articles/page.sidebar.md");
		var heroResource = resource(hero.path(), false, "hero");
		var secondHeroResource = resource(secondHero.path(), false, "secondary");
		var sidebarResource = resource(sidebar.path(), false, "sidebar");
		when(contentRoot.resolve(owner.path())).thenReturn(ownerFile);
		when(content.listSectionEntries(ownerFile)).thenReturn(List.of(hero, secondHero, sidebar));
		when(contentStore.get(hero.path())).thenReturn(Optional.of(heroResource));
		when(contentStore.get(secondHero.path())).thenReturn(Optional.of(secondHeroResource));
		when(contentStore.get(sidebar.path())).thenReturn(Optional.of(sidebarResource));
		when(contentParser.parse(heroResource)).thenReturn(new ContentParser.Content("hero", hero.data()));
		when(contentParser.parse(secondHeroResource)).thenReturn(new ContentParser.Content("secondary", secondHero.data()));
		when(contentParser.parse(sidebarResource)).thenReturn(new ContentParser.Content("sidebar", sidebar.data()));

		Assertions.assertThat(repository.sections(owner))
				.extracting(section -> section.id(), section -> section.owner(),
						section -> section.name(), section -> section.content())
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(hero.path(), owner.path(), "hero", "hero"),
						org.assertj.core.groups.Tuple.tuple(secondHero.path(), owner.path(), "hero", "secondary"),
						org.assertj.core.groups.Tuple.tuple(sidebar.path(), owner.path(), "sidebar", "sidebar"));
		org.mockito.Mockito.clearInvocations(contentParser);
		Assertions.assertThat(repository.sections(owner, "hero"))
				.extracting(section -> section.id())
				.containsExactly(hero.path(), secondHero.path());
		verify(contentParser, never()).parse(sidebarResource);
	}

	@Test
	void resolvesVariantsAndCanonicalContextInsideTheRepository() throws Exception {
		var canonical = node("articles/page.md");
		var blue = node("articles/.variants/page/blue/page.md");
		var red = node("articles/.variants/page/red/page.md");
		when(contentStore.list("articles/.variants/page")).thenReturn(Stream.of(
				resource("articles/.variants/page/blue", true, ""),
				resource("articles/.variants/page/red", true, "")));
		when(content.byPath(blue.path())).thenReturn(Optional.of(blue));
		when(content.byPath(red.path())).thenReturn(Optional.of(red));
		when(content.byPath(canonical.path())).thenReturn(Optional.of(canonical));

		Assertions.assertThat(repository.variants(canonical))
				.extracting(variant -> variant.id(), variant -> variant.node())
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("blue", blue),
						org.assertj.core.groups.Tuple.tuple("red", red));

		when(contentStore.list("articles/.variants/page")).thenReturn(Stream.of(
				resource("articles/.variants/page/blue", true, ""),
				resource("articles/.variants/page/red", true, "")));
		var context = repository.variantContext(blue);
		Assertions.assertThat(context.canonical()).isEqualTo(canonical);
		Assertions.assertThat(context.isVariant()).isTrue();
		Assertions.assertThat(context.activeVariantId()).contains("blue");
		Assertions.assertThat(context.variants()).hasSize(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	void reusesTheExistingQueryApi() {
		ContentQuery<ContentNode> allContentQuery = mock(ContentQuery.class);
		ContentQuery<ContentNode> scopedQuery = mock(ContentQuery.class);
		when(content.<ContentNode>query(any())).thenReturn(allContentQuery);
		when(content.<ContentNode>query(org.mockito.ArgumentMatchers.eq("articles"), any()))
				.thenReturn(scopedQuery);

		Assertions.assertThat(repository.query()).isSameAs(allContentQuery);
		Assertions.assertThat(repository.query("articles")).isSameAs(scopedQuery);
		verify(content).query(org.mockito.ArgumentMatchers.eq("articles"), any());
	}

	@Test
	void writesMovesAndDeletesContentBehindTheMutableRepositoryBoundary() throws Exception {
		repository.save("drafts/page.md", Map.of("title", "Draft"), "body");
		var source = contentBase.resolve("drafts/page.md");
		Assertions.assertThat(Files.readString(source))
				.contains("title: Draft", "body");

		repository.move("drafts/page.md", "published/page.md");
		var target = contentBase.resolve("published/page.md");
		Assertions.assertThat(source).doesNotExist();
		Assertions.assertThat(target).exists();

		repository.deleteRecursively("published");
		Assertions.assertThat(contentBase.resolve("published")).doesNotExist();
		verify(fileSystem, times(3)).flushContentChanges();
	}

	@Test
	void rejectsWritesOutsideTheContentRoot() {
		Assertions.assertThatThrownBy(() -> repository.save("../outside.md", Map.of(), ""))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static ContentNode node(String path) {
		var name = path.substring(path.lastIndexOf('/') + 1);
		return new ContentNode(path, "/" + path.replace(".md", ""), name, Map.of());
	}

	private static ContentResource resource(String path, boolean directory, String content) {
		return new ContentResource() {
			@Override
			public String path() {
				return path;
			}

			@Override
			public boolean directory() {
				return directory;
			}

			@Override
			public Instant lastModified() {
				return Instant.EPOCH;
			}

			@Override
			public String content(Charset charset) throws IOException {
				return content;
			}
		};
	}
}
