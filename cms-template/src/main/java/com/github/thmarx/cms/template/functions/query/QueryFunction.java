package com.github.thmarx.cms.template.functions.query;

/*-
 * #%L
 * cms-server
 * %%
 * Copyright (C) 2023 Marx-Software
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.ContentQuery;
import com.github.thmarx.cms.api.db.DB;
import com.github.thmarx.cms.api.feature.features.ContentNodeMapperFeature;
import com.github.thmarx.cms.api.feature.features.ContentParserFeature;
import com.github.thmarx.cms.api.feature.features.MarkdownRendererFeature;
import com.github.thmarx.cms.template.functions.AbstractCurrentNodeFunction;
import com.github.thmarx.cms.api.model.ListNode;
import com.github.thmarx.cms.api.request.RequestContext;
import com.github.thmarx.cms.filesystem.metadata.memory.MemoryQuery;
import com.github.thmarx.cms.filesystem.metadata.query.ExtendableQuery;
import com.google.common.base.Strings;
import java.nio.file.Path;
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

	public QueryFunction(DB db, Path currentNode, RequestContext context) {
		this(
				db, 
				currentNode,
				context, Map.of());
	}
	
	public QueryFunction(DB db, Path currentNode, RequestContext context, final Map<String, BiPredicate<Object, Object>> queryOperations) {
		super(
				db, 
				currentNode, 
				context.get(ContentParserFeature.class).contentParser(), 
				context.get(MarkdownRendererFeature.class).markdownRenderer(), 
				context.get(ContentNodeMapperFeature.class).contentNodeMapper(),
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
		
		var query = db.getContent().query(nodeMapper());
		((ExtendableQuery)query).addAllCustomOperators(extendedQueryOperations);
		
		if (!Strings.isNullOrEmpty(contentType)) {
			query.contentType(contentType);
		}
		return query;
	}

	public ContentQuery create(final String startUri) {
		var query = db.getContent().query(startUri, nodeMapper());
		
		((ExtendableQuery)query).addAllCustomOperators(extendedQueryOperations);
		
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

		return uri + (isPreview() ? "?preview" : "");
	}
}
