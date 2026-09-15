package com.condation.cms.repository;

/*-
 * #%L
 * integration-tests
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

import com.condation.cms.TestDirectoryUtils;
import com.condation.cms.api.Constants;
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.configuration.Configuration;
import com.condation.cms.api.configuration.configs.SiteConfiguration;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.content.DefaultContentParser;
import com.condation.cms.core.eventbus.DefaultEventBus;
import com.condation.cms.filesystem.FileDB;
import com.condation.cms.filesystem.FileSystemContentRepository;
import com.condation.cms.filesystem.FileSystemContentStore;
import com.condation.cms.filesystem.NIOReadOnlyFile;
import java.io.IOException;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileSystemContentRepositoryIntegrationTest {

	private static FileDB db;
	private static ContentRepository repository;

	@BeforeAll
	static void setUp() throws IOException {
		var hostBase = Path.of("target/test-repository-" + System.nanoTime());
		TestDirectoryUtils.copyDirectory(Path.of("hosts/test"), hostBase);

		var contentParser = new DefaultContentParser();
		var configuration = new Configuration();
		var siteConfiguration = mock(SiteConfiguration.class);
		var siteProperties = mock(SiteProperties.class);
		when(siteProperties.id()).thenReturn("repository-test-site");
		when(siteConfiguration.siteProperties()).thenReturn(siteProperties);
		configuration.add(SiteConfiguration.class, siteConfiguration);

		db = new FileDB(hostBase, new DefaultEventBus(), file -> {
			try {
				ReadOnlyFile cmsFile = new NIOReadOnlyFile(
						file, hostBase.resolve(Constants.Folders.CONTENT));
				return contentParser.parseMeta(cmsFile);
			} catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}, configuration);
		db.init();

		repository = new FileSystemContentRepository(
				db.getContent(),
				db.getFileSystem(),
				new FileSystemContentStore(db.getFileSystem()),
				contentParser);
	}

	@AfterAll
	static void tearDown() throws Exception {
		db.close();
	}

	@Test
	void loadsIndexedContentAndDelegatesQueries() throws Exception {
		var node = repository.get("products/test.md").orElseThrow();

		Assertions.assertThat(repository.findByUrl("/products/test"))
				.contains(node);
		Assertions.assertThat(repository.load(node))
				.hasValueSatisfying(document ->
						Assertions.assertThat(document.content())
								.contains("Das ist ein Produkt!"));
		Assertions.assertThat(repository.isVisible(node)).isTrue();
		Assertions.assertThat(repository.isVisible(
				repository.get("alias-hidden.md").orElseThrow())).isFalse();
		Assertions.assertThat(repository.children("products"))
				.extracting(contentNode -> contentNode.path())
				.containsExactlyInAnyOrder("products/index.md", "products/test.md");
		Assertions.assertThat(repository.directories("nodelist"))
				.extracting(contentNode -> contentNode.path())
				.containsExactlyInAnyOrder("nodelist/folder1", "nodelist/folder2");
		Assertions.assertThat(repository.query()
				.where(Constants.MetaFields.TITLE, "ProduktSeite")
				.get())
				.extracting(contentNode -> contentNode.path())
				.contains("products/test.md");
	}

	@Test
	void loadsSectionsWithTheirParsedContent() throws Exception {
		var page = repository.get("page.md").orElseThrow();

		Assertions.assertThat(repository.sections(page, "left"))
				.hasSize(4)
				.allSatisfy(section -> {
					Assertions.assertThat(section.owner()).isEqualTo(page.path());
					Assertions.assertThat(section.name()).isEqualTo("left");
					Assertions.assertThat(section.content()).contains("Und hier der Inhalt");
				});
	}

	@Test
	void resolvesVariantsAndTheirCanonicalContext() throws Exception {
		var canonical = repository.get("test.md").orElseThrow();
		var summer = repository.variant(canonical, "summer").orElseThrow();

		Assertions.assertThat(summer.node().path())
				.isEqualTo(".variants/test/summer/test.md");
		Assertions.assertThat(repository.load(summer.node()))
				.hasValueSatisfying(document ->
						Assertions.assertThat(document.content())
								.contains("Summer variant content"));

		var context = repository.variantContext(summer.node());
		Assertions.assertThat(context.canonical()).isEqualTo(canonical);
		Assertions.assertThat(context.activeVariantId()).contains("summer");
		Assertions.assertThat(context.variants())
				.extracting(variant -> variant.id())
				.containsExactly("summer");
	}
}
