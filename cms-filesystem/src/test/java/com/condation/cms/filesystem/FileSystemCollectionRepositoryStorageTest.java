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

import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.feature.features.IsPreviewFeature;
import com.condation.cms.api.feature.features.WorkflowFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.request.RequestContextScope;
import com.condation.cms.api.workflow.WFStatusProvider;
import com.condation.cms.api.workflow.WFStatusQueryProvider;
import com.condation.cms.api.workflow.WorkflowInstance;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

class FileSystemCollectionRepositoryStorageTest {

	@TempDir
	Path tempDirectory;

	@Test
	void queriesCollectionsWithFilteringSortingPagingAndRawMarkdown() throws Exception {
		write("blog/first.md", "title: First\nfeatured: true\nrank: 2", "# First");
		write("blog/second.md", "title: Second\nfeatured: false\nrank: 3", "**Second**");
		write("blog/third.md", "title: Third\nfeatured: true\nrank: 1", "_Third_");
		write("authors/first.md", "title: Author\nfeatured: true\nrank: 0", "Author body");
		write("blog/nested/ignored.md", "title: Ignored", "Ignored body");

		var collections = createCollections();
		try {
			Assertions.assertThat(collections.names()).containsExactlyInAnyOrder("blog", "authors");

			var page = collections.collection("blog")
					.query()
					.where("featured", true)
					.orderby("rank")
					.asc()
					.page(1, 1);

			Assertions.assertThat(page.getTotalItems()).isEqualTo(2);
			Assertions.assertThat(page.getTotalPages()).isEqualTo(2);
			Assertions.assertThat(page.getItems())
					.singleElement()
					.satisfies(item -> {
						Assertions.assertThat(item.id()).isEqualTo("third");
						Assertions.assertThat(item.collection()).isEqualTo("blog");
						Assertions.assertThat(item.path()).isEqualTo("blog/third.md");
						Assertions.assertThat(item.content()).isEqualTo("_Third_");
						Assertions.assertThat(item.meta()).containsEntry("title", "Third");
					});

			Assertions.assertThat(collections.collection("authors").query().get())
					.extracting(item -> item.id())
					.containsExactly("first");
		} finally {
			collections.close();
		}
	}

	@Test
	void appliesFlatFileChangesIncrementally() throws Exception {
		var item = write("blog/item.md", "title: Before\nfeatured: false", "Before");
		var collections = createCollections();
		try {
			write("blog/item.md", "title: After\nfeatured: true", "After");
			collections.handleEvent(new FileEvent(item.toFile(), FileEvent.Type.MODIFIED));
			collections.flushChanges();

			Assertions.assertThat(collections.collection("blog").query().where("featured", true).get())
					.singleElement()
					.satisfies(result -> {
						Assertions.assertThat(result.meta()).containsEntry("title", "After");
						Assertions.assertThat(result.content()).isEqualTo("After");
					});

			Files.delete(item);
			collections.handleEvent(new FileEvent(item.toFile(), FileEvent.Type.DELETED));
			collections.flushChanges();
			Assertions.assertThat(collections.collection("blog").query().get()).isEmpty();
		} finally {
			collections.close();
		}
	}

	@Test
	void searchesCollectionTitlesWithPaging() throws Exception {
		write("blog/first.md", "title: Collection Article One", "First");
		write("blog/second.md", "title: Collection Article Two", "Second");
		write("blog/other.md", "title: Unrelated Entry", "Other");

		var collections = createCollections();
		try {
			var page = collections.collection("blog")
					.query()
					.searchByTitle("Collection Article")
					.orderby("title")
					.asc()
					.page(2, 1);

			Assertions.assertThat(page.getTotalItems()).isEqualTo(2);
			Assertions.assertThat(page.getTotalPages()).isEqualTo(2);
			Assertions.assertThat(page.getItems())
					.extracting(item -> item.id())
					.containsExactly("second");
		} finally {
			collections.close();
		}
	}

