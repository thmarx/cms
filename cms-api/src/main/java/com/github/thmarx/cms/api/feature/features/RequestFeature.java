package com.github.thmarx.cms.api.feature.features;

/*-
 * #%L
 * cms-api
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
import com.github.thmarx.cms.api.feature.Feature;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public record RequestFeature(String context, String uri, Map<String, List<String>> queryParameters) implements Feature {

	public RequestFeature(String uri, Map<String, List<String>> queryParameters) {
		this(null, uri, queryParameters);
	}
	
	public String getQueryParameter(String name, final String defaultValue) {
		if (!queryParameters.containsKey(name)) {
			return defaultValue;
		}

		return queryParameters.get(name).getFirst();
	}

	public int getQueryParameterAsInt(String name, final int defaultValue) {
		if (!queryParameters.containsKey(name)) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(queryParameters.get(name).getFirst());
		} catch (Exception e) {
			log.error(null, e);
		}
		return defaultValue;
	}
}
