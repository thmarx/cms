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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.annotations.Action;
import com.condation.cms.api.hooks.HookSystem;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.ui.action.UIHookAction;
import com.condation.cms.api.ui.action.UIAction;
import com.condation.cms.api.ui.action.UIScriptAction;
import com.condation.cms.api.ui.apps.AppExtensionPoint;
import com.condation.cms.api.ui.elements.Menu;
import com.condation.cms.api.ui.elements.MenuEntry;
import com.condation.modules.api.ModuleManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.condation.cms.api.ui.extensions.UIActionsExtensionPoint;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.cms.api.utils.JSONUtil;
import com.condation.cms.auth.services.AuthorizationService;
import com.condation.cms.auth.services.User;
import com.condation.cms.auth.services.RoleService;
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.repository.CollectionAccess;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

/**
 *
 * @author thorstenmarx
 */
@Slf4j
@RequiredArgsConstructor
public class ActionFactory {

	private final SiteModuleContext context;
    private final SiteProperties siteProperties;
    private final HookSystem hookSystem;
    private final ModuleManager moduleManager;
    private final User user;

    AuthorizationService authService;

	private AuthorizationService authorizationService() {
		if (authService == null) {
			if (context.has(InjectorFeature.class)) {
				authService = new AuthorizationService(context.get(InjectorFeature.class)
						.injector().getInstance(RoleService.class));
			} else {
				authService = new AuthorizationService();
			}
		}
		return authService;
	}

    public List<ShortCutHolder> createShortCuts() {
        List<ShortCutHolder> shortCuts = new ArrayList<>();
        moduleManager.extensions(UIActionsExtensionPoint.class).forEach(extension -> {
			try {
				shortCuts.addAll(scanShortCuts(extension));
			} catch (Exception exception) {
				log.error("Could not register manager shortcuts from {}",
						extension.getClass().getName(), exception);
			}
        });

        return shortCuts;
    }

