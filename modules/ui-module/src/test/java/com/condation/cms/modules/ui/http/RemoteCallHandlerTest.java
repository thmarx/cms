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
import com.condation.cms.api.db.Content;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.collection.Collection;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.feature.features.CurrentCollectionItemFeature;
import com.condation.cms.api.feature.features.CurrentNodeFeature;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.ui.rpc.RPCError;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.ui.rpc.RPCResult;
import com.condation.cms.modules.ui.services.RemoteMethodService;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Request;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class RemoteCallHandlerTest {

	@Mock
	private RemoteMethodService remoteMethodService;

	@Mock
	private SiteModuleContext moduleContext;

	@Mock
	private RequestContext requestContext;

	@Test
	void buildErrorResult_preservesCodeFromRPCException() throws Exception {
		var handler = new RemoteCallHandler(remoteMethodService, moduleContext, requestContext);

		Method buildErrorResult = RemoteCallHandler.class.getDeclaredMethod("buildErrorResult", Exception.class, String.class);
		buildErrorResult.setAccessible(true);

		RPCException exception = new RPCException(404, "content node not found");

		RPCResult result = (RPCResult) buildErrorResult.invoke(handler, exception, "workflow.transit");

		assertThat(result.error()).isEqualTo(new RPCError(404, "content node not found"));
	}

	@Test
	void buildErrorResult_fallsBackToCodeMinusOne_forGenericException() throws Exception {
		var handler = new RemoteCallHandler(remoteMethodService, moduleContext, requestContext);

		Method buildErrorResult = RemoteCallHandler.class.getDeclaredMethod("buildErrorResult", Exception.class, String.class);
		buildErrorResult.setAccessible(true);

		Exception exception = new IllegalStateException("boom");

		RPCResult result = (RPCResult) buildErrorResult.invoke(handler, exception, "some.method");

		assertThat(result.error()).isEqualTo(new RPCError("boom"));
		assertThat(result.error().code()).isEqualTo(-1);
	}

	@Test
	void contentUriHeaderAddsCurrentNodeFeature() throws Exception {
		var context = new RequestContext();
		var content = mock(ContentRepository.class);
		var repositories = new RepositoryFeature(content, mock(CollectionRepository.class));
		var request = mock(Request.class);
		var headers = mock(HttpFields.class);
		var node = new ContentNode(
				".variants/about/summer/about.md",
				"/about",
				"about.md",
				Map.of()
		);
		when(moduleContext.has(RepositoryFeature.class)).thenReturn(true);
		when(moduleContext.get(RepositoryFeature.class)).thenReturn(repositories);
		when(request.getHeaders()).thenReturn(headers);
		when(headers.get(RemoteCallHandler.COLLECTION_HEADER)).thenReturn(null);
		when(headers.get(RemoteCallHandler.CONTENT_URI_HEADER)).thenReturn(node.uri());
		when(content.findByUrl(node.uri())).thenReturn(Optional.empty());
		when(content.get(node.uri())).thenReturn(Optional.of(node));

		var handler = new RemoteCallHandler(remoteMethodService, moduleContext, context);
		var method = RemoteCallHandler.class.getDeclaredMethod("setCurrentContentNode", Request.class);
		method.setAccessible(true);
		method.invoke(handler, request);

		assertThat(context.get(CurrentNodeFeature.class).node()).isEqualTo(node);
	}

	@Test
	void unknownContentUriHeaderDoesNotAddCurrentNodeFeature() throws Exception {
		var context = new RequestContext();
		var content = mock(ContentRepository.class);
		var repositories = new RepositoryFeature(content, mock(CollectionRepository.class));
		var request = mock(Request.class);
		var headers = mock(HttpFields.class);
		when(moduleContext.has(RepositoryFeature.class)).thenReturn(true);
		when(moduleContext.get(RepositoryFeature.class)).thenReturn(repositories);
		when(request.getHeaders()).thenReturn(headers);
		when(headers.get(RemoteCallHandler.COLLECTION_HEADER)).thenReturn(null);
		when(headers.get(RemoteCallHandler.CONTENT_URI_HEADER)).thenReturn("unknown.md");
		when(content.findByUrl("unknown.md")).thenReturn(Optional.empty());
		when(content.get("unknown.md")).thenReturn(Optional.empty());

		var handler = new RemoteCallHandler(remoteMethodService, moduleContext, context);
		var method = RemoteCallHandler.class.getDeclaredMethod("setCurrentContentNode", Request.class);
		method.setAccessible(true);
		method.invoke(handler, request);

		assertThat(context.has(CurrentNodeFeature.class)).isFalse();
	}

	@Test
	void collectionHeadersAddCurrentCollectionItemAndNodeFeatures() throws Exception {
		var context = new RequestContext();
		var collections = mock(CollectionRepository.class);
		var repositories = new RepositoryFeature(mock(ContentRepository.class), collections);
		var request = mock(Request.class);
		var headers = mock(HttpFields.class);
		var item = new CollectionItem(
				"first",
				"blog",
				"blog/first.md",
				"Body",
				Map.of("title", "First"));
		when(moduleContext.has(RepositoryFeature.class)).thenReturn(true);
		when(moduleContext.get(RepositoryFeature.class)).thenReturn(repositories);
		when(collections.get("blog", "first")).thenReturn(Optional.of(item));
		when(request.getHeaders()).thenReturn(headers);
		when(headers.get(RemoteCallHandler.COLLECTION_HEADER)).thenReturn("blog");
		when(headers.get(RemoteCallHandler.COLLECTION_ITEM_HEADER)).thenReturn("first");

		var handler = new RemoteCallHandler(remoteMethodService, moduleContext, context);
		var method = RemoteCallHandler.class.getDeclaredMethod("setCurrentContentNode", Request.class);
		method.setAccessible(true);
		method.invoke(handler, request);

		assertThat(context.get(CurrentCollectionItemFeature.class).item()).isEqualTo(item);
		assertThat(context.get(CurrentNodeFeature.class).node().uri()).isEqualTo(item.path());
	}
}
