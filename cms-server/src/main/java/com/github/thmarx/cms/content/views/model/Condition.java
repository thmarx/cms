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

import java.util.HashMap;

/**
 *
 * @author t.marx
 */

public class Condition extends HashMap<String, Object> {
//	private String name;
//	private String key;
//	private String operator;
//	private String value;
	
	public String getName () {
		return (String)get("name");
	}
	public String getKey () {
		return (String)get("key");
	}
	public String getOperator () {
		return (String)get("operator");
	}

	public <T> T getValue (Class<T> clazz) {
		return (T)get("value");
	}
}
