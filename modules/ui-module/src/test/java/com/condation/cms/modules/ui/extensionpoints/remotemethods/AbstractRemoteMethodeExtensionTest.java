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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.condation.cms.api.feature.features.MutableRepositoryFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.MutableCollectionRepository;
import com.condation.cms.api.repository.MutableContentRepository;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class AbstractRemoteMethodeExtensionTest {

	@Test
	void resolvesLocalRepositoriesFromFeatures() {
		var content = mock(ContentRepository.class);
		var collections = mock(CollectionRepository.class);
		var mutableContent = mock(MutableContentRepository.class);
		var mutableCollections = mock(MutableCollectionRepository.class);
		var context = mock(SiteModuleContext.class);
		when(context.get(RepositoryFeature.class))
				.thenReturn(new RepositoryFeature(content, collections));
		when(context.get(MutableRepositoryFeature.class))
				.thenReturn(new MutableRepositoryFeature(mutableContent, mutableCollections));
		var extension = new TestExtension();
		extension.setContext(context);

		Assertions.assertThat(extension.content()).isSameAs(content);
		Assertions.assertThat(extension.collections()).isSameAs(collections);
		Assertions.assertThat(extension.mutableContent()).isSameAs(mutableContent);
		Assertions.assertThat(extension.mutableCollections()).isSameAs(mutableCollections);
	}

	private static final class TestExtension extends AbstractRemoteMethodeExtension {

		ContentRepository content() {
			return getContentRepository(Map.of());
		}

		CollectionRepository collections() {
			return getCollectionRepository(Map.of());
		}

		MutableContentRepository mutableContent() {
			return getMutableContentRepository(Map.of());
		}

		MutableCollectionRepository mutableCollections() {
			return getMutableCollectionRepository(Map.of());
		}
	}
}
