package com.condation.cms.content.template.functions.query;

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
import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.feature.features.ContentNodeMapperFeature;
import com.condation.cms.api.model.ListNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.content.template.functions.AbstractCurrentNodeFunction;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import lombok.Setter;

/**
 *
 * @author t.marx
 */
public class QueryFunction extends AbstractCurrentNodeFunction {

	BiFunction<ContentNode, Integer, ListNode> nodeMapper = null;
	Map<String, BiPredicate<Object, Object>> extendedQueryOperations = new HashMap<>();
	
	@Setter
	private String contentType;

	public QueryFunction(ContentRepository contentRepository,
			ContentNode currentNode, RequestContext context) {
		this(
				contentRepository,
				currentNode,
				context, Map.of());
	}
	
	public QueryFunction(ContentRepository contentRepository,
			ContentNode currentNode, RequestContext context,
			final Map<String, BiPredicate<Object, Object>> queryOperations) {
		super(
				currentNode,
				contentRepository,
				context);
		this.extendedQueryOperations = queryOperations;
	}
	
	private BiFunction<ContentNode, Integer, ListNode> nodeMapper() {
		if (nodeMapper == null) {
			nodeMapper = (node, excerptLength) -> {
				return context.get(ContentNodeMapperFeature.class).contentNodeMapper().toListNode(node, context, excerptLength);
			};
		}

		return nodeMapper;
	}

	public ContentQuery create() {
		
		var query = contentRepository.query(nodeMapper());
		query.customOperators(extendedQueryOperations);
		
		if (!Strings.isNullOrEmpty(contentType)) {
			query.contentType(contentType);
		}
		return query;
	}

	public ContentQuery create(final String startUri) {
		var base = startUri.startsWith("/") ? "" : currentDirectory();
		var query = contentRepository.query(repositoryPath(base, startUri), nodeMapper());
		
		query.customOperators(extendedQueryOperations);
		
		if (!Strings.isNullOrEmpty(contentType)) {
			query.contentType(contentType);
		}
		return query;
	}

	public String toUrl(String uri) {
		if (uri.endsWith("index.md")) {
			uri = uri.replace("index.md", "");
		}
		if (uri.endsWith(".md")) {
			uri = uri.substring(0, uri.lastIndexOf(".md"));
		}

		if (!uri.startsWith("/")) {
			uri = "/" + uri;
		}
		if (uri.length() > 1 && uri.endsWith("/")) {
			uri = uri.substring(0, uri.length() - 1);
		}

		return uri + (isPreview() ? "?preview=" + getPreviewMode() : "");
	}
}
