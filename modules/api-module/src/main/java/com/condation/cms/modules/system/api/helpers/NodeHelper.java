package com.condation.cms.modules.system.api.helpers;

/*-
 * #%L
 * CMS Api Module
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
import com.condation.cms.api.configuration.configs.SiteConfiguration;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.ConfigurationFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.cms.api.db.NodeVisibility;
import com.condation.cms.api.Constants;
import java.util.Collections;
import java.util.Map;
import org.eclipse.jetty.server.Request;

/**
 *
 * @author t.marx
 */
public final class NodeHelper {

	private NodeHelper() {
	}

	public static Map<String, String> getLinks(ContentNode node, Request request) {
		if (!NodeVisibility.isVisible(node) || !Constants.NodeType.PAGE.equals(node.nodeType())) {
			return Collections.emptyMap();
		}
		return getLinks(node.uri(), request);
	}

	public static Map<String, String> getLinks(String nodeUri, Request request) {

		var requestContext = (RequestContext) request.getAttribute("_requestContext");
		var siteProperties = requestContext.get(ConfigurationFeature.class).configuration().get(SiteConfiguration.class).siteProperties();

		var contextPath = siteProperties.contextPath();
		if (!contextPath.endsWith("/")) {
			contextPath += "/";
		}

		if (nodeUri.endsWith("index.md")) {
			nodeUri = nodeUri.replaceFirst("index.md", "");
		}

		if (nodeUri.endsWith(".md")) {
			nodeUri = nodeUri.substring(0, nodeUri.length() - 3);
		}

		if (nodeUri.equals("/")) {
			nodeUri = "";
		}
		
		HTTPUtil.appendPreviewParameter(nodeUri, requestContext);

		return Map.of(
				"_self", "%sapi/v1/navigation/%s".formatted(contextPath, nodeUri),
				"_content", "%sapi/v1/content/%s".formatted(contextPath, nodeUri)
		);
	}

	public static String getPath(ContentNode node) {
		return getPath(node.uri());
	}

	public static String getPath(String uri) {
		if (uri.endsWith("index.md")) {
			uri = uri.replaceFirst("index.md", "");
		}
		if (!uri.startsWith("/")) {
			uri = "/" + uri;
		}
		if (uri.endsWith(".md")) {
			uri = uri.substring(0, uri.length() - 3);
		}
		return uri;
	}
}
