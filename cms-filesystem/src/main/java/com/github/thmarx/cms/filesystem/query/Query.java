package com.github.thmarx.cms.filesystem.query;

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

import com.github.thmarx.cms.api.content.Page;
import com.github.thmarx.cms.filesystem.MetaData;
import static com.github.thmarx.cms.filesystem.query.QueryUtil.filtered;
import static com.github.thmarx.cms.filesystem.query.QueryUtil.sorted;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 *
 * @author t.marx
 */
public class Query<T> {

	private final Collection<MetaData.MetaNode> nodes;

	private ExcerptMapperFunction<T> nodeMapper;
	
	public Query(Collection<MetaData.MetaNode> nodes, BiFunction<MetaData.MetaNode, Integer, T> nodeMapper) {
		this(nodes, new ExcerptMapperFunction<>(nodeMapper));
	}
	public Query(Collection<MetaData.MetaNode> nodes, ExcerptMapperFunction<T> nodeMapper) {
		this.nodes = nodes;
		this.nodeMapper = nodeMapper;
	}
	
	public Query<T> excerpt (final int excerptLength) {
		nodeMapper.setExcerpt(excerptLength);
		return this;
	}
	
	public Query<T> where (final String field, final Object value) {
		return where(field, QueryUtil.Operator.EQ, value);
	}

	public Query<T> where (final String field, final String operator, final Object value) {
		return where(field, QueryUtil.operator4String(operator), value);
	}
	
	public Query<T> whereContains (final String field, final Object value) {
		return where(field, QueryUtil.Operator.CONTAINS, value);
	}
	
	public Query<T> whereNotContains (final String field, final Object value) {
		return where(field, QueryUtil.Operator.CONTAINS_NOT, value);
	}
	
	public Query<T> whereIn (final String field, final Object... value) {
		return where(field, QueryUtil.Operator.IN, value);
	}
	
	public Query<T> whereNotIn (final String field, final Object... value) {
		return where(field, QueryUtil.Operator.NOT_IN, value);
	}
	
	public Query<T> whereIn (final String field, final List<Object> value) {
		return where(field, QueryUtil.Operator.IN, value);
	}
	
	public Query<T> whereNotIn (final String field, final List<Object> value) {
		return where(field, QueryUtil.Operator.NOT_IN, value);
	}
	
	private Query<T> where (final String field, final QueryUtil.Operator operator, final Object value) {
		return new Query<>(filtered(nodes, field, value, operator), nodeMapper);
	}
	
	public int count() {
		return nodes.size();
	}
	
	public Page<T> page (final long page, final long size) {
		return page((int)page, (int)size);
	}
	
	public Page<T> page (final int page, final int size) {
		int total = count();
		int offset = (page - 1) * size;
		
		var filteredNodes = nodes.stream()
				.filter(node -> !node.isDirectory())
				.filter(MetaData::isVisible)
				.skip(offset)
				.limit(size)
				.map(nodeMapper)
				.toList();
		
		int totalPages = (int) Math.ceil((float) total / size);
		return new Page<T>(filteredNodes.size(), totalPages, page, filteredNodes);
	}
	
	public List<T> get(final long offset, final long size) {
		return get((int)offset, (int)size);
	}
	
	public List<T> get(final int offset, final int size) {
		var filteredNodes = nodes.stream()
				.filter(node -> !node.isDirectory())
				.filter(MetaData::isVisible)
				.skip(offset)
				.limit(size)
				.toList();
		return Collections.unmodifiableList(filteredNodes.stream().map(nodeMapper).toList());
	}
	
	public List<T> get() {
		var filteredNodes = nodes.stream()
				.filter(node -> !node.isDirectory())
				.filter(MetaData::isVisible)
				.toList();
		return Collections.unmodifiableList(filteredNodes.stream().map(nodeMapper).toList());
	}
	
	public Sort<T> orderby (final String field) {
		return new Sort<T>(field, nodes, nodeMapper);
	}
	
	public Map<Object, List<MetaData.MetaNode>> groupby (final String field) {
		return QueryUtil.groupby(nodes, field);
	}

	public static record Sort<T>(String field, Collection<MetaData.MetaNode> nodes, ExcerptMapperFunction<T> nodeMapper) {

		public Query<T> asc() {
			return new Query<>(sorted(nodes, field, true), nodeMapper);
		}

		public Query<T> desc() {
			return new Query<>(sorted(nodes, field, false), nodeMapper);
		}
	}
}
