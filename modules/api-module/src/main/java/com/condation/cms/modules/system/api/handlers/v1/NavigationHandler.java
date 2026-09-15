package com.condation.cms.modules.system.api.handlers.v1;

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

import com.condation.cms.api.Constants;
import com.condation.cms.api.extensions.http.HttpHandler;
import com.condation.cms.api.model.NavNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.cms.api.utils.RequestUtil;
import com.condation.cms.content.template.functions.navigation.NavigationFunction;
import com.condation.cms.core.configuration.GSONProvider;
import com.condation.cms.modules.system.api.helpers.NodeHelper;
import com.condation.cms.modules.system.api.services.ApiNavNode;
import java.util.Collections;
import java.util.List;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 *
 * @author thmar
 */
public class NavigationHandler implements HttpHandler {

	private final ContentRepository contentRepository;
	private final RequestContext requestContext;
	
	public NavigationHandler(final ContentRepository contentRepository, final RequestContext requestContext) {
		this.requestContext = requestContext;
		this.contentRepository = contentRepository;
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		var uri = RequestUtil.getContentPath(request);
		uri = uri.replaceFirst("api/v1/navigation", "");
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		
		var queryParameters = HTTPUtil.queryParameters(request.getHttpURI().getQuery());
		var start = queryParameters.getOrDefault("start", List.of(".")).getFirst();
		var depth = Integer.valueOf(queryParameters.getOrDefault("depth", List.of("1")).getFirst());
		var contentType = queryParameters.getOrDefault("contentType", List.of(Constants.ContentTypes.HTML)).getFirst();
		
		var requestUrl = uri.isEmpty() ? "/" : "/" + uri;
		var repositoryPath = uri;
		var startNode = contentRepository.findByUrl(requestUrl)
				.or(() -> contentRepository.get(repositoryPath));

		if (startNode.isEmpty()) {
			response.setStatus(404);
			callback.succeeded();
			return true;
		}
		
		NavigationFunction navFN = new NavigationFunction(
				contentRepository, startNode.get(), requestContext);
		
		var navNodes = navFN.contentType(contentType).list(start, depth);
		
		var nodes = navNodes.stream().map(navNode -> {
			return new ApiNavNode(navNode.path(), navNode.name(), NodeHelper.getLinks(navNode.path(), request), mapChildren(navNode.children(), request));
		}).toList();
		
		
		response.getHeaders().add(HttpHeader.CONTENT_TYPE, "application/json; charset=utf-8");
		Content.Sink.write(response, true, GSONProvider.GSON.toJson(new NavResponse(nodes)), callback);
		
		return true;
	}
	
	public static record NavResponse (List<ApiNavNode> nodes) {}
	
	private List<ApiNavNode> mapChildren (List<NavNode> children, Request request) {
		if (children == null || children.isEmpty()) {
			return Collections.emptyList();
		}
		return children.stream().map(child -> {
			return new ApiNavNode(child.path(), child.name(), NodeHelper.getLinks(child.path(), request), mapChildren(child.children(), request));
		}).toList();
	}
	
}
