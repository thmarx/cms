package com.condation.cms.modules.ui.utils;

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

import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.repository.CollectionAccess;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.feature.features.SitePropertiesFeature;
import com.condation.cms.api.hooks.HookSystem;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.ui.action.UIScriptAction;
import com.condation.cms.api.ui.apps.App;
import com.condation.cms.api.ui.apps.AppExtensionPoint;
import com.condation.cms.api.ui.elements.CollectionType;
import com.condation.cms.api.ui.elements.ContentTypes;
import com.condation.cms.api.ui.extensions.UIActionsExtensionPoint;
import com.condation.cms.auth.services.User;
import com.condation.modules.api.ModuleManager;
import com.google.inject.Injector;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;

class ActionFactoryAppsTest {

	private static class AnnotatedApps extends com.condation.cms.api.extensions.AbstractExtensionPoint
			implements UIActionsExtensionPoint {

		@com.condation.cms.api.ui.annotations.App(
				id = "editor-app",
				title = "Editor's app",
				icon = "/manager/public/apps/editor.svg",
				permissions = Permissions.CONTENT_EDIT)
		@com.condation.cms.api.ui.annotations.ShortCut(section = "Apps")
		@com.condation.cms.api.ui.annotations.ScriptAction(module = "/manager/actions/editor")
		public void editorApp() {
			// method is only for annotations
		}

		@com.condation.cms.api.ui.annotations.App(
				id = "manager-app",
				title = "Manager app",
				icon = "/manager/public/apps/manager.svg",
				permissions = Permissions.CACHE_INVALIDATE)
		@com.condation.cms.api.ui.annotations.ShortCut(section = "Apps")
		@com.condation.cms.api.ui.annotations.ScriptAction(module = "/manager/actions/manager")
		public void managerApp() {
			// method is only for annotations
		}
	}

	@Test
	void createsAuthorizedAppsWithContextAwareIconAndScriptAction() {
		SiteProperties siteProperties = mock(SiteProperties.class);
		when(siteProperties.contextPath()).thenReturn("/de");
		SiteModuleContext context = mock(SiteModuleContext.class);
		when(context.get(SitePropertiesFeature.class))
				.thenReturn(new SitePropertiesFeature(siteProperties));

		AppExtensionPoint extension = mock(AppExtensionPoint.class);
		when(extension.getApps()).thenReturn(List.of(
				new App(
						"menu-manager",
						"Menu Manager",
						"/manager/public/apps/menu-manager.svg",
						new UIScriptAction("/manager/actions/menu/manage-menus", Map.of()),
						List.of(Permissions.CONTENT_EDIT)),
				new App(
						"admin-only",
						"Admin only",
						"/manager/public/apps/admin.svg",
						new UIScriptAction("/manager/actions/admin", Map.of()),
						List.of(Permissions.CACHE_INVALIDATE))));
		ModuleManager moduleManager = mock(ModuleManager.class);
		when(moduleManager.extensions(AppExtensionPoint.class))
				.thenReturn(List.of(extension));

		ActionFactory factory = new ActionFactory(
				context,
				siteProperties,
				null,
				moduleManager,
				new User("editor", "hash", new String[]{"editor"}));

		Assertions.assertThat(factory.createApps()).singleElement().satisfies(app -> {
			Assertions.assertThat(app.id()).isEqualTo("menu-manager");
			Assertions.assertThat(app.icon()).isEqualTo("/de/manager/public/apps/menu-manager.svg");
			Assertions.assertThat(app.action()).isInstanceOfSatisfying(
					UIScriptAction.class,
					action -> Assertions.assertThat(action.getModule())
							.isEqualTo("/de/manager/actions/menu/manage-menus"));
		});
	}

