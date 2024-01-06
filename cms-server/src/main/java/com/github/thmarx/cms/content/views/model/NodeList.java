package com.github.thmarx.cms.content.views.model;

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

import com.github.thmarx.cms.api.Constants;
import lombok.Data;

/**
 *
 * @author t.marx
 */
@Data
public class NodeList {
	private String from;
    private String reverse;
    private String sort;
    private String excerpt = String.valueOf(Constants.DEFAULT_EXCERPT_LENGTH);
    private String page = String.valueOf(Constants.DEFAULT_PAGE);
    private String size = String.valueOf(Constants.DEFAULT_PAGE_SIZE);
    private String index;
}
