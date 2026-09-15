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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.MutableContentRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;

class VariantSelectorConfigurationRepositoryTest {

	private final ContentNode canonical = new ContentNode(
			"news/about.md", "/news/about", "about.md", Map.of()
	);
	private VariantSelectorConfigurationRepository repository;
	private ContentRepository contentRepository;
	private MutableContentRepository mutableContentRepository;

	@BeforeEach
	void setUp() {
		contentRepository = mock(ContentRepository.class);
		mutableContentRepository = mock(MutableContentRepository.class);
		repository = new VariantSelectorConfigurationRepository(
				contentRepository, mutableContentRepository);
	}

	@Test
	void missingConfigurationUsesDateRangeDefault() {
		assertThat(repository.getSelectorId(canonical))
				.isEqualTo(VariantSelectorConfigurationRepository.DEFAULT_SELECTOR_ID);
	}

	@Test
	void storesOneConfigurationAtCanonicalVariantFolder() throws Exception {
		when(contentRepository.variantSelectorId(canonical)).thenReturn(Optional.of("audience"));
		repository.setSelectorId(canonical, "audience");

		verify(mutableContentRepository).setVariantSelectorId(canonical, "audience");
		assertThat(repository.getSelectorId(canonical)).isEqualTo("audience");
	}
}
