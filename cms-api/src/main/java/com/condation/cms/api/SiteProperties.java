package com.condation.cms.api;

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


import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *
 * @author t.marx
 */
public class SiteProperties extends ThemeProperties {
	
	public SiteProperties (final Map<String, Object> properties) {
		super(properties);
	}
	
	public List<String> hostnames () {
		var hostnames = properties.getOrDefault("hostname", "localhost");
		
		if (hostnames instanceof String hostname) {
			return List.of(hostname);
		} else if (hostnames instanceof List) {
			return (List<String>) hostnames;
		} else {
			return List.of("localhost");
		}
	}
	
	public String markdownEngine () {
		return (String)getSubMap("markdown").get("engine");
	}
	
	public String contextPath () {
		return (String) properties.getOrDefault("context_path", "/");
	}
	
	public String id () {
		return (String) properties.getOrDefault("id", "default");
	}
	
	public String theme () {
		return (String) properties.get("theme");
	}

	public String queryIndexMode () {
		return (String) getSubMap("index", getSubMap("query")).getOrDefault("mode", "MEMORY");
	}
	
	public Locale locale () {
		if (properties.containsKey("language")) {
			Locale.forLanguageTag((String)properties.get("language"));
		}
		return Locale.getDefault();
	}
	
	public String defaultContentType () {
		return (String)getSubMap("content").getOrDefault("type", Constants.DEFAULT_CONTENT_TYPE);
	}
	
	public List<String> contentPipeline () {
		return (List<String>)getSubMap("content").getOrDefault("pipeline", Constants.DEFAULT_CONTENT_PIPELINE);
	}
	
	public String cacheEngine() {
		return (String) getSubMap("cache").getOrDefault("engine", Constants.DEFAULT_CACHE_ENGINE);
	}
	public boolean cacheContent() {
		return (boolean) getSubMap("cache").getOrDefault("content", Constants.DEFAULT_CONTENT_CACHE_ENABLED);
	}
}
