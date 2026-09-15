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
import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.db.VariantSearchMode;
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.CurrentNodeFeature;
import com.condation.cms.api.feature.features.CurrentCollectionItemFeature;
import com.condation.cms.api.feature.features.WorkflowFeature;
import com.condation.cms.api.feature.features.EventBusFeature;
import com.condation.cms.api.eventbus.events.InvalidateContentCacheEvent;
import com.condation.cms.api.eventbus.events.ReIndexContentMetaDataEvent;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.ui.extensions.UIRemoteMethodExtensionPoint;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.modules.api.annotation.Extension;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.condation.cms.api.ui.annotations.RemoteMethod;
import com.condation.cms.api.workflow.WFTransitionException;
import com.condation.cms.api.workflow.Workflow;
import com.condation.cms.api.workflow.WFTransition;
import com.condation.cms.api.workflow.WFStatusQueryProvider;
import com.condation.cms.auth.services.AuthorizationService;
import com.condation.cms.auth.services.Realm;
import com.condation.cms.auth.services.RoleService;
import com.condation.cms.auth.services.User;
import com.condation.cms.auth.services.UserService;
import com.condation.cms.auth.services.WorkflowAuthorizationService;
import com.condation.cms.modules.ui.model.NodeDTO;
import com.condation.cms.modules.ui.utils.NumberUtils;
import java.io.IOException;
import java.util.Optional;
import java.util.List;

/**
 *
 * @author t.marx
 */
@Slf4j
@Extension(UIRemoteMethodExtensionPoint.class)
public class RemoteWorkflowEndpointsExtension extends AbstractRemoteMethodeExtension {

	private static final String TRANSITIONS = "transitions";
	private static final String STATUS = "status";
	
	private Optional<WorkflowTarget> getContentTarget(Map<String, Object> parameters, String uri) {
		var node = getContentRepository(parameters).get(uri);
		if (node.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new WorkflowTarget(
				node.get(),
				false,
				null,
				null));
	}

	private Optional<WorkflowTarget> getWorkflowTarget(Map<String, Object> parameters) throws RPCException {
		if (!parameters.containsKey("uri")
				&& getRequestContext().has(CurrentCollectionItemFeature.class)) {
			var item = getRequestContext().get(CurrentCollectionItemFeature.class).item();
			if (getCollectionRepository(parameters).access(item.collection()) != CollectionAccess.READ_WRITE) {
				return Optional.empty();
			}
			var node = new ContentNode(
					item.path(),
					item.path(),
					item.id() + ".md",
					new HashMap<>(item.meta()));
			return Optional.of(new WorkflowTarget(
					node,
					true,
					item.collection(),
					item.id()));
		}
		return getContentTarget(parameters, contentUri(parameters));
	}

	private record WorkflowTarget(
			ContentNode node,
			boolean collection,
			String collectionName,
			String itemId) {
	}
    
	@RemoteMethod(name = "workflow.manager.node.status", permissions = {Permissions.CONTENT_EDIT})
	public Object nodeStatus(Map<String, Object> parameters) throws RPCException {

		Map<String, Object> result = new HashMap<>();

		var target = getWorkflowTarget(parameters);
		if (target.isEmpty()) {
			return result;
		}

		var node = target.get().node();
        final Workflow workflow = getContext().get(WorkflowFeature.class).workflow();

		var status = workflow.getStatusProvider().status(node);

		result.put(STATUS, status);
		result.put(TRANSITIONS, transitionDtos(allowedTransitions(workflow, node)));

		return result;
	}

	@RemoteMethod(name = "workflow.transitions.get", permissions = {Permissions.CONTENT_EDIT})
	public Object getTransitions(Map<String, Object> parameters) throws RPCException {

		Map<String, Object> result = new HashMap<>();

		var target = getWorkflowTarget(parameters);
		if (target.isEmpty()) {
			result.put(TRANSITIONS, java.util.List.of());
			return result;
		}

		Workflow workflow = getContext().get(WorkflowFeature.class).workflow();
		result.put(TRANSITIONS, transitionDtos(allowedTransitions(workflow, target.get().node())));

		return result;
	}

	@RemoteMethod(name = "workflow.pages.unpublished", permissions = {Permissions.CONTENT_EDIT})
	public Object unpublishedPages(Map<String, Object> parameters) throws RPCException {
		long requestedPage = Math.max(1, NumberUtils.toLong(parameters.getOrDefault("page", 1L)));
		long requestedSize = Math.clamp(NumberUtils.toLong(parameters.getOrDefault("size", 10L)), 1, 100);
		Workflow workflow = getContext().get(WorkflowFeature.class).workflow();
		var repository = getContentRepository(parameters);
		var query = repository.query()
				.variants(VariantSearchMode.ORIGINAL);
		Page<ContentNode> page;

		if (workflow.getStatusProvider() instanceof WFStatusQueryProvider queryProvider) {
			page = queryProvider.unpublished(query).page(requestedPage, requestedSize);
		} else {
			log.warn("Workflow '{}' status provider does not implement WFStatusQueryProvider; "
					+ "falling back to in-memory unpublished-page filtering", workflow.getId());
			List<ContentNode> unpublished = query.get().stream()
					.filter(node -> !repository.variantContext(node).isVariant())
					.filter(node -> !workflow.getStatusProvider().isPublished(node))
					.toList();
			page = inMemoryPage(unpublished, requestedPage, requestedSize);
		}

		return mapPage(page);
	}