	public List<AppHolder> createApps() {
		Map<String, AppHolder> apps = new LinkedHashMap<>();
		moduleManager.extensions(AppExtensionPoint.class).forEach(extension -> {
			try {
				extension.getApps().forEach(app -> registerApp(apps, app));
			} catch (Exception exception) {
				log.error("Could not register manager apps from {}",
						extension.getClass().getName(), exception);
			}
		});
		moduleManager.extensions(UIActionsExtensionPoint.class).forEach(extension -> {
			try {
				scanApps(extension).forEach(app -> registerApp(apps, app, true));
			} catch (Exception exception) {
				log.error("Could not register annotated manager apps from {}",
						extension.getClass().getName(), exception);
			}
		});

		return apps.values().stream()
				.sorted(Comparator.comparing(AppHolder::title, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	private void registerApp(Map<String, AppHolder> apps, com.condation.cms.api.ui.apps.App app) {
		registerApp(apps, app, false);
	}

	private void registerApp(
			Map<String, AppHolder> apps,
			com.condation.cms.api.ui.apps.App app,
			boolean actionHasContext) {
		if (!authorizationService().hasAllPermissions(
				user, app.permissions().toArray(String[]::new))) {
			return;
		}

		var holder = new AppHolder(
				app.id(),
				app.title(),
				HTTPUtil.modifyUrl(app.icon(), context),
				actionHasContext ? app.action() : withContext(app.action()));
		if (apps.putIfAbsent(holder.id(), holder) != null) {
			log.warn("Ignoring duplicate manager app id '{}'", holder.id());
		}
	}

	private List<com.condation.cms.api.ui.apps.App> scanApps(Object moduleInstance) {
		List<com.condation.cms.api.ui.apps.App> apps = new ArrayList<>();

		for (Method method : moduleInstance.getClass().getMethods()) {
			var appAnnotation = method.getAnnotation(com.condation.cms.api.ui.annotations.App.class);
			if (appAnnotation == null) {
				continue;
			}

			UIAction action = resolveMethodAction(method);
			if (action == null) {
				log.warn("Ignoring manager app '{}' without an action", appAnnotation.id());
				continue;
			}

			apps.add(new com.condation.cms.api.ui.apps.App(
					appAnnotation.id(),
					appAnnotation.title(),
					appAnnotation.icon(),
					action,
					Arrays.asList(appAnnotation.permissions())));
		}

		return apps;
	}

    public Menu createContentTypeMenu() {
        UIHooks uiHooks = new UIHooks(hookSystem);
        var contentTypes = uiHooks.contentTypes();

        Menu menu = new Menu();

        contentTypes.getPageTemplates().stream().map(pt -> {
            return MenuEntry.builder()
                    .id(pt.name())
                    .name(pt.name())
                    .action(new UIScriptAction(HTTPUtil.prependContext("/manager/actions/page/create-node", siteProperties),
                            Map.of(
                                    "name", pt.name(),
                                    "folder", pt.contentFolder(),
                                    "template", pt.template(),
									"contentType", pt.name()
                            )
                    ))
                    .children(new ArrayList<>())
                    .build();
        }).forEach(menu::addMenuEntry);

		var collections = context.get(RepositoryFeature.class).collectionRepository();
		collections.names().stream()
				.filter(name -> collections.access(name) == CollectionAccess.READ_WRITE)
				.sorted()
				.map(name -> MenuEntry.builder()
						.id("collection-" + name)
						.name(contentTypes.getCollection(name)
								.map(collection -> collection.label())
								.orElse(name))
						.action(new UIScriptAction(
								HTTPUtil.prependContext(
										"/manager/actions/collection/create-collection-item",
										siteProperties),
								Map.of("collection", name)))
						.children(new ArrayList<>())
						.build())
				.forEach(menu::addMenuEntry);

        return menu;
    }

    public Menu createMenu() {
        UIHooks uiHooks = new UIHooks(hookSystem);
        var menu = uiHooks.menu();

        moduleManager.extensions(UIActionsExtensionPoint.class).forEach(extension -> {
            try {
                extension.addMenuItems(menu);
            } catch (Exception e) {
                log.error("", e);
            }
        });

        List<EntryHolder> entries = new ArrayList<>();
        moduleManager.extensions(UIActionsExtensionPoint.class).forEach(extension -> {
            try {
                entries.addAll(scanMenuEntries(extension));
            } catch (Exception e) {
                log.error("", e);
            }
        });

        insertEntriesIntoMenu(menu, entries);

        var filteredMenu = new Menu();
        var menuEntries = menu.entries();
        menuEntries.stream()
                .filter(entry -> authorizationService().hasAllPermissions(user, entry.getPermissions().toArray(new String[0])))
                .forEach(filteredMenu::addMenuEntry);

        return filteredMenu;
    }

    private List<ShortCutHolder> scanShortCuts(Object moduleInstance) {
        List<ShortCutHolder> shortCuts = new ArrayList<>();

        for (Method method : moduleInstance.getClass().getMethods()) {
            var shortcutAnnotation = method.getAnnotation(com.condation.cms.api.ui.annotations.ShortCut.class);
            if (shortcutAnnotation == null) {
                continue;
            }

			var appAnnotation = method.getAnnotation(com.condation.cms.api.ui.annotations.App.class);
			String id = fallback(shortcutAnnotation.id(), appAnnotation == null ? "" : appAnnotation.id());
			String title = fallback(shortcutAnnotation.title(), appAnnotation == null ? "" : appAnnotation.title());
			String icon = fallback(shortcutAnnotation.icon(), appAnnotation == null ? "" : appAnnotation.icon());
			String[] permissions = shortcutAnnotation.permissions().length > 0
					? shortcutAnnotation.permissions()
					: appAnnotation == null ? new String[0] : appAnnotation.permissions();

			if (id.isBlank() || title.isBlank()) {
				log.warn("Ignoring manager shortcut on {} without id or title", method);
				continue;
			}
			if (!authorizationService().hasAllPermissions(user, permissions)) {
				continue;
			}

			UIAction menuAction = resolveShortcutAction(method, shortcutAnnotation);
			if (menuAction != null) {
                shortCuts.add(new ShortCutHolder(
						id,
						title,
						icon.isBlank() ? "" : HTTPUtil.modifyUrl(icon, context),
                        shortcutAnnotation.hotkey(),
                        shortcutAnnotation.parent(),
                        shortcutAnnotation.section(),
                        menuAction,
						permissions));
            }

        }

        return shortCuts;
    }

    private List<EntryHolder> scanMenuEntries(Object moduleInstance) {

        List<EntryHolder> entries = new ArrayList<>();

        for (Method method : moduleInstance.getClass().getMethods()) {
            var menuAnn = method.getAnnotation(com.condation.cms.api.ui.annotations.MenuEntry.class);
            if (menuAnn == null) {
                continue;
            }

			UIAction menuAction = resolveMenuAction(method, menuAnn);

            var entry = MenuEntry.builder()
                    .id(menuAnn.id())
                    .name(menuAnn.name())
                    .divider(menuAnn.divider())
                    .position(menuAnn.position())
                    .action(menuAction)
                    .children(new ArrayList<>())
                    .permissions(Arrays.asList(menuAnn.permissions()))
                    .build();

            entries.add(new EntryHolder(menuAnn.parent(), entry));
        }

        return entries;
    }

    private void insertEntriesIntoMenu(Menu menu, List<EntryHolder> entries) {
        Map<String, MenuEntry> index = new HashMap<>();
        entries.forEach(holder -> index.put(holder.entry().getId(), holder.entry()));

        // füge alle mit parent == "" oder null in die Wurzel ein
        for (EntryHolder holder : entries) {
            String parentId = holder.parent();
            MenuEntry entry = holder.entry();

            if (parentId == null || parentId.isBlank()) {
                menu.addMenuEntry(entry);
            } else {
                // Versuche Parent in fertigem Menü zu finden
                Optional<MenuEntry> parentInMenu = findEntryById(menu, parentId);

                if (parentInMenu.isEmpty()) {
                    // Versuche in den noch nicht eingefügten Entries
                    MenuEntry parentInBatch = index.get(parentId);
                    if (parentInBatch != null) {
                        parentInBatch.getChildren().add(entry);
                    } else {
                        log.warn("Parent entry with ID '" + parentId + "' not found for menu entry '" + entry.getId() + "'");
                    }
                } else {
                    parentInMenu.get().addChildren(entry);
                }
            }
        }

        // Jetzt alle "Wurzel"-Einträge, die nicht direkt im Menü sind, einfügen
        for (EntryHolder holder : entries) {
            String parentId = holder.parent();
            if (parentId == null || parentId.isBlank()) {
                menu.addMenuEntry(holder.entry());
            }
        }
    }

    private Optional<MenuEntry> findEntryById(Menu menu, String id) {
        if (menu.entries() == null) {
            return Optional.empty();
        }
        for (MenuEntry entry : menu.entries()) {
            Optional<MenuEntry> result = findEntryByIdRecursive(entry, id);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private Optional<MenuEntry> findEntryByIdRecursive(MenuEntry entry, String id) {
        if (entry.getId().equals(id)) {
            return Optional.of(entry);
        }
        if (entry.getChildren() == null) {
            return Optional.empty();
        }
        for (MenuEntry child : entry.getChildren()) {
            Optional<MenuEntry> result = findEntryByIdRecursive(child, id);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

	private record EntryHolder(String parent, MenuEntry entry) {

	}

	private UIAction resolveMethodAction(Method method) {
		Action actionAnnotation = method.getAnnotation(Action.class);
		if (actionAnnotation != null) {
			return new UIHookAction(actionAnnotation.value(), Map.of());
		}

		var hookAction = method.getAnnotation(com.condation.cms.api.ui.annotations.HookAction.class);
		if (hookAction != null && !hookAction.value().isBlank()) {
			return new UIHookAction(hookAction.value(), Map.of());
		}

		var scriptAction = method.getAnnotation(com.condation.cms.api.ui.annotations.ScriptAction.class);
		if (scriptAction != null && !scriptAction.module().isBlank()) {
			return scriptAction(scriptAction.module(), scriptAction.function(), Map.of());
		}

		return null;
	}

	private UIAction resolveShortcutAction(
			Method method,
			com.condation.cms.api.ui.annotations.ShortCut shortcutAnnotation) {
		UIAction action = resolveMethodAction(method);
		if (action != null) {
			return action;
		}
		if (!shortcutAnnotation.hookAction().value().isBlank()) {
			return new UIHookAction(shortcutAnnotation.hookAction().value(), Map.of());
		}
		if (!shortcutAnnotation.scriptAction().module().isBlank()) {
			return scriptAction(
					shortcutAnnotation.scriptAction().module(),
					shortcutAnnotation.scriptAction().function(),
					Map.of());
		}

		var menuAnnotation = method.getAnnotation(com.condation.cms.api.ui.annotations.MenuEntry.class);
		return menuAnnotation == null ? null : resolveMenuAction(method, menuAnnotation);
	}

	private UIAction resolveMenuAction(
			Method method,
			com.condation.cms.api.ui.annotations.MenuEntry menuAnnotation) {
		UIAction action = resolveMethodAction(method);
		if (action != null) {
			return action;
		}
		if (!menuAnnotation.hookAction().value().isBlank()) {
			return new UIHookAction(menuAnnotation.hookAction().value(), Map.of());
		}
		if (!menuAnnotation.scriptAction().module().isBlank()) {
			return scriptAction(
					menuAnnotation.scriptAction().module(),
					menuAnnotation.scriptAction().function(),
					Map.of());
		}
		return null;
	}

	private String fallback(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private UIScriptAction scriptAction(String module, String function, Map<String, Object> parameters) {
		return new UIScriptAction(HTTPUtil.modifyUrl(module, context), function, parameters);
	}

	private UIAction withContext(UIAction action) {
		if (action instanceof UIScriptAction scriptAction) {
			return scriptAction(
					scriptAction.getModule(),
					scriptAction.getFunction(),
					scriptAction.getParameters());
		}
		return action;
	}

	public record ShortCutHolder(String id, String title, String icon, String hotkey, String parent, String section, UIAction action, String[] permissions) {

		public String getActionDefinition() {
			return action != null ? JSONUtil.toJson(action) : "";
		}

		public String getIdDefinition() {
			return JSONUtil.toJson(id);
		}

		public String getTitleDefinition() {
			return JSONUtil.toJson(title);
		}

		public String getIconDefinition() {
			return JSONUtil.toJson(icon);
		}

		public String getHotkeyDefinition() {
			return JSONUtil.toJson(hotkey);
		}

		public String getSectionDefinition() {
			return JSONUtil.toJson(section);
		}
	}

	public record AppHolder(String id, String title, String icon, UIAction action) {

		public String getActionDefinition() {
			return JSONUtil.toJson(action);
		}
	}
;
}