	@Test
	void queriesMetadataWithoutReturningMarkdownBodies() throws Exception {
		write("blog/first.md", "title: First", "Large body");
		var collections = createCollections();
		try {
			var result = collections.collection("blog").metadataQuery().page(1, 10);

			Assertions.assertThat(result.getItems())
					.singleElement()
					.satisfies(item -> {
						Assertions.assertThat(item.id()).isEqualTo("first");
						Assertions.assertThat(item.meta()).containsEntry("title", "First");
					});
		} finally {
			collections.close();
		}
	}

	@Test
	void reusesPersistedCollectionIndexWhenFilesAreUnchanged() throws Exception {
		write("blog/first.md", "title: First", "First");
		write("blog/second.md", "title: Second", "Second");
		var parseCount = new AtomicInteger();
		var parser = (java.util.function.Function<Path, Map<String, Object>>) path -> {
			parseCount.incrementAndGet();
			return parseMeta(path);
		};

		var first = new FileSystemCollectionRepository("test-site", tempDirectory, parser);
		first.init();
		first.close();
		Assertions.assertThat(parseCount).hasValue(2);

		var second = new FileSystemCollectionRepository("test-site", tempDirectory, parser);
		try {
			second.init();
			Assertions.assertThat(parseCount).hasValue(2);
			Assertions.assertThat(second.collection("blog").metadataQuery().page(1, 10).getTotalItems())
					.isEqualTo(2);
		} finally {
			second.close();
		}
	}

	@Test
	void explicitReindexReparsesUnchangedCollectionItems() throws Exception {
		write("blog/first.md", "title: First", "First");
		write("blog/second.md", "title: Second", "Second");
		var parseCount = new AtomicInteger();
		var collections = new FileSystemCollectionRepository("test-site", tempDirectory, path -> {
			parseCount.incrementAndGet();
			return parseMeta(path);
		});

		try {
			collections.init();
			collections.reindex();

			Assertions.assertThat(parseCount).hasValue(4);
			Assertions.assertThat(collections.collection("blog").metadataQuery().get())
					.extracting(item -> item.id())
					.containsExactlyInAnyOrder("first", "second");
		} finally {
			collections.close();
		}
	}

	@Test
	void pagesCollectionsWithAnOpaqueCursorAndExpiresItAfterIndexChanges() throws Exception {
		write("blog/first.md", "title: A", "First");
		write("blog/second.md", "title: B", "Second");
		write("blog/third.md", "title: C", "Third");
		var collections = createCollections();
		try {
			var firstPage = collections.metadataCursorPage(
					"blog", null, 2, query -> query.orderby("title").asc());
			var secondPage = collections.metadataCursorPage(
					"blog", firstPage.nextCursor(), 2, query -> query.orderby("title").asc());

			Assertions.assertThat(firstPage.items()).extracting(item -> item.id())
					.containsExactly("first", "second");
			Assertions.assertThat(firstPage.hasNext()).isTrue();
			Assertions.assertThat(secondPage.items()).extracting(item -> item.id())
					.containsExactly("third");
			Assertions.assertThat(secondPage.hasNext()).isFalse();

			write("blog/third.md", "title: D", "Changed");
			collections.refresh("blog", "third");
			Assertions.assertThatIllegalArgumentException().isThrownBy(() ->
					collections.metadataCursorPage(
							"blog",
							firstPage.nextCursor(),
							2,
							query -> query.orderby("title").asc()))
					.withMessageContaining("cursor has expired");
		} finally {
			collections.close();
		}
	}

	@Test
	void regularPageUsesServerSideSeekingWithoutAnExternalCursor() throws Exception {
		write("blog/first.md", "title: A", "First");
		write("blog/second.md", "title: B", "Second");
		write("blog/third.md", "title: C", "Third");
		write("blog/fourth.md", "title: D", "Fourth");
		write("blog/fifth.md", "title: E", "Fifth");
		var collections = createCollections();
		try {
			var query = collections.collection("blog").metadataQuery();
			var page = query
					.orderby("title").asc()
					.page(2, 2);

			Assertions.assertThat(page.getTotalItems()).isEqualTo(5);
			Assertions.assertThat(page.getTotalPages()).isEqualTo(3);
			Assertions.assertThat(page.getPage()).isEqualTo(2);
			Assertions.assertThat(page.getItems()).extracting(item -> item.id())
					.containsExactly("third", "fourth");
			Assertions.assertThat(query.getClass().getMethods())
					.noneMatch(method -> method.getName().equals("cursorPage")
							|| method.getName().equals("seekPage"));
		} finally {
			collections.close();
		}
	}

