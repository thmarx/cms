package com.condation.cms.modules.ui.extensionpoints;

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

import com.condation.cms.api.extensions.TemplateModelExtendingExtensionPoint;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.feature.features.IsPreviewFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.utils.JSONUtil;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.modules.api.annotation.Extension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author thorstenmarx
 */
@Extension(TemplateModelExtendingExtensionPoint.class)
public class UiTemplateModelExtension extends TemplateModelExtendingExtensionPoint {

	@Override
	public Map<String, Object> getModel() {
		return Map.of("ui", new UIHelper(getRequestContext(), getContext()));	
	}	
	
	@RequiredArgsConstructor
	public static class UIHelper {
		
		private final RequestContext requestContext;
        private final SiteModuleContext siteContext;
		
		public String editMeta (String editor, String element) {
			return editMeta(editor, element, Collections.emptyMap());
		}
		
		public String editMeta (String editor, String element, Map<String, Object> options) {
			if (!requestContext.has(IsPreviewFeature.class)) {
				return "";
			}
			return " data-cms-editor='%s' data-cms-editor-options='%s' data-cms-element='meta' data-cms-meta-element='%s' ".formatted(
					editor, 
					JSONUtil.toJson(options),
					element);
		}
		
		public String editMeta (String editor, String element, String uri, String toolbar) {
			return editMeta(editor, element, uri, toolbar, Collections.emptyMap());
		}
		
		public String editMeta (String editor, String element, String uri, String toolbar, Map<String, Object> options) {
			if (!requestContext.has(IsPreviewFeature.class)) {
				return "";
			}
			return " data-cms-editor='%s' data-cms-editor-options='%s' data-cms-element='meta' data-cms-meta-element='%s' %s ".formatted(
					editor, 
					JSONUtil.toJson(options),
					element, 
					toolbar(toolbar, uri)
			);
		}
		
		
		public String toolbar (String id, String type, String[] actions) {
			return toolbar(id, type, actions, Map.of());
		}
		public String toolbar (String id, String type, String[] actions, Map<String, Object> additional) {
			if (!requestContext.has(IsPreviewFeature.class)) {
				return "";
			}
			Map<String, Object> base = Map.of(
					"id", id,
					"type", type,
					"actions", actions
			);
			HashMap<String, Object> toolbar = new HashMap<>(additional);
			toolbar.putAll(base);
			return " data-cms-toolbar='%s'  ".formatted(
					JSONUtil.toJson(toolbar)
			);
		}
		public String toolbar (String id, String uri) {
			return toolbar(id, uri, new String[0]);
		}

		public String collectionToolbar(CollectionItem item, String[] actions) {
			return collectionToolbar(item, actions, Map.of());
		}

		public String collectionToolbar(
				CollectionItem item,
				String[] actions,
				Map<String, Object> additional) {
			if (!requestContext.has(IsPreviewFeature.class)) {
				return "";
			}
			if (item == null) {
				return "";
			}
			var collections = siteContext.get(RepositoryFeature.class).collectionRepository();
			if (collections.access(item.collection()) != CollectionAccess.READ_WRITE) {
				return "";
			}
			var options = new HashMap<>(additional);
			options.put("collection", item.collection());
			options.put("itemId", item.id());
			return toolbar(item.collection() + "-" + item.id(), "collectionItem", actions, options);
		}
		
		public String mediaToolbar (String [] actions, Map<String, Object> options) {
			if (!requestContext.has(IsPreviewFeature.class)) {
				return "";
			}
			
			Map<String, Object> toolbar = Map.of(
					"actions", actions,
					"options", options
			);
			return " data-cms-media-toolbar='%s'  ".formatted(
					JSONUtil.toJson(toolbar)
			);
		}
		
	}
}
