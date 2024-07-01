package com.github.thmarx.cms.filesystem.metadata.query;

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

import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.utils.MapUtil;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 *
 * @author t.marx
 */
public class Queries {
	public static enum Operator {
		CONTAINS,
		CONTAINS_NOT,
		IN,
		NOT_IN,
		EQ,
		NOT_EQ,
		GT,
		GTE,
		LT,
		LTE;
	}

	public final static Map<Operator, Filter> filters = new HashMap<>();

	static {
		filters.put(Operator.EQ, (node_value, value) -> Objects.equals(node_value, value));
		filters.put(Operator.NOT_EQ, (node_value, value) -> !Objects.equals(node_value, value));
		filters.put(Operator.CONTAINS, (node_value, value) -> ((List) node_value).contains(value));
		filters.put(Operator.CONTAINS_NOT, (node_value, value) -> !((List) node_value).contains(value));
		filters.put(Operator.GT, (node_value, value) -> compare(node_value, value) > 0);
		filters.put(Operator.GTE, (node_value, value) -> compare(node_value, value) >= 0);
		filters.put(Operator.LT, (node_value, value) -> compare(node_value, value) < 0);
		filters.put(Operator.LTE, (node_value, value) -> compare(node_value, value) <= 0);
		filters.put(Operator.IN, (node_value, value) -> {
			List<?> values = Collections.emptyList();
			if (value instanceof List) {
				values = (List<?>) value;
			} else if (value.getClass().isArray()) {
				values = Arrays.asList((Object[]) value);
			}
			return values.contains(node_value);
		});
		filters.put(Operator.NOT_IN, (node_value, value) -> {
			List<?> values = Collections.emptyList();
			if (value instanceof List) {
				values = (List<?>) value;
			} else if (value.getClass().isArray()) {
				values = Arrays.asList((Object[]) value);
			}
			return !values.contains(node_value);
		});
	}

	private static final List<String> operations = List.of(
			"=" , "eq", 
			"!=", "not eq", 
			">", "gt",
			">=", "gte",
			"<", "lt",
			"<=", "lte",
			"in", "not in", 
			"contains", "not contains"
			);
	public static boolean isDefaultOperation (final String operation) {
		return operations.contains(operation);
	}
	
	public static Operator operator4String(final String operator) {
		if (Strings.isNullOrEmpty(operator)) {
			return Operator.EQ;
		}
		return switch (operator) {
			case "=" ->
				Operator.EQ;
			case "eq" ->
				Operator.EQ;
			case "!=" ->
				Operator.NOT_EQ;
			case "not eq" ->
				Operator.NOT_EQ;
			case ">" ->
				Operator.GT;
			case "gt" ->
				Operator.GT;
			case ">=" ->
				Operator.GTE;
			case "gte" ->
				Operator.GTE;
			case "<" ->
				Operator.LT;
			case "lt" ->
				Operator.LT;
			case "<=" ->
				Operator.LTE;
			case "lte" ->
				Operator.LTE;
			case "in" ->
				Operator.IN;
			case "not in" ->
				Operator.NOT_IN;
			case "contains" ->
				Operator.CONTAINS;
			case "not contains" ->
				Operator.CONTAINS_NOT;
			default ->
				throw new IllegalArgumentException("unknown operator " + operator);
		};
	}
	
	public static int compare(Object o1, Object o2) {
		if (Objects.equals(o1, o2)) {
			return 0;
		}
		if (o1 == null) {
			return -1;
		}
		if (o2 == null) {
			return 1;
		}

		if (!o1.getClass().equals(o2.getClass())) {
			return 0;
		}

		if (o1 instanceof Float) {
			return Float.compare((float) o1, (float) o2);
		} else if (o1 instanceof Double) {
			return Double.compare((double) o1, (double) o2);
		} else if (o1 instanceof Short) {
			return Short.compare((short) o1, (short) o2);
		} else if (o1 instanceof Integer) {
			return Integer.compare((int) o1, (int) o2);
		} else if (o1 instanceof Long) {
			return Long.compare((long) o1, (long) o2);
		} else if (o1 instanceof String string) {
			return string.compareTo((String) o2);
		} else if (o1 instanceof Date date) {
			return date.compareTo((Date) o2);
		}

		return 0;
	}
	
	public static Predicate<ContentNode> createExtensionPredicate(final String field, final Object value, final BiPredicate<Object, Object> predicate) {
		return (node) -> {
			var node_value = MapUtil.getValue(node.data(), field);

			if (node_value == null) {
				return false;
			}
			
			return predicate.test(node_value, value);
		};
	}
}