	@Test
	void refreshesOneCollectionItemImmediately() throws Exception {
		write("blog/item.md", "title: Before", "Before");
		var collections = createCollections();
		try {
			write("blog/item.md", "title: After", "After");
			collections.refresh("blog", "item");

			Assertions.assertThat(collections.collection("blog").item("item"))
					.isPresent()
					.get()
					.satisfies(item -> {
						Assertions.assertThat(item.meta()).containsEntry("title", "After");
						Assertions.assertThat(item.content()).isEqualTo("After");
					});
		} finally {
			collections.close();
		}
	}

	@Test
	void preservesMarkdownHorizontalRulesOutsideFrontMatter() throws Exception {
		write(
				"blog/item.md",
				"title: Horizontal rules",
				"Introduction\n\n---\n\nMiddle section\n\n---\n\nConclusion");
		var collections = createCollections();
		try {
			Assertions.assertThat(collections.collection("blog").item("item"))
					.isPresent()
					.get()
					.extracting(item -> item.content())
					.isEqualTo("Introduction\n\n---\n\nMiddle section\n\n---\n\nConclusion");
		} finally {
			collections.close();
		}
	}

	@Test
	void preservesHorizontalRulesInMarkdownWithoutFrontMatter() throws Exception {
		var file = tempDirectory.resolve("collections/blog/item.md");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "Introduction\n\n---\n\nConclusion\n");
		var collections = createCollections();
		try {
			var previewContext = new RequestContext();
			previewContext.add(IsPreviewFeature.class, new IsPreviewFeature(IsPreviewFeature.Mode.PREVIEW));
			var optionalItem = ScopedValue.where(RequestContextScope.REQUEST_CONTEXT, previewContext)
					.call(() -> collections.collection("blog").item("item"));
			Assertions.assertThat(optionalItem)
					.isPresent()
					.get()
					.extracting(item -> item.content())
					.isEqualTo("Introduction\n\n---\n\nConclusion\n");
		} finally {
			collections.close();
		}
	}

	@Test
	void removesDeletedCollectionItemImmediatelyOnRefresh() throws Exception {
		var item = write("blog/item.md", "title: Before", "Before");
		var collections = createCollections();
		try {
			Files.delete(item);
			collections.refresh("blog", "item");

			Assertions.assertThat(collections.collection("blog").query().get()).isEmpty();
		} finally {
			collections.close();
		}
	}

	@Test
	void rejectsUnsafeCollectionNames() throws Exception {
		var collections = createCollections();
		try {
			Assertions.assertThatIllegalArgumentException()
					.isThrownBy(() -> collections.collection("../content"));
		} finally {
			collections.close();
		}
	}

	@Test
	void consistentlyRejectsInvalidItemIdsFromFilesAndDirectLookups() throws Exception {
		write("blog/valid-item.md", "title: Valid", "Valid");
		write("blog/invalid item.md", "title: Invalid", "Invalid");
		var collections = createCollections();
		try {
			Assertions.assertThat(collections.collection("blog").query().get())
					.extracting(item -> item.id())
					.containsExactly("valid-item");
			Assertions.assertThatIllegalArgumentException()
					.isThrownBy(() -> collections.collection("blog").item("invalid item"))
					.withMessage("invalid collection item id: invalid item");
		} finally {
			collections.close();
		}
	}

	@Test
	void appliesDefaultWorkflowSchedulingAndPreview() throws Exception {
		writeMeta("blog/published.md", "status: published", "Published");
		writeMeta("blog/draft.md", "status: draft", "Draft");
		writeMeta(
				"blog/future.md",
				"status: published\npublish_date: 2099-01-01T00:00:00Z",
				"Future");
		writeMeta(
				"blog/expired.md",
				"status: published\nunpublish_date: 2000-01-01T00:00:00Z",
				"Expired");

		var collections = createCollections();
		try {
			var publicPage = collections.collection("blog").query().page(1, 10);
			Assertions.assertThat(publicPage.getTotalItems()).isEqualTo(1);
			Assertions.assertThat(publicPage.getItems())
					.extracting(item -> item.id())
					.containsExactly("published");
			Assertions.assertThat(collections.collection("blog").item("published")).isPresent();
			Assertions.assertThat(collections.collection("blog").item("draft")).isEmpty();

			var previewContext = new RequestContext();
			previewContext.add(IsPreviewFeature.class, new IsPreviewFeature(IsPreviewFeature.Mode.PREVIEW));
			var previewItems = ScopedValue.where(RequestContextScope.REQUEST_CONTEXT, previewContext)
					.call(() -> collections.collection("blog").query().get());
			var previewDraft = ScopedValue.where(RequestContextScope.REQUEST_CONTEXT, previewContext)
					.call(() -> collections.collection("blog").item("draft"));

			Assertions.assertThat(previewItems)
					.extracting(item -> item.id())
					.containsExactlyInAnyOrder("published", "draft", "future", "expired");
			Assertions.assertThat(previewDraft).isPresent();
		} finally {
			collections.close();
		}
	}

	@Test
	void appliesConfiguredContentWorkflow() throws Exception {
		writeMeta("blog/approved.md", "status: draft\napproval: approved", "Approved");
		writeMeta("blog/blocked.md", "status: published\napproval: blocked", "Blocked");

		var collections = createCollections();
		try {
			var requestContext = new RequestContext();
			requestContext.add(
					WorkflowFeature.class,
					new WorkflowFeature(new WorkflowInstance("approval", "Approval", approvalStatusProvider())));

			var result = ScopedValue.where(RequestContextScope.REQUEST_CONTEXT, requestContext)
					.call(() -> collections.collection("blog").query().page(1, 10));
			var approvedItem = ScopedValue.where(RequestContextScope.REQUEST_CONTEXT, requestContext)
					.call(() -> collections.collection("blog").item("approved"));
			var blockedItem = ScopedValue.where(RequestContextScope.REQUEST_CONTEXT, requestContext)
					.call(() -> collections.collection("blog").item("blocked"));

			Assertions.assertThat(result.getTotalItems()).isEqualTo(1);
			Assertions.assertThat(result.getItems())
					.singleElement()
					.satisfies(item -> Assertions.assertThat(item.id()).isEqualTo("approved"));
			Assertions.assertThat(approvedItem).isPresent();
			Assertions.assertThat(blockedItem).isEmpty();
		} finally {
			collections.close();
		}
	}

	private FileSystemCollectionRepository createCollections() throws Exception {
		var collections = new FileSystemCollectionRepository(
				"test-site", tempDirectory, FileSystemCollectionRepositoryStorageTest::parseMeta);
		collections.init();
		return collections;
	}

	private Path write(String relativePath, String meta, String body) throws Exception {
		return writeMeta(relativePath, "status: published\n" + meta, body);
	}

	private Path writeMeta(String relativePath, String meta, String body) throws Exception {
		var file = tempDirectory.resolve("collections").resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, "---%n%s%n---%n%s%n".formatted(meta, body));
		return file;
	}

	private static WFStatusProvider approvalStatusProvider() {
		return new WFStatusQueryProvider() {
			@Override
			public boolean isPublished(ContentNode node) {
				return "approved".equals(node.data().get("approval"));
			}

			@Override
			public Status status(ContentNode node) {
				return new Status(
						isPublished(node),
						null,
						null,
						true,
						(String) node.data().get("approval"));
			}

			@Override
			public String newNodeStatus() {
				return "blocked";
			}

			@Override
			public <T> Optional<ContentQuery<T>> published(ContentQuery<T> query) {
				return Optional.of(query.where("approval", "approved"));
			}

			@Override
			public <T> ContentQuery<T> unpublished(ContentQuery<T> query) {
				return query.where("approval", "!=", "approved");
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parseMeta(Path file) {
		try {
			var content = Files.readString(file);
			var parts = content.split("---", 3);
			return parts.length == 3 ? new Yaml().load(parts[1]) : Map.of();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
