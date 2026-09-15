package com.condation.cms.modules.ui.extensionpoints;

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

import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.IsPreviewFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.request.RequestContext;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class UiTemplateModelExtensionTest {

	@Test
	void doesNotRenderCollectionToolbarForReferencedCollection() {
		var collections = mock(CollectionRepository.class);
		when(collections.access("authors")).thenReturn(CollectionAccess.READ_ONLY);
		var siteContext = mock(SiteModuleContext.class);
		when(siteContext.get(RepositoryFeature.class)).thenReturn(new RepositoryFeature(
				mock(com.condation.cms.api.repository.ContentRepository.class), collections));
		var requestContext = new RequestContext();
		requestContext.add(IsPreviewFeature.class, new IsPreviewFeature());
		var helper = new UiTemplateModelExtension.UIHelper(requestContext, siteContext);
		var item = new CollectionItem(
				"first",
				"authors",
				"authors/first.md",
				"content",
				Map.of());

		var toolbar = helper.collectionToolbar(item, new String[]{"edit"});

		Assertions.assertThat(toolbar).isEmpty();
	}
}
