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

import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.MutableContentRepository;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads and stores the selector configured for a canonical page.
 */
@Slf4j
public class VariantSelectorConfigurationRepository {

	public static final String DEFAULT_SELECTOR_ID = "date-range";
	public static final String CONFIG_FILE_NAME = "variants.yaml";
	private static final String SELECTOR_PROPERTY = "selector";

	private final ContentRepository contentRepository;
	private final MutableContentRepository mutableContentRepository;

	public VariantSelectorConfigurationRepository(ContentRepository contentRepository,
			MutableContentRepository mutableContentRepository) {
		this.contentRepository = contentRepository;
		this.mutableContentRepository = mutableContentRepository;
	}

	public String getSelectorId(ContentNode node) {
		return contentRepository.variantSelectorId(node).orElse(DEFAULT_SELECTOR_ID);
	}

	public void setSelectorId(ContentNode node, String selectorId) throws IOException {
		if (selectorId == null || selectorId.isBlank()) {
			throw new IllegalArgumentException("selectorId must not be blank");
		}
		mutableContentRepository.setVariantSelectorId(node, selectorId);
	}
}
