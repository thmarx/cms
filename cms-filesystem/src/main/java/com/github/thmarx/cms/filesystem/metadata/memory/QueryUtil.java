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
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.utils.MapUtil;
import com.github.thmarx.cms.filesystem.metadata.query.Queries;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public final class QueryUtil {

	

	public static Map<Object, List<ContentNode>> groupby(final Stream<ContentNode> nodes, final String field) {
		return nodes.collect(Collectors.groupingBy((node) -> MapUtil.getValue(node.data(), field)));
	}

	protected static <T extends ContentNode> QueryContext<T> sorted(final QueryContext<T> context, final String field, final boolean asc) {

		var tempNodes = context.getNodes().sorted(
				(node1, node2) -> {
					var value1 = MapUtil.getValue(node1.data(), field);
					var value2 = MapUtil.getValue(node2.data(), field);

					return Queries.compare(value1, value2);
				}
		).toList();

		if (!asc) {
			tempNodes = tempNodes.reversed();
		}

		context.setNodes(tempNodes.stream());

		return context;
	}

	public static QueryContext filtered(final QueryContext context, final String field, final Object value, final Queries.Operator operator) {
		context.setNodes(context.getNodes().filter(createPredicate(field, value, operator)));
		return context;
	}

	private static Predicate<ContentNode> createPredicate(final String field, final Object value, final Queries.Operator operator) {
		return (node) -> {
			var node_value = MapUtil.getValue(node.data(), field);

			if (node_value == null) {
				return false;
			}

			if (Queries.filters.containsKey(operator)) {
				return Queries.filters.get(operator).matches(node_value, value);
			}

			log.error("unknown operation " + operator.name());
			return false;
		};
	}

	protected static QueryContext filter_extension(final QueryContext context, final String field, final Object value, final BiPredicate<Object, Object> predicate) {
		context.setNodes(context.getNodes().filter(Queries.createExtensionPredicate(field, value, predicate)));
		return context;
	}

	
	
}
