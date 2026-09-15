package com.condation.cms.modules.ui.http;

/*-
 * #%L
 * UI Module
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
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.feature.features.CurrentNodeFeature;
import com.condation.cms.api.feature.features.CurrentCollectionItemFeature;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.ui.rpc.RPCError;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.ui.rpc.RPCResult;
import com.condation.cms.auth.services.User;
import com.condation.cms.modules.ui.model.RemoteCall;
import com.condation.cms.modules.ui.services.RemoteMethodService;
import com.condation.cms.modules.ui.utils.json.UIGsonProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class RemoteCallHandler extends JettyHandler {

	public static final String CONTENT_URI_HEADER = "X-CMS-Content-Uri";
	public static final String COLLECTION_HEADER = "X-CMS-Collection";
	public static final String COLLECTION_ITEM_HEADER = "X-CMS-Collection-Item";

	private final RemoteMethodService remoteCallService;
	private final SiteModuleContext moduleContext;
	private final RequestContext requestContext;

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {

		if (!request.getMethod().equalsIgnoreCase("POST")) {
			Response.writeError(request, response, callback, HttpStatus.METHOD_NOT_ALLOWED_405, "");
			return true;
		}

		String body = getBody(request);
		var remoteCall = UIGsonProvider.INSTANCE.fromJson(body, RemoteCall.class);

		RPCResult rpcResult;
		try {
			final Optional<User> userOpt = getUser(request, moduleContext, requestContext);
			if (userOpt.isEmpty()) {
				rpcResult = new RPCResult(new RPCError("no user present"));
			} else {
				setCurrentContentNode(request);
				Optional<?> result = remoteCallService.execute(remoteCall.method(), remoteCall.parameters(), userOpt.get());
				if (result.isPresent()) {
					rpcResult = new RPCResult(result.get());
				} else {
					rpcResult = new RPCResult();
				}
			}

		} catch (Exception e) {
			rpcResult = buildErrorResult(e, remoteCall.method());
		}

		response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json; charset=UTF-8");
		response.setStatus(200);
		Content.Sink.write(response, true, UIGsonProvider.INSTANCE.toJson(rpcResult), callback);

		return true;
	}

	private void setCurrentContentNode(Request request) {
		if (setCurrentCollectionItem(request)) {
			return;
		}
		var uri = request.getHeaders().get(CONTENT_URI_HEADER);
		if (uri == null || uri.isBlank()) {
			return;
		}
		if (!moduleContext.has(RepositoryFeature.class)) {
			return;
		}
		var content = moduleContext.get(RepositoryFeature.class).contentRepository();
		content.findByUrl(uri.trim())
				.or(() -> content.get(uri.trim()))
				.ifPresent(node -> requestContext.add(
						CurrentNodeFeature.class,
						new CurrentNodeFeature(node)
				));
	}

	private boolean setCurrentCollectionItem(Request request) {
		var collectionName = request.getHeaders().get(COLLECTION_HEADER);
		if (collectionName == null || collectionName.isBlank()) {
			return false;
		}
		var itemId = request.getHeaders().get(COLLECTION_ITEM_HEADER);
		if (itemId == null || itemId.isBlank()) {
			return false;
		}
		if (!moduleContext.has(RepositoryFeature.class)) {
			return false;
		}
		try {
			var item = moduleContext.get(RepositoryFeature.class)
					.collectionRepository()
					.get(collectionName.trim(), itemId.trim());
			item.ifPresent(value -> {
				requestContext.add(CurrentCollectionItemFeature.class, new CurrentCollectionItemFeature(value));
				requestContext.add(CurrentNodeFeature.class, new CurrentNodeFeature(new ContentNode(
						value.path(), value.path(), value.id() + ".md", value.meta())));
			});
			return item.isPresent();
		} catch (IllegalArgumentException ex) {
			log.warn("invalid collection preview context", ex);
			return false;
		}
	}

	private RPCResult buildErrorResult(Exception e, String method) {
		log.error("error executing endpoint {}", method, e);
		if (e instanceof RPCException rpcException) {
			return new RPCResult(new RPCError(rpcException.getCode(), rpcException.getMessage()));
		}
		return new RPCResult(new RPCError(e.getMessage()));
	}

}
