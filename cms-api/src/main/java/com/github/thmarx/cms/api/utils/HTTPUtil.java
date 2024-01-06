package com.github.thmarx.cms.api.utils;

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
import com.github.thmarx.cms.api.SiteProperties;
import com.github.thmarx.cms.api.request.RequestContext;
import com.github.thmarx.cms.api.feature.features.IsPreviewFeature;
import com.github.thmarx.cms.api.feature.features.SitePropertiesFeature;
import com.google.common.base.Strings;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author t.marx
 */
public class HTTPUtil {

	/**
	 * Adds the context according to the siteproperties and the preview to an url
	 * 
	 * @param url
	 * @param requestContext
	 * @return 
	 */
	public static String modifyUrl(String url, final RequestContext requestContext) {

		// is external url
		if (url.startsWith("http") || url.startsWith("https")) {
			return url;
		}

		url = modifyUrl(url, requestContext.get(SitePropertiesFeature.class).siteProperties());
		
		if (requestContext.has(IsPreviewFeature.class)) {
			if (url.contains("?")) {
				url += "&preview=true";
			} else {
				url += "?preview=true";
			}
		}

		return url;
	}

	/**
	 * Adds the context according to the siteproperties to an url
	 * 
	 * @param url
	 * @param siteProperties
	 * @return 
	 */
	public static String modifyUrl(String url, final SiteProperties siteProperties) {

		// is external url
		if (url.startsWith("http") || url.startsWith("https")) {
			return url;
		}

		var contextPath = siteProperties.contextPath();
		if (!"/".equals(contextPath)) {
			url = contextPath + url;
		}

		return url;
	}

	public static Map<String, List<String>> queryParameters(String query) {
		if (Strings.isNullOrEmpty(query)) {
			return Collections.emptyMap();
		}
		return Pattern.compile("&")
				.splitAsStream(query)
				.map(s -> Arrays.copyOf(s.split("=", 2), 2))
				.collect(Collectors.groupingBy(s -> decode(s[0]), Collectors.mapping(s -> decode(s[1]), Collectors.toList())));
	}

	private static String decode(final String encoded) {
		return Optional.ofNullable(encoded)
				.map(e -> URLDecoder.decode(e, StandardCharsets.UTF_8))
				.orElse(null);
	}
}