	@Test
	void createsAnnotatedAppsAndInheritsTheirShortcutMetadata() {
		SiteProperties siteProperties = mock(SiteProperties.class);
		when(siteProperties.contextPath()).thenReturn("/de");
		SiteModuleContext context = mock(SiteModuleContext.class);
		when(context.get(SitePropertiesFeature.class))
				.thenReturn(new SitePropertiesFeature(siteProperties));

		ModuleManager moduleManager = mock(ModuleManager.class);
		when(moduleManager.extensions(AppExtensionPoint.class)).thenReturn(List.of());
		when(moduleManager.extensions(UIActionsExtensionPoint.class))
				.thenReturn(List.of(new AnnotatedApps()));

		ActionFactory factory = new ActionFactory(
				context,
				siteProperties,
				null,
				moduleManager,
				new User("editor", "hash", new String[]{"editor"}));

		Assertions.assertThat(factory.createApps()).singleElement().satisfies(app -> {
			Assertions.assertThat(app.id()).isEqualTo("editor-app");
			Assertions.assertThat(app.title()).isEqualTo("Editor's app");
			Assertions.assertThat(app.icon()).isEqualTo("/de/manager/public/apps/editor.svg");
			Assertions.assertThat(app.action()).isInstanceOfSatisfying(
					UIScriptAction.class,
					action -> Assertions.assertThat(action.getModule())
							.isEqualTo("/de/manager/actions/editor"));
		});

		Assertions.assertThat(factory.createShortCuts()).singleElement().satisfies(shortcut -> {
			Assertions.assertThat(shortcut.id()).isEqualTo("editor-app");
			Assertions.assertThat(shortcut.title()).isEqualTo("Editor's app");
			Assertions.assertThat(shortcut.icon()).isEqualTo("/de/manager/public/apps/editor.svg");
			Assertions.assertThat(shortcut.section()).isEqualTo("Apps");
			Assertions.assertThat(shortcut.permissions()).containsExactly(Permissions.CONTENT_EDIT);
		});
	}

	@Test
	void addsCollectionsToCreateContentMenu() {
		SiteProperties siteProperties = mock(SiteProperties.class);
		when(siteProperties.contextPath()).thenReturn("/de");
		SiteModuleContext context = mock(SiteModuleContext.class);
		when(context.get(SitePropertiesFeature.class))
				.thenReturn(new SitePropertiesFeature(siteProperties));
		CollectionRepository collections = mock(CollectionRepository.class);
		when(context.get(RepositoryFeature.class)).thenReturn(new RepositoryFeature(
				mock(com.condation.cms.api.repository.ContentRepository.class), collections));
		when(collections.names()).thenReturn(Set.of("blog", "shared"));
		when(collections.access("blog")).thenReturn(CollectionAccess.READ_WRITE);

		HookSystem hookSystem = mock(HookSystem.class);
		when(hookSystem.doFilter(eq(UIHooks.HOOK_REGISTER_CONTENT_TYPES), any(ContentTypes.class)))
				.thenAnswer(invocation -> {
					ContentTypes contentTypes = invocation.getArgument(1);
					contentTypes.registerCollection(new CollectionType("blog", "Blog posts", Map.of()));
					return contentTypes;
				});
		ActionFactory factory = new ActionFactory(
				context,
				siteProperties,
				hookSystem,
				mock(ModuleManager.class),
				new User("editor", "hash", new String[]{"editor"}));

		Assertions.assertThat(factory.createContentTypeMenu().getMenuEntry("collection-blog"))
				.isPresent()
				.get()
				.satisfies(entry -> {
					Assertions.assertThat(entry.getName()).isEqualTo("Blog posts");
					Assertions.assertThat(entry.getAction()).isInstanceOfSatisfying(
							UIScriptAction.class,
							action -> {
								Assertions.assertThat(action.getModule())
										.isEqualTo("/de/manager/actions/collection/create-collection-item");
								Assertions.assertThat(action.getParameters()).containsEntry("collection", "blog");
							});
				});
		Assertions.assertThat(factory.createContentTypeMenu().getMenuEntry("collection-shared")).isEmpty();
	}
}
