package com.condation.cms.collections;

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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.configuration.Configuration;
import com.condation.cms.api.configuration.configs.SiteConfiguration;
import com.condation.cms.api.db.collection.Collection;
import com.condation.cms.api.extensions.RegisterTemplateFunctionExtensionPoint;
import com.condation.cms.api.feature.features.CurrentNodeFeature;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.HookSystemFeature;
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.ModuleManagerFeature;
import com.condation.cms.api.model.Parameter;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.core.content.io.ContentFileParser;
import com.condation.cms.core.eventbus.DefaultEventBus;
import com.condation.cms.filesystem.FileDB;
import com.condation.cms.hooksystem.CMSHookSystem;
import com.condation.cms.hooksystem.extensions.TemplateHooks;
import com.condation.cms.modules.system.templates.CollectionTemplateFunctionExtensions;
import com.condation.cms.templates.CMSTemplateEngine;
import com.condation.cms.templates.DynamicConfiguration;
import com.condation.cms.templates.TemplateEngineFactory;
import com.condation.cms.templates.components.TemplateComponents;
import com.condation.cms.templates.loaders.StringTemplateLoader;
import com.condation.cms.test.TestSiteProperties;
import com.condation.modules.api.ModuleManager;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CollectionFunctionsIntegrationTest {

	private static final String COLLECTION_NAME = "products";
	private static final int ITEM_COUNT = 10_000;

	@TempDir
	static Path hostBase;

	private FileDB db;
	private Collection collection;
	private CMSTemplateEngine templateEngine;
	private DynamicConfiguration dynamicConfiguration;

	@BeforeAll
	void setUp() throws Exception {
		createCollectionItems();
		db = createDatabase();
		db.init();
		
		var extension = new CollectionTemplateFunctionExtensions();
		var moduleContext = new SiteModuleContext();
		moduleContext.add(DBFeature.class, new DBFeature(db));
		moduleContext.add(RepositoryFeature.class, new RepositoryFeature(null, db.getCollectionRepository()));
		extension.setContext(moduleContext);

		collection = (Collection) extension.collection(new Parameter(Map.of("value", COLLECTION_NAME)));
		configureTemplateEngine(extension);
	}

	@AfterAll
	void tearDown() throws Exception {
		if (db != null) {
			db.close();
		}
	}

	@Test
	void exposesCollectionThroughTemplateFunction() throws Exception {
		var template = templateEngine.getTemplateFromString(
				"{{ cms.collection('products').name() }}|"
				+ "{{ cms.collection({'name': 'products'}).name() }}");

		Assertions.assertThat(template.evaluate(Map.of(), dynamicConfiguration))
				.isEqualTo("products|products");
	}

	@Test
	void retrievesSingleCollectionItemIncludingMarkdownBody() {
		Assertions.assertThat(collection.name()).isEqualTo(COLLECTION_NAME);
		Assertions.assertThat(collection.item("item-04242"))
				.isPresent()
				.get()
				.satisfies(item -> {
					Assertions.assertThat(item.id()).isEqualTo("item-04242");
					Assertions.assertThat(item.collection()).isEqualTo(COLLECTION_NAME);
					Assertions.assertThat(item.path()).isEqualTo("products/item-04242.md");
					Assertions.assertThat(item.meta())
							.containsEntry("title", "Product 04242")
							.containsEntry("sequence", 4242)
							.containsEntry("category", "even");
					Assertions.assertThat(item.content()).isEqualTo("Product body 04242");
				});
		Assertions.assertThat(collection.item("missing-item")).isEmpty();
	}

	@Test
	void filtersSortsAndPagesAcrossTenThousandItems() {
		var page = collection.query()
				.where("category", "even")
				.orderby("sequence")
				.desc()
				.page(2, 25);

		Assertions.assertThat(page.getTotalItems()).isEqualTo(ITEM_COUNT / 2);
		Assertions.assertThat(page.getTotalPages()).isEqualTo(200);
		Assertions.assertThat(page.getPage()).isEqualTo(2);
		Assertions.assertThat(page.getItems()).hasSize(25);
		Assertions.assertThat(page.getItems())
				.extracting(item -> item.id())
				.startsWith("item-09948", "item-09946")
				.endsWith("item-09902", "item-09900");
	}

	@Test
	void queriesMetadataForTheWholeCollection() {
		var allItems = collection.metadataQuery().page(1, 1);
		var selectedItems = collection.metadataQuery()
				.where("sequence", 4321)
				.get();

		Assertions.assertThat(allItems.getTotalItems()).isEqualTo(ITEM_COUNT);
		Assertions.assertThat(allItems.getTotalPages()).isEqualTo(ITEM_COUNT);
		Assertions.assertThat(selectedItems)
				.singleElement()
				.satisfies(item -> {
					Assertions.assertThat(item.id()).isEqualTo("item-04321");
					Assertions.assertThat(item.meta())
							.containsEntry("title", "Product 04321")
							.containsEntry("category", "odd");
				});
	}

	private void createCollectionItems() throws Exception {
		Files.createDirectories(hostBase.resolve("content"));
		var collectionDirectory = Files.createDirectories(
				hostBase.resolve("collections").resolve(COLLECTION_NAME));
		IntStream.range(0, ITEM_COUNT).parallel().forEach(index -> {
			try {
				var id = "item-%05d".formatted(index);
				var content = """
                                 ---
                                 title: Product %05d
                                 sequence: %d
                                 category: %s
                                 status: published
                                 ---
                                 Product body %05d
                                 """.formatted(index, index, index % 2 == 0 ? "even" : "odd", index);
				Files.writeString(collectionDirectory.resolve(id + ".md"), content);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	private FileDB createDatabase() {
		var configuration = new Configuration();
		var siteConfiguration = mock(SiteConfiguration.class);
		SiteProperties siteProperties = new TestSiteProperties(Map.of(
				"id", "collection-functions-test",
				"query.index.mode", "MEMORY"));
		when(siteConfiguration.siteProperties()).thenReturn(siteProperties);
		configuration.add(SiteConfiguration.class, siteConfiguration);

		return new FileDB(hostBase, new DefaultEventBus(), path -> {
			try {
				return new ContentFileParser(path.toString()).getHeader();
			} catch (Exception exception) {
				throw new IllegalStateException("Could not parse test collection item " + path, exception);
			}
		}, configuration);
	}

	private void configureTemplateEngine(CollectionTemplateFunctionExtensions extension) {
		var requestContext = new RequestContext();
		requestContext.add(HookSystemFeature.class, new HookSystemFeature(new CMSHookSystem()));
		requestContext.add(TemplateHooks.class, new TemplateHooks(requestContext));
		requestContext.add(CurrentNodeFeature.class, new CurrentNodeFeature(
				new ContentNode("index.md", "/", "index.md", Map.of())));

		var injector = mock(Injector.class);
		var moduleManager = mock(ModuleManager.class);
		when(injector.getInstance(ModuleManager.class)).thenReturn(moduleManager);
		when(moduleManager.extensions(RegisterTemplateFunctionExtensionPoint.class))
				.thenReturn(List.of(extension));
		requestContext.add(InjectorFeature.class, new InjectorFeature(injector));
		requestContext.add(ModuleManagerFeature.class, new ModuleManagerFeature(moduleManager));

		dynamicConfiguration = new DynamicConfiguration(new TemplateComponents(), requestContext);
		templateEngine = TemplateEngineFactory
				.newInstance(new StringTemplateLoader(), true)
				.defaultFilters()
				.defaultTags()
				.create();
	}
}
