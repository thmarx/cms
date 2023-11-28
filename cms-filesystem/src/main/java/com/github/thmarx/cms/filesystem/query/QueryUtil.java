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

import com.github.thmarx.cms.filesystem.MetaData;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public final class QueryUtil {
	
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
		LTE
		;
	}
	
	public static Operator operator4String (final String operator) {
		if (Strings.isNullOrEmpty(operator)) {
			return Operator.EQ;
		}
		return switch (operator) {
			case "=" -> Operator.EQ;
			case "!=" -> Operator.NOT_EQ;
			case ">" -> Operator.GT;
			case ">=" -> Operator.GTE;
			case "<" -> Operator.LT;
			case "<=" -> Operator.LTE;
			default -> throw new IllegalArgumentException("unknown operator " + operator);
		};
	}

	protected static Map<Object, List<MetaData.MetaNode>> groupby (final Collection<MetaData.MetaNode> nodes, final String field) {
		return nodes.stream().collect(Collectors.groupingBy((node) -> getValue(node.data(), field)));
	}
	
	protected static List<MetaData.MetaNode> sorted(final Collection<MetaData.MetaNode> nodes, final String field, final boolean asc) {
		
		var tempNodes = nodes.stream().sorted(
				(node1, node2) -> {
					var value1 = getValue(node1.data(), field);
					var value2 = getValue(node2.data(), field);
					
					return compare(value1, value2);
				}
		).toList();
		
		if (!asc) {
			tempNodes = tempNodes.reversed();
		}
		
		return tempNodes;
	}
	
	private static int compare (Object o1, Object o2) {
		if (Objects.equals(o1, o2)) {
			return 0;
		}
		if (o1 == null) {
			return -1;
		}
		if (o2 == null ) {
			return 1;
		}
		
		if (!o1.getClass().equals(o2.getClass())) {
			return 0;
		}
				
		if (o1 instanceof Float) {
			return Float.compare((float)o1, (float)o2);
		} else if (o1 instanceof Double) {
			return Double.compare((double)o1, (double)o2);
		} else if (o1 instanceof Short) {
			return Short.compare((short)o1, (short)o2);
		} else if (o1 instanceof Integer) {
			return Integer.compare((int)o1, (int)o2);
		} else if (o1 instanceof Long) {
			return Long.compare((long)o1, (long)o2);
		} else if (o1 instanceof String string) {
			return string.compareTo((String)o2);
		} else if (o1 instanceof Date date) {
			return date.compareTo((Date)o2);
		}
		
		return 0;
	}

	protected static Collection<MetaData.MetaNode> filtered(final Collection<MetaData.MetaNode> nodes, final String field, final Object value, final Operator operator) {
		return nodes.stream().filter(createPredicate(field, value, operator)).toList();
	}

	private static Predicate<? super MetaData.MetaNode> createPredicate(final String field, final Object value, final Operator operator) {
		return (node) -> {
			var node_value = getValue(node.data(), field);
			
			if (node_value == null) {
				return false;
			}
			
			if (Operator.EQ.equals(operator)) {
				return Objects.equals(node_value, value);
			} else if (Operator.NOT_EQ.equals(operator)) {
				return !Objects.equals(node_value, value);
			} else if (Operator.CONTAINS.equals(operator) && node_value instanceof List) {
				return ((List)node_value).contains(value);
			} else if (Operator.CONTAINS_NOT.equals(operator) && node_value instanceof List) {
				return !((List)node_value).contains(value);
			} else if (Operator.GT.equals(operator)) {
				return compare(node_value, value) > 0;
			} else if (Operator.GTE.equals(operator)) {
				return compare(node_value, value) >= 0;
			} else if (Operator.LT.equals(operator)) {
				return compare(node_value, value) < 0;
			} else if (Operator.LTE.equals(operator)) {
				return compare(node_value, value) <= 0;
			} else if (Operator.IN.equals(operator)) {
				List<?> values = Collections.emptyList();
				if (value instanceof List) {
					values = (List<?>)value;
				} else if (value.getClass().isArray()) {
					values = Arrays.asList((Object[])value);
				}
				return values.contains(node_value);
			} else if (Operator.NOT_IN.equals(operator)) {
				List<?> values = Collections.emptyList();
				if (value instanceof List) {
					values = (List<?>)value;
				} else if (value.getClass().isArray()) {
					values = Arrays.asList((Object[])value);
				}
				return !values.contains(node_value);
			}
			
			log.error("unknown operation " + operator.name());
			return false;
		};
	}

	private static Object getValue(final Map<String, Object> map, final String field) {
		String[] keys = field.split("\\.");
		Map subMap = map;
		for (int i = 0; i < keys.length - 1; i++) {
			subMap = (Map<String, Object>) subMap.getOrDefault(keys[i], Collections.emptyMap());
		}
		return subMap.get(keys[keys.length - 1]);
	}
}
