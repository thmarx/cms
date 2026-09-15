package com.condation.cms.modules.ui.extensionpoints.remotemethods;

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

import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.ContentQuery;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.db.VariantSearchMode;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.feature.features.CurrentCollectionItemFeature;
import com.condation.cms.api.feature.features.WorkflowFeature;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.request.RequestContextScope;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.workflow.WFStatusProvider;
import com.condation.cms.api.workflow.WFStatusQueryProvider;
import com.condation.cms.api.workflow.Workflow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RemoteWorkflowEndpointsExtensionTest {

	@Mock
	private SiteModuleContext moduleContext;

	@Mock
	private ContentRepository contentRepository;

	@Mock
	private CollectionRepository collectionRepository;

	private RemoteWorkflowEndpointsExtension endpoints;

	@BeforeEach
	void setUp() {
		endpoints = new RemoteWorkflowEndpointsExtension() {
			@Override
			protected ContentRepository getContentRepository(Map<String, Object> parameters) {
				return contentRepository;
			}

			@Override
			protected CollectionRepository getCollectionRepository(Map<String, Object> parameters) {
				return collectionRepository;
			}
		};
		endpoints.setContext(moduleContext);
		lenient().when(collectionRepository.access("blog")).thenReturn(CollectionAccess.READ_WRITE);
	}

	@Test
	void nodeStatus_returnsResultWithoutStatus_whenContentNodeNotFound() throws RPCException {
		Map<String, Object> params = Map.of("uri", "missing.md");

		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) endpoints.nodeStatus(params);

		assertThat(result).doesNotContainKey("status")
                .doesNotContainKey("error");
	}

	@Test
	void getTransitions_returnsEmptyTransitions_whenContentNodeNotFound() throws RPCException {
		Map<String, Object> params = Map.of("uri", "missing.md");

		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) endpoints.getTransitions(params);

		assertThat(result).containsEntry("transitions", java.util.List.of())
                .doesNotContainKey("error");
	}

	@Test
	void transit_throwsRPCException_whenContentNodeNotFound() {
		Map<String, Object> params = Map.of("uri", "missing.md", "transitionId", "publish");

		assertThatThrownBy(() -> endpoints.transit(params))
				.isInstanceOf(RPCException.class)
				.satisfies(ex -> assertThat(((RPCException) ex).getCode()).isEqualTo(404));
	}

	@Test
	void nodeStatus_usesCurrentCollectionItemOnDetailPage() throws Exception {
		var item = new CollectionItem(
				"first",
				"blog",
				"blog/first.md",
				"Body",
				Map.of("title", "First", "status", "draft"));
		var requestContext = new RequestContext();
		requestContext.add(
				CurrentCollectionItemFeature.class,
				new CurrentCollectionItemFeature(item));
		var workflow = mock(Workflow.class);
		var statusProvider = mock(WFStatusProvider.class);
		var status = new WFStatusProvider.Status(false, null, null, true, "draft");
		when(workflow.getStatusProvider()).thenReturn(statusProvider);
		when(statusProvider.status(any(ContentNode.class))).thenReturn(status);
		when(moduleContext.get(WorkflowFeature.class)).thenReturn(new WorkflowFeature(workflow));

		@SuppressWarnings("unchecked")
		var result = (Map<String, Object>) ScopedValue.where(
				RequestContextScope.REQUEST_CONTEXT,
				requestContext).call(() -> endpoints.nodeStatus(Map.of()));

		assertThat(result).containsEntry("status", status)
                .containsEntry("transitions", List.of());
	}

	@Test
	void nodeStatus_ignoresReferencedCollectionItems() throws Exception {
		when(collectionRepository.access("blog")).thenReturn(CollectionAccess.READ_ONLY);
		var requestContext = new RequestContext();
		requestContext.add(
				CurrentCollectionItemFeature.class,
				new CurrentCollectionItemFeature(new CollectionItem(
						"first", "blog", "blog/first.md", "Body", Map.of())));

		@SuppressWarnings("unchecked")
		var result = (Map<String, Object>) ScopedValue.where(
				RequestContextScope.REQUEST_CONTEXT,
				requestContext).call(() -> endpoints.nodeStatus(Map.of()));

		assertThat(result).isEmpty();
	}

	@Test
	void unpublishedPages_delegatesFilteringAndPaginationToWorkflowProvider() throws RPCException {
		@SuppressWarnings("unchecked")
		ContentQuery<ContentNode> query = mock(ContentQuery.class);
		WFStatusQueryProvider statusProvider = mock(WFStatusQueryProvider.class);
		Workflow workflow = mock(Workflow.class);
		Page<ContentNode> emptyPage = new Page<>(0, 5, 0, 1, List.of());

		doReturn(query).when(contentRepository).query();
		when(query.variants(VariantSearchMode.ORIGINAL)).thenReturn(query);
		when(statusProvider.unpublished(query)).thenReturn(query);
		when(query.page(1, 5)).thenReturn(emptyPage);
		when(workflow.getStatusProvider()).thenReturn(statusProvider);
		when(moduleContext.get(WorkflowFeature.class)).thenReturn(new WorkflowFeature(workflow));

		@SuppressWarnings("unchecked")
		Page<Object> result = (Page<Object>) endpoints.unpublishedPages(Map.of("page", 1, "size", 5));

		assertThat(result.getItems()).isEmpty();
		assertThat(result.getTotalItems()).isZero();
		verify(query).variants(VariantSearchMode.ORIGINAL);
		verify(statusProvider).unpublished(query);
		verify(query).page(1, 5);
	}
}
