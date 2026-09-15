package com.condation.cms.api.db;

/*-
 * #%L
 * CMS Api
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


import com.condation.cms.api.Constants;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 *
 * @author t.marx
 * @param <T>
 */
public interface ContentQuery<T> {

	ContentQuery<T> excerpt(final long excerptLength);

	Page<T> page(final long page, final long size);
	
	List<T> get();
	
	Map<Object, List<ContentNode>> groupby(final String field);

	Sort<T> orderby(final String field);

	ContentQuery<T> json();
	
	ContentQuery<T> html();
	
	ContentQuery<T> contentType(String contentType);

	/**
	 * Restricts the query to canonical content, variants, or both.
	 *
	 * @param mode variant selection to apply
	 * @return this query
	 */
	ContentQuery<T> variants(VariantSearchMode mode);
	
	ContentQuery<T> where(final String field, final Object value);

	ContentQuery<T> where(final String field, final String operator, final Object value);

	ContentQuery<T> whereContains(final String field, final Object value);

	ContentQuery<T> whereNotContains(final String field, final Object value);

	ContentQuery<T> whereIn(final String field, final Object... value);

	ContentQuery<T> whereIn(final String field, final List<Object> value);

	ContentQuery<T> whereNotIn(final String field, final Object... value);

	ContentQuery<T> whereNotIn(final String field, final List<Object> value);
	
	ContentQuery<T> whereExists(final String field);

	ContentQuery<T> within(
			final String field,
			final double latitude,
			final double longitude,
			final double radius,
			final DistanceUnit unit);

	ContentQuery<T> within(
			final String field,
			final double latitude,
			final double longitude,
			final double radius,
			final String unit);
	
	ContentQuery<T> expression(final String expressions);

	/**
	 * Adds backend-specific comparison operators without exposing the concrete
	 * query implementation to callers.
	 */
	default ContentQuery<T> customOperators(
			Map<String, BiPredicate<Object, Object>> operators) {
		if (!operators.isEmpty()) {
			throw new UnsupportedOperationException("custom query operators are not supported");
		}
		return this;
	}

	/**
	 * Restricts the query to items whose title matches the input.
	 *
	 * Implementations with a full-text title index should override this method.
	 * The default keeps existing query implementations source compatible and uses
	 * their regular title-field matching.
	 */
	default ContentQuery<T> searchByTitle(final String input) {
		return where(Constants.MetaFields.TITLE, input);
	}

	public static interface Sort<T> {
		public ContentQuery<T> asc();

		public ContentQuery<T> desc();
	}
}