	@RemoteMethod(name = "workflow.transit", permissions = {Permissions.WORKFLOW_EXECUTE})
	public Object transit(Map<String, Object> parameters) throws RPCException {
		var result = new HashMap<String, Object>();
		try {
			var transitionId = requiredTransitionId(parameters);

			var target = getWorkflowTarget(parameters);
			if (target.isEmpty()) {
				throw new RPCException(404, "content node not found");
			}

			var contentNode = target.get().node();

			Workflow workflow = getContext().get(WorkflowFeature.class).workflow();
			WFTransition transition = workflow.getNextTransitions(contentNode).stream()
					.filter(candidate -> candidate.id().equals(transitionId))
					.findFirst()
					.orElseThrow(() -> new RPCException(400, "unknown or currently unavailable transition: " + transitionId));
			ensureAllowed(transition);
			workflow.transit(transitionId, contentNode);

			if (target.get().collection()) {
				var repository = getMutableCollectionRepository(parameters);
				var item = repository.get(target.get().collectionName(), target.get().itemId())
						.orElseThrow(() -> new RPCException(404, "collection item not found"));
				repository.save(
						target.get().collectionName(),
						target.get().itemId(),
						contentNode.data(),
						item.content());
			} else {
				var repository = getMutableContentRepository(parameters);
				var document = repository.load(contentNode).orElseThrow();
				repository.save(contentNode.path(), contentNode.data(), document.content());
			}
			getContext().get(EventBusFeature.class).eventBus().publish(new InvalidateContentCacheEvent());

			result.put("success", true);
		} catch (RPCException ex) {
			throw ex;
		} catch (IOException | WFTransitionException ex) {
			log.error("error transit workflow", ex);
			throw new RPCException(0, ex.getMessage());
		}
		return result;
	}

	private List<WFTransition> allowedTransitions(Workflow workflow, ContentNode node) {
		Optional<User> user = currentUser();
		if (user.isEmpty()) return List.of();
		return workflowAuthorizationService().allowedTransitions(user.get(), workflow.getNextTransitions(node));
	}

	private List<Map<String, Object>> transitionDtos(List<WFTransition> transitions) {
		return transitions.stream().map(transition -> Map.<String, Object>of(
				"id", transition.id(),
				"label", transition.label(),
				"description", transition.description() == null ? "" : transition.description()
		)).toList();
	}

	private Page<ContentNode> inMemoryPage(List<ContentNode> nodes, long requestedPage, long requestedSize) {
		long totalItems = nodes.size();
		long totalPages = totalItems == 0 ? 0 : (totalItems + requestedSize - 1) / requestedSize;
		int pageNumber = (int) Math.clamp(requestedPage, 1, Math.max(1, totalPages));
		int from = (int) Math.min((pageNumber - 1) * requestedSize, totalItems);
		int to = (int) Math.min(from + requestedSize, totalItems);
		return new Page<>(totalItems, requestedSize, totalPages, pageNumber, nodes.subList(from, to));
	}

	private Page<NodeDTO> mapPage(Page<ContentNode> page) {
		List<NodeDTO> items = page.getItems().stream().map(node -> {
			return new NodeDTO(HTTPUtil.modifyUrl(node.url(), getContext()), node.data());
		}).toList();
		return new Page<>(page.getTotalItems(), page.getPageSize(), page.getTotalPages(), page.getPage(), items);
	}

	private void ensureAllowed(WFTransition transition) throws RPCException {
		User user = currentUser().orElseThrow(() -> new RPCException(403, "workflow transition not permitted"));
		if (!workflowAuthorizationService().canExecute(user, transition)) {
			throw new RPCException(403, "workflow transition not permitted");
		}
	}

	private Optional<User> currentUser() {
		String username = getUserName();
		if (username.isBlank()) return Optional.empty();
		return getContext().get(InjectorFeature.class).injector().getInstance(UserService.class)
				.byUsername(Realm.of("manager-users"), username);
	}

	private AuthorizationService authorizationService() {
		RoleService roleService = getContext().get(InjectorFeature.class).injector().getInstance(RoleService.class);
		return new AuthorizationService(roleService);
	}

	private WorkflowAuthorizationService workflowAuthorizationService() {
		return new WorkflowAuthorizationService(authorizationService());
	}

	private String requiredTransitionId(Map<String, Object> parameters) throws RPCException {
		if (parameters.get("transitionId") instanceof String transitionId && !transitionId.isBlank()) {
			return transitionId;
		}
		throw new RPCException(400, "transitionId must not be blank");
	}

	private String contentUri(Map<String, Object> parameters) throws RPCException {
		var value = parameters.get("uri");
		if (value instanceof String uri && !uri.isBlank()) {
			return uri;
		}
		if (getRequestContext().has(CurrentNodeFeature.class)) {
			return getRequestContext().get(CurrentNodeFeature.class).node().uri();
		}
		throw new RPCException(400, "uri must not be blank");
	}

}
