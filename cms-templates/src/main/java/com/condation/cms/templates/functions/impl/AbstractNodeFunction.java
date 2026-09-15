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

import com.condation.cms.api.content.MapAccess;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.templates.functions.TemplateFunction;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author thmar
 */
@RequiredArgsConstructor
public abstract class AbstractNodeFunction implements TemplateFunction {
	protected final RequestContext requestContext;

	protected void extendMap (Map<String, Object> node, ContentNode contentNode) {
		
	}
	
	@Override
	public Object invoke(Object... params) {
		
		if (params == null || params.length == 0 ) {
			return null;
		}
		if (!(params[0] instanceof String)) {
			return null;
		}
		var repository = requestContext.get(RepositoryFeature.class).contentRepository();
		var contentNode = repository.findByUrl((String) params[0])
				.or(() -> repository.get(normalize((String) params[0])));
		if (contentNode.isPresent()) {
			var node = new HashMap<String, Object>();
			node.put("meta", new MapAccess(contentNode.get().data()));
			node.put("uri", contentNode.get().url());
			extendMap(node, contentNode.get());
			return node;
		}
		
		return null;
	}

	private static String normalize(String value) {
		var result = value.replace('\\', '/');
		while (result.startsWith("/")) {
			result = result.substring(1);
		}
		return result;
	}
}
