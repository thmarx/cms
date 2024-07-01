package com.github.thmarx.cms.api.db;

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

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author t.marx
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Page<T> {
	
	public static final Page EMPTY = new Page(0, 0, 0, 1, Collections.EMPTY_LIST);
	
	/**
	 * Total number of items
	 */
	private long totalItems;
	/**
	 * Total number of items per page
	 */
	private long pageSize;
	/**
	 * Total number of pages
	 */
	private long totalPages;
	/**
	 * Number of the current page
	 */
	private int page;
	
	/**
	 * Items of the current page;
	 */
	private List<T> items;
}
