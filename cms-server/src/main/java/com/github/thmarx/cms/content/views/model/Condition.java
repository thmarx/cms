package com.github.thmarx.cms.content.views.model;

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
