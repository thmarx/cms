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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.extensions.AbstractExtensionPoint;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.feature.features.HookSystemFeature;
import com.condation.cms.api.ui.action.UIScriptAction;
import com.condation.cms.api.ui.elements.Menu;
import com.condation.cms.api.ui.elements.MenuEntry;
import com.condation.cms.api.ui.extensions.UIActionsExtensionPoint;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.cms.modules.ui.utils.UIHooks;
import com.condation.modules.api.annotation.Extension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Adds one manager menu entry for every writable collection of the site. */
@Extension(UIActionsExtensionPoint.class)
public class CollectionMenuExtension extends AbstractExtensionPoint implements UIActionsExtensionPoint {

	@Override
	public void addMenuItems(Menu menu) {
		var repository = getContext().get(RepositoryFeature.class).collectionRepository();
		var names = repository.names().stream()
				.filter(name -> repository.access(name) == CollectionAccess.READ_WRITE)
				.sorted()
				.toList();
		if (names.isEmpty()) {
			return;
		}

		var contentTypes = new UIHooks(getRequestContext().get(HookSystemFeature.class).hookSystem()).contentTypes();
		var position = new AtomicInteger(1);
		var children = names.stream().map(name -> MenuEntry.builder()
				.id("collection-" + name)
				.name(contentTypes.getCollection(name).map(type -> type.label()).orElse(name))
				.position(position.getAndIncrement())
				.permissions(List.of(Permissions.CONTENT_EDIT))
				.action(new UIScriptAction(
						HTTPUtil.modifyUrl("/manager/actions/collection/manage-collection", getContext()),
						Map.of("collection", name)))
				.children(new ArrayList<>())
				.build()).toList();

		menu.addMenuEntry(MenuEntry.builder()
				.id("collections-menu")
				.name("Collections")
				.position(5)
				.permissions(List.of(Permissions.CONTENT_EDIT))
				.children(new ArrayList<>(children))
				.build());
	}
}
