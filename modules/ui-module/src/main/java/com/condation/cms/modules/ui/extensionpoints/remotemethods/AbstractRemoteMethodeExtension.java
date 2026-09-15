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

import com.condation.cms.api.Constants;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.DBFileSystem;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.extensions.AbstractExtensionPoint;
import com.condation.cms.api.feature.features.AuthFeature;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.HookSystemFeature;
import com.condation.cms.api.feature.features.MutableRepositoryFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.repository.MutableCollectionRepository;
import com.condation.cms.api.repository.MutableContentRepository;
import com.condation.cms.api.ui.extensions.UIRemoteMethodExtensionPoint;
import com.condation.cms.core.serivce.ServiceRegistry;
import com.condation.cms.core.serivce.impl.SiteCollectionRepositoryService;
import com.condation.cms.core.serivce.impl.SiteContentRepositoryService;
import com.condation.cms.core.serivce.impl.SiteDBService;
import com.condation.cms.modules.ui.utils.UIHooks;
import java.nio.file.Path;
import java.util.Map;

/**
 *
 * @author thorstenmarx
 */
public abstract class AbstractRemoteMethodeExtension extends AbstractExtensionPoint implements UIRemoteMethodExtensionPoint {
	
	private static final String SITE_ID = "siteId";
	private static final String ASSETS = "assets";
	protected static final String CONTENT = "content";
	
	protected String getUserName() {
		if (getRequestContext().has(AuthFeature.class)) {
			return getRequestContext().get(AuthFeature.class).username();
		}
		return "";
	}
	
	protected UIHooks uiHooks() {
		return new UIHooks(getRequestContext().get(HookSystemFeature.class).hookSystem());
	}
	
	protected DB getDB (Map<String, Object> parameters) {
		if (parameters.containsKey(SITE_ID)) {
			return ServiceRegistry.getInstance().get((String)parameters.get(SITE_ID), 
					SiteDBService.class).get().db();
		} else {
			return getContext().get(DBFeature.class).db();
		}
	}

	protected ContentRepository getContentRepository(Map<String, Object> parameters) {
		if (parameters.containsKey(SITE_ID)) {
			return siteContentRepositoryService(parameters).repository();
		}
		return getContext().get(RepositoryFeature.class).contentRepository();
	}

	protected MutableContentRepository getMutableContentRepository(Map<String, Object> parameters) {
		if (parameters.containsKey(SITE_ID)) {
			return siteContentRepositoryService(parameters).mutableRepository();
		}
		return getContext().get(MutableRepositoryFeature.class).contentRepository();
	}

	protected CollectionRepository getCollectionRepository(Map<String, Object> parameters) {
		if (parameters.containsKey(SITE_ID)) {
			return siteCollectionRepositoryService(parameters).repository();
		}
		return getContext().get(RepositoryFeature.class).collectionRepository();
	}

	protected MutableCollectionRepository getMutableCollectionRepository(Map<String, Object> parameters) {
		if (parameters.containsKey(SITE_ID)) {
			return siteCollectionRepositoryService(parameters).mutableRepository();
		}
		return getContext().get(MutableRepositoryFeature.class).collectionRepository();
	}

	private SiteContentRepositoryService siteContentRepositoryService(Map<String, Object> parameters) {
		return ServiceRegistry.getInstance()
				.get((String) parameters.get(SITE_ID), SiteContentRepositoryService.class)
				.orElseThrow(() -> new IllegalArgumentException(
						"unknown site: " + parameters.get(SITE_ID)));
	}

	private SiteCollectionRepositoryService siteCollectionRepositoryService(Map<String, Object> parameters) {
		return ServiceRegistry.getInstance()
				.get((String) parameters.get(SITE_ID), SiteCollectionRepositoryService.class)
				.orElseThrow(() -> new IllegalArgumentException(
						"unknown site: " + parameters.get(SITE_ID)));
	}
	
    
    public static ReadOnlyFile getBase(DBFileSystem fileSystem, String type) {
		validateFileType(type);
		return switch (type) {
			case CONTENT ->
				fileSystem.contentBase();
			case ASSETS ->
				fileSystem.assetBase();
			default ->
				throw new IllegalArgumentException("Unsupported file type: " + type);
		};
	}

	public static Path getWritableBase(DBFileSystem fileSystem, String type) {
		validateFileType(type);
		return switch (type) {
			case CONTENT ->
				fileSystem.resolve(Constants.Folders.CONTENT);
			case ASSETS ->
				fileSystem.resolve(Constants.Folders.ASSETS);
			default ->
				throw new IllegalArgumentException("Unsupported file type: " + type);
		};
	}

	private static void validateFileType(String type) {
		if (!CONTENT.equals(type) && !ASSETS.equals(type)) {
			throw new IllegalArgumentException("Unsupported file type: " + type);
		}
	}
}
