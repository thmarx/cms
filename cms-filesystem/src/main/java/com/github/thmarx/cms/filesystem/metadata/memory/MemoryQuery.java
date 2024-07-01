package com.github.thmarx.cms.filesystem.metadata.memory;

/*-
 * #%L
 * cms-filesystem
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
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.db.ContentQuery;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.Page;
import static com.github.thmarx.cms.filesystem.metadata.memory.QueryUtil.filtered;
import static com.github.thmarx.cms.filesystem.metadata.memory.QueryUtil.sorted;
import com.github.thmarx.cms.api.utils.NodeUtil;
import com.github.thmarx.cms.filesystem.metadata.AbstractMetaData;
import com.github.thmarx.cms.filesystem.metadata.query.ExcerptMapperFunction;
import com.github.thmarx.cms.filesystem.metadata.query.Queries;
import com.github.thmarx.cms.filesystem.metadata.query.ExtendableQuery;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 *
 * @author t.marx
 * @param <T>
 */
public class MemoryQuery<T> extends ExtendableQuery<T> {

	private QueryContext<T> context;

	public MemoryQuery(Collection<ContentNode> nodes, BiFunction<ContentNode, Integer, T> nodeMapper) {
		this(nodes.stream(), new ExcerptMapperFunction<>(nodeMapper));
	}

	public MemoryQuery(Stream<ContentNode> nodes, ExcerptMapperFunction<T> nodeMapper) {
		this(new QueryContext<>(nodes, nodeMapper, Constants.DEFAULT_CONTENT_TYPE));
	}

	public MemoryQuery(QueryContext<T> context) {
		this.context = context;
	}
	
	@Override
	public MemoryQuery<T> excerpt(final long excerptLength) {
		context.getNodeMapper().setExcerpt((int)excerptLength);
		return this;
	}

	@Override
	public MemoryQuery<T> where(final String field, final Object value) {
		return where(field, Queries.Operator.EQ, value);
	}

	@Override
	public MemoryQuery<T> where(final String field, final String operator, final Object value) {
		if (Queries.isDefaultOperation(operator)) {
			return where(field, Queries.operator4String(operator), value);
		} else if (getContext().getQueryOperations().containsKey(operator)) {
			return new MemoryQuery<>(QueryUtil.filter_extension(context, field, value, getContext().getQueryOperations().get(operator)));
		}
		throw new IllegalArgumentException("unknown operator " + operator);
	}

	@Override
	public MemoryQuery<T> whereContains(final String field, final Object value) {
		return where(field, Queries.Operator.CONTAINS, value);
	}

	@Override
	public MemoryQuery<T> whereNotContains(final String field, final Object value) {
		return where(field, Queries.Operator.CONTAINS_NOT, value);
	}

	@Override
	public MemoryQuery<T> whereIn(final String field, final Object... value) {
		return where(field, Queries.Operator.IN, value);
	}

	@Override
	public MemoryQuery<T> whereNotIn(final String field, final Object... value) {
		return where(field, Queries.Operator.NOT_IN, value);
	}

	@Override
	public MemoryQuery<T> whereIn(final String field, final List<Object> value) {
		return where(field, Queries.Operator.IN, value);
	}

	@Override
	public MemoryQuery<T> whereNotIn(final String field, final List<Object> value) {
		return where(field, Queries.Operator.NOT_IN, value);
	}

	private MemoryQuery<T> where(final String field, final Queries.Operator operator, final Object value) {
		return new MemoryQuery<>(filtered(context, field, value, operator));
	}


	@Override
	public MemoryQuery<T> html() {
		context.setContentType(Constants.ContentTypes.HTML);
		return new MemoryQuery<>(context);
	}

	@Override
	public MemoryQuery<T> json() {
		context.setContentType(Constants.ContentTypes.JSON);
		return new MemoryQuery<>(context);
	}

	@Override
	public MemoryQuery<T> contentType(String contentType) {
		context.setContentType(contentType);
		return new MemoryQuery<>(context);
	}

	@Override
	public List<T> get() {
		return context.getNodes()
				.filter(NodeUtil.contentTypeFiler(context.getContentType()))
				.filter(node -> !node.isDirectory())
				.filter(AbstractMetaData::isVisible)
				.map(context.getNodeMapper())
				.toList();
	}

	public Page<T> page(final Object page, final Object size) {
		int i_page = Constants.DEFAULT_PAGE;
		int i_size = Constants.DEFAULT_PAGE_SIZE;
		if (page instanceof Integer || page instanceof Long) {
			i_page = ((Number) page).intValue();
		} else if (page instanceof String string) {
			i_page = Integer.parseInt(string);
		}
		if (size instanceof Integer || size instanceof Long) {
			i_size = ((Number) size).intValue();
		} else if (size instanceof String string) {
			i_size = Integer.parseInt(string);
		}
		return page((int) i_page, (int) i_size);
	}

	@Override
	public Page<T> page(final long page, final long pageSize) {
		long offset = (page - 1) * pageSize;

		var filteredNodes = context.getNodes()
				.filter(NodeUtil.contentTypeFiler(context.getContentType()))
				.filter(node -> !node.isDirectory())
				.filter(AbstractMetaData::isVisible)
				.toList();

		var totalItems = filteredNodes.size();

		var filteredTargetNodes = filteredNodes.stream()
				.skip(offset)
				.limit(pageSize)
				.map(context.getNodeMapper())
				.toList();

		int totalPages = (int) Math.ceil((float) totalItems / pageSize);
		return new Page<>(totalItems, pageSize, totalPages, (int)page, filteredTargetNodes);
	}

	@Override
	public Sort<T> orderby(final String field) {
		return new Sort<>(field, context);
	}

	@Override
	public Map<Object, List<ContentNode>> groupby(final String field) {
		return QueryUtil.groupby(context.getNodes(), field);
	}

	public static record Sort<T>(String field, QueryContext context) implements ContentQuery.Sort<T> {

		public MemoryQuery<T> asc() {
			return new MemoryQuery<T>(sorted(context, field, true));
		}

		public MemoryQuery<T> desc() {
			return new MemoryQuery<T>(sorted(context, field, false));
		}
	}
}
