package com.github.thmarx.cms.filesystem.metadata.query;

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
import com.github.thmarx.cms.api.db.ContentNode;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
public class ExcerptMapperFunction<T> implements Function<ContentNode, T> {
	
	private final BiFunction<ContentNode, Integer, T> nodeMapper;

	@Setter
	private int excerpt = Constants.DEFAULT_EXCERPT_LENGTH;
	
	@Override
	public T apply(ContentNode t) {
		return nodeMapper.apply(t, excerpt);
	}

}
