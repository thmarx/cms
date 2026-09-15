package com.condation.cms.content;

/*-
 * #%L
 * CMS Content
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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.condation.cms.api.configuration.Configuration;
import com.condation.cms.api.configuration.configs.CollectionConfiguration;
import com.condation.cms.api.configuration.configs.CollectionDefinition;
import com.condation.cms.api.configuration.configs.CollectionDetailConfiguration;
import com.condation.cms.api.content.DefaultContentResponse;
import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.db.collection.Collection;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.db.collection.CollectionItemMetadata;
import com.condation.cms.api.feature.features.CurrentCollectionItemFeature;
import com.condation.cms.api.feature.features.CurrentNodeFeature;
import com.condation.cms.api.feature.features.RequestFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.CollectionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CollectionResolverTest {

	private final ContentRenderer renderer = mock(ContentRenderer.class);
	private final CollectionRepository collections = mock(CollectionRepository.class);
	private final Collection collection = mock(Collection.class);
	private final ConcurrentHashMap<String, CollectionDefinition> definitions = new ConcurrentHashMap<>();
	private final Configuration configuration = new Configuration();
	private CollectionConfiguration collectionConfiguration;
	private final CollectionItem item = new CollectionItem(
			"first",
			"blog",
			"blog/first.md",
			"# First",
			Map.of("title", "First", "slug", "first-post"));

	@BeforeEach
	void setUp() throws Exception {
		collectionConfiguration = new CollectionConfiguration(definitions);
		configuration.add(CollectionConfiguration.class, collectionConfiguration);
		when(collections.collection("blog")).thenReturn(collection);
		when(renderer.renderCollection(
				any(),
				eq(item),
				anyString(),
				any())).thenReturn("<h1>First</h1>");
	}

	@Test
	void resolvesAnIdRouteAndUsesReloadedDefinitions() throws Exception {
		define(definition("/old/{id}"));
		when(collection.item("first")).thenReturn(Optional.of(item));
		var resolver = new CollectionResolver(renderer, collections, configuration);
		var context = context("/blog/first");

		Assertions.assertThat(resolver.getContent(context)).isEmpty();

		define(definition("/blog/{id}"));
		var response = resolver.getContent(context);

		Assertions.assertThat(response)
				.isPresent()
				.get()
				.isInstanceOfSatisfying(DefaultContentResponse.class, content ->
						Assertions.assertThat(content.content()).isEqualTo("<h1>First</h1>"));
		Assertions.assertThat(context.get(CurrentNodeFeature.class).node().url()).isEqualTo("/blog/first");
		Assertions.assertThat(context.get(CurrentCollectionItemFeature.class).item()).isEqualTo(item);
		var node = ArgumentCaptor.forClass(com.condation.cms.api.db.ContentNode.class);
		verify(renderer).renderCollection(
				node.capture(),
				eq(item),
				eq("collections/detail.html"),
				eq(context));
		Assertions.assertThat(node.getValue().data()).containsEntry("template", "collections/detail.html");
	}

	@Test
	void resolvesAConfiguredFrontMatterField() throws Exception {
		define(definition("/blog/{slug}"));
		@SuppressWarnings("unchecked")
		var query = (ContentQuery<CollectionItemMetadata>) mock(ContentQuery.class);
		when(collection.metadataQuery()).thenReturn(query);
		when(query.where("slug", "first-post")).thenReturn(query);
		when(query.page(1, 2)).thenReturn(new Page<>(1, 2, 1, 1, List.of(
				new CollectionItemMetadata("first", "blog", "blog/first.md", item.meta()))));
		when(collection.item("first")).thenReturn(Optional.of(item));
		var resolver = new CollectionResolver(renderer, collections, configuration);

		var response = resolver.getContent(context("/blog/first-post/"));

		Assertions.assertThat(response).isPresent();
		verify(query).where("slug", "first-post");
	}

	@Test
	void doesNotScanTheCollectionForLegacyUnnormalizedRouteValues() throws Exception {
		define(definition("/blog/{slug}"));
		@SuppressWarnings("unchecked")
		var exactQuery = (ContentQuery<CollectionItemMetadata>) mock(ContentQuery.class);
		when(collection.metadataQuery()).thenReturn(exactQuery);
		when(exactQuery.where("slug", "ueber-uns")).thenReturn(exactQuery);
		when(exactQuery.page(1, 2)).thenReturn(new Page<>(0, 2, 0, 1, List.of()));

		var response = new CollectionResolver(renderer, collections, configuration)
				.getContent(context("/blog/ueber-uns"));

		Assertions.assertThat(response).isEmpty();
		verify(collection).metadataQuery();
	}

	@Test
	void resolvesAComplexRouteAgainstIndexedMetadata() {
		var detail = new CollectionDetailConfiguration(
				"/events/{date:yyyy}/{date:MM}/{date:dd}/{location.country}/{location.city}",
				"collections/detail.html",
				Map.of("location.country", Map.of("de", "germany")));
		define(new CollectionDefinition("blog", detail));
		var metadata = Map.<String, Object>of(
				"date", LocalDate.of(2026, 9, 3),
				"location", Map.of("country", "de", "city", "München"));
		var event = new CollectionItem(
				"first", "blog", "blog/first.md", "# Event", metadata);
		@SuppressWarnings("unchecked")
		var query = (ContentQuery<CollectionItemMetadata>) mock(ContentQuery.class);
		when(collection.metadataQuery()).thenReturn(query);
		when(query.get()).thenReturn(List.of(
				new CollectionItemMetadata("first", "blog", "blog/first.md", metadata)));
		when(collection.item("first")).thenReturn(Optional.of(event));

		var route = new CollectionRouteResolver(collections, collectionConfiguration)
				.resolve("/events/2026/09/03/germany/muenchen/");

		Assertions.assertThat(route).isPresent();
		Assertions.assertThat(route.orElseThrow().item()).isEqualTo(event);
	}

	@Test
	void rendersAnItemFromTheConfiguredSourceSiteWithoutExposingItsFile() throws Exception {
		define(
				new CollectionDefinition(
						"blog",
						"content-site",
						new CollectionDetailConfiguration(
								"/shared/{id}",
								"collections/detail.html")));
		when(collection.item("first")).thenReturn(Optional.of(item));
		when(renderer.renderCollection(
				any(),
				eq(item),
				anyString(),
				any())).thenReturn("<h1>Shared</h1>");
		var response = new CollectionResolver(renderer, collections, configuration)
				.getContent(context("/shared/first"));

		Assertions.assertThat(response).isPresent();
		verify(renderer).renderCollection(
				any(),
				eq(item),
				eq("collections/detail.html"),
				any());
	}

	private static CollectionDefinition definition(String route) {
		return new CollectionDefinition(
				"blog",
				new CollectionDetailConfiguration(route, "collections/detail.html"));
	}

	private void define(CollectionDefinition definition) {
		definitions.put("blog", definition);
		collectionConfiguration.replaceCollections(definitions);
	}

	private static RequestContext context(String uri) {
		var context = new RequestContext();
		context.add(RequestFeature.class, new RequestFeature(uri, Map.of()));
		return context;
	}
}
