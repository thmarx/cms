package com.condation.cms.modules.system.templates;

/*-
 * #%L
 * CMS System Modules
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

import com.condation.cms.api.annotations.TemplateFunction;
import com.condation.cms.api.extensions.RegisterTemplateFunctionExtensionPoint;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.model.Parameter;
import com.condation.modules.api.annotation.Extension;

/**
 * Makes site collections available as {@code cms.collection('name')}.
 */
@Extension(value = RegisterTemplateFunctionExtensionPoint.class, cached = Extension.Caching.TRUE)
public class CollectionTemplateFunctionExtensions extends RegisterTemplateFunctionExtensionPoint {

	@TemplateFunction(value = "collection", namespace = "cms")
	public Object collection(Parameter params) {
		var value = params.get(params.containsKey("name") ? "name" : "value");
		if (value == null) {
			throw new IllegalArgumentException("collection name must not be null");
		}
		return context.get(RepositoryFeature.class)
				.collectionRepository()
				.collection(value.toString());
	}
}
