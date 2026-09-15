package com.condation.cms.templates.functions.impl;

/*-
 * #%L
 * CMS Templates
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
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.content.ContentRenderer;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author thorstenmarx
 */
@Slf4j
public class NodeFunction extends AbstractNodeFunction {

	public static final String FUNCTION_NAME = "select_node";

	public NodeFunction(RequestContext requestContext) {
		super(requestContext);
	}
	
	@Override
	protected void extendMap(Map<String, Object> node, ContentNode contentNode) {
		try {
			var contentRepository = requestContext.get(RepositoryFeature.class).contentRepository();
			var contentRenderer = requestContext.get(InjectorFeature.class).injector().getInstance(ContentRenderer.class);
			var sectionEntries = contentRenderer.renderSections(
					contentRepository.sections(contentNode), requestContext);
			node.put("sections", sectionEntries);
		} catch (IOException iOException) {
			log.error("error loading sections", iOException);
		}
	}

	@Override
	public String name() {
		return FUNCTION_NAME;
	}
	
}
