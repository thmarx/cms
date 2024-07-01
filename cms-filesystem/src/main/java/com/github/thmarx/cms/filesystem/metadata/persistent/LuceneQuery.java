package com.github.thmarx.cms.filesystem.metadata.persistent;

/*-
 * #%L
 * cms-filesystem
 * %%
 * Copyright (C) 2023 - 2024 Marx-Software
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
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.ContentQuery;
import com.github.thmarx.cms.api.db.Page;
import com.github.thmarx.cms.filesystem.MetaData;
import com.github.thmarx.cms.filesystem.metadata.AbstractMetaData;
import com.github.thmarx.cms.filesystem.metadata.memory.QueryUtil;
import com.github.thmarx.cms.filesystem.metadata.query.ExcerptMapperFunction;
import com.github.thmarx.cms.filesystem.metadata.query.Queries;
import com.github.thmarx.cms.filesystem.metadata.query.ExtendableQuery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;

/**
 *
 * @author t.marx
 * @param <T>
 */
@Slf4j
@RequiredArgsConstructor
public class LuceneQuery<T> extends ExtendableQuery<T> implements ContentQuery.Sort<T> {

	private final LuceneIndex index;
	private final MetaData metaData;
	private final ExcerptMapperFunction<T> nodeMapper;

	private String contentType = Constants.DEFAULT_CONTENT_TYPE;

	private final BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

	enum Order {
		ASC, DESC;
	}

	private Order sortOrder = Order.ASC;
	private Optional<String> orderByField = Optional.empty();

	private Optional<String> startUri = Optional.empty();

	private List<Predicate<ContentNode>> extensionOperations = new ArrayList<>();

	public LuceneQuery(
			final String startUri,
			final LuceneIndex index,
			final MetaData metaData,
			final ExcerptMapperFunction<T> nodeMapper) {
		this(index, metaData, nodeMapper);
		this.startUri = Optional.ofNullable(startUri);
	}

	@Override
	public ContentQuery<T> excerpt(long excerptLength) {
		nodeMapper.setExcerpt((int) excerptLength);
		return this;
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
	public Page<T> page(long page, long size) {

		long offset = (page - 1) * size;

		var result = queryNodes();

		var filteredTargetNodes = result.nodes.stream()
				.skip(offset)
				.limit(size)
				.toList();

		int totalPages = (int) Math.ceil((float) result.total / size);
		return new Page<>(result.total, size, totalPages, (int) page, filteredTargetNodes);
	}

	@Override
	public List<T> get() {
		return queryNodes().nodes;
	}

	private List<ContentNode> queryContentNodes() {
		queryBuilder.add(new TermQuery(new Term("content.type", contentType)), BooleanClause.Occur.MUST);
		if (startUri.isPresent()) {
			queryBuilder.add(new PrefixQuery(new Term("_uri", startUri.get())), BooleanClause.Occur.FILTER);
		}

		try {
			List<Document> result = index.query(queryBuilder.build());

			var contentNodes = result.stream()
					.map(document -> document.get("_uri"))
					.map(metaData::byUri)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.filter(node -> !node.isDirectory())
					.filter(AbstractMetaData::isVisible)
					.toList();

			if (!extensionOperations.isEmpty()) {
				contentNodes = contentNodes.stream()
						.filter((node) -> {
							return extensionOperations.stream()
									.map(predicate -> predicate.test(node))
									.filter(value -> !value)
									.count() == 0;
						})
						.toList();
			}
			return contentNodes;
		} catch (IOException ex) {
			log.error("", ex);
		}
		return Collections.emptyList();
	}

	private NodeResult<T> queryNodes() {
		var contentNodes = queryContentNodes();

		var mappedContentNodes = contentNodes.stream()
				.map(nodeMapper)
				.toList();

		var total = contentNodes.size();

		return new NodeResult<>(total, mappedContentNodes);
	}

	@Override
	public Map<Object, List<ContentNode>> groupby(String field) {
		var nodes = queryContentNodes();
		return QueryUtil.groupby(nodes.stream(), field);
	}

	@Override
	public Sort<T> orderby(String field) {
		this.orderByField = Optional.ofNullable(field);
		return this;
	}

	@Override
	public ContentQuery<T> json() {
		this.contentType = Constants.ContentTypes.JSON;
		return this;
	}

	@Override
	public ContentQuery<T> html() {
		this.contentType = Constants.ContentTypes.HTML;
		return this;
	}

	@Override
	public ContentQuery<T> contentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	@Override
	public ContentQuery<T> where(String field, Object value) {
		return where(field, Queries.Operator.EQ, value);
	}

	@Override
	public ContentQuery<T> where(String field, String operator, Object value) {
		if (Queries.isDefaultOperation(operator)) {
			return where(field, Queries.operator4String(operator), value);
		} else if (getContext().getQueryOperations().containsKey(operator)) {
			extensionOperations.add(
					(Predicate<ContentNode>) Queries.createExtensionPredicate(
							field,
							value,
							getContext().getQueryOperations().get(operator)
					));
			return this;
		}
		throw new IllegalArgumentException("unknown operator " + operator);
	}

	@Override
	public ContentQuery<T> whereContains(String field, Object value) {
		return where(field, Queries.Operator.CONTAINS, value);
	}

	@Override
	public ContentQuery<T> whereNotContains(String field, Object value) {
		return where(field, Queries.Operator.CONTAINS_NOT, value);
	}

	@Override
	public ContentQuery<T> whereIn(String field, Object... value) {
		return where(field, Queries.Operator.IN, value);
	}

	@Override
	public ContentQuery<T> whereIn(String field, List<Object> value) {
		return where(field, Queries.Operator.IN, value);
	}

	@Override
	public ContentQuery<T> whereNotIn(String field, Object... value) {
		return where(field, Queries.Operator.NOT_IN, value);
	}

	@Override
	public ContentQuery<T> whereNotIn(String field, List<Object> value) {
		return where(field, Queries.Operator.NOT_IN, value);
	}

	private ContentQuery<T> where(final String field, final Queries.Operator operator, final Object value) {

		QueryHelper.exists(queryBuilder, field, value);

		switch (operator) {
			case EQ ->
				QueryHelper.eq(queryBuilder, field, value, BooleanClause.Occur.MUST);
			case NOT_EQ ->
				QueryHelper.eq(queryBuilder, field, value, BooleanClause.Occur.MUST_NOT);
			case CONTAINS ->
				QueryHelper.contains(queryBuilder, field, value, BooleanClause.Occur.MUST);
			case CONTAINS_NOT ->
				QueryHelper.contains(queryBuilder, field, value, BooleanClause.Occur.MUST_NOT);
			case IN ->
				QueryHelper.in(queryBuilder, field, value, BooleanClause.Occur.MUST);
			case NOT_IN ->
				QueryHelper.in(queryBuilder, field, value, BooleanClause.Occur.MUST_NOT);
			case LT ->
				QueryHelper.lt(queryBuilder, field, value);
			case LTE ->
				QueryHelper.lte(queryBuilder, field, value);
			case GT ->
				QueryHelper.gt(queryBuilder, field, value);
			case GTE ->
				QueryHelper.gte(queryBuilder, field, value);
		}

		return this;
	}

	@Override
	public ContentQuery<T> asc() {
		this.sortOrder = Order.ASC;
		return this;
	}

	@Override
	public ContentQuery<T> desc() {
		this.sortOrder = Order.DESC;
		return this;
	}

	private record NodeResult<T>(int total, List<T> nodes) {

	}
}
