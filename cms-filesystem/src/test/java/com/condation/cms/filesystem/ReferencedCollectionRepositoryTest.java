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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.condation.cms.api.configuration.configs.CollectionConfiguration;
import com.condation.cms.api.configuration.configs.CollectionDefinition;
import com.condation.cms.api.db.collection.Collection;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.repository.MutableCollectionRepository;
import com.condation.cms.core.serivce.ServiceRegistry;
import com.condation.cms.core.serivce.impl.SiteCollectionRepositoryService;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ReferencedCollectionRepositoryTest {

	@AfterEach
	void clearServices() {
		ServiceRegistry.getInstance().clear();
	}

	@Test
	void resolvesReferencedCollectionsThroughTheSourceSite() {
		var local = mock(MutableCollectionRepository.class);
		when(local.names()).thenReturn(Set.of("local"));
		var sourceCollections = mock(MutableCollectionRepository.class);
		var sourceCollection = mock(Collection.class);
		when(sourceCollections.access("shared")).thenReturn(CollectionAccess.READ_WRITE);
		when(sourceCollections.collection("shared")).thenReturn(sourceCollection);
		ServiceRegistry.getInstance().register(
				"content-site",
				SiteCollectionRepositoryService.class,
				new SiteCollectionRepositoryService(sourceCollections, sourceCollections));
		var definitions = new ConcurrentHashMap<String, CollectionDefinition>();
		definitions.put("shared", new CollectionDefinition("shared", "content-site", null));
		var collections = new ReferencedCollectionRepository(
				"consumer-site",
				local,
				new CollectionConfiguration(definitions));

		Assertions.assertThat(collections.names()).containsExactlyInAnyOrder("local", "shared");
		Assertions.assertThat(collections.access("shared")).isEqualTo(CollectionAccess.READ_ONLY);
		Assertions.assertThat(collections.collection("shared")).isSameAs(sourceCollection);
		Assertions.assertThatThrownBy(() -> collections.save("shared", "item", Map.of(), "body"))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessageContaining("read-only");
	}

	@Test
	void usesReloadedConfigurationWithoutRecreatingTheRepository() throws Exception {
		var local = mock(MutableCollectionRepository.class);
		var localCollection = mock(Collection.class);
		when(local.names()).thenReturn(Set.of("shared"));
		when(local.access("shared")).thenReturn(CollectionAccess.READ_WRITE);
		when(local.collection("shared")).thenReturn(localCollection);
		var definitions = new ConcurrentHashMap<String, CollectionDefinition>();
		definitions.put("shared", new CollectionDefinition("shared", "content-site", null));
		var configuration = new CollectionConfiguration(definitions);
		var collections = new ReferencedCollectionRepository(
				"consumer-site",
				local,
				configuration);

		definitions.put("shared", new CollectionDefinition("shared", null));
		configuration.replaceCollections(definitions);

		Assertions.assertThat(collections.access("shared")).isEqualTo(CollectionAccess.READ_WRITE);
		Assertions.assertThat(collections.collection("shared")).isSameAs(localCollection);
		collections.save("shared", "item", Map.of(), "body");
		verify(local).save("shared", "item", Map.of(), "body");
	}
}
