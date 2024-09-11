package com.github.thmarx.cms.api.content;

/*-
 * #%L
 * cms-api
 * %%
 * Copyright (C) 2023 - 2024 CondationCMS
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


/**
 *
 * @author t.marx
 */
public record ContentResponse(String content, String contentType, ContentNode node) {

	public ContentResponse (String content, ContentNode node) {
		this(content, Constants.DEFAULT_CONTENT_TYPE, node);
	}
	
	public ContentResponse (ContentNode node) {
		this("", Constants.DEFAULT_CONTENT_TYPE, node);
	}
	
	public boolean isRedirect () {
		return node != null && node.isRedirect();
	}
};
