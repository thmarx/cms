package com.condation.cms.content.views.model;

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
import com.condation.cms.api.db.Page;
import com.condation.cms.api.model.ListNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.content.views.NodeResolver;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.graalvm.polyglot.Context;

/**
 *
 * @author t.marx
 */
@Data
public class View {
	
	private Map<String, Object> meta;
	
	private String template;
	private Content content;
	
	public Page<ListNode> getNodes (final ContentRepository contentRepository,
			final ContentNode currentNode, final Context context, final Map<String, List<String>> queryParams,
			final RequestContext requestContext) {
		
		if (content.getNodelist() != null) {
			return new NodeResolver(contentRepository, currentNode, context, queryParams)
					.nodelist(this, requestContext);
		} else if (content.getQuery() != null) {
			return new NodeResolver(contentRepository, currentNode, context, queryParams)
					.query(this, requestContext);
		}
		return Page.EMPTY;
	}
	
	
}
