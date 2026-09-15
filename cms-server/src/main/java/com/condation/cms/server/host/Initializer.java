package com.condation.cms.server.host;

/*-
 * #%L
 * CMS Server
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

import com.condation.cms.api.configuration.Configuration;
import com.condation.cms.api.db.DB;
import com.condation.cms.core.serivce.ServiceRegistry;
import com.condation.cms.core.serivce.impl.NodeTranslationService;
import com.condation.cms.core.serivce.impl.SiteDBService;
import com.condation.cms.core.serivce.impl.SiteCollectionRepositoryService;
import com.condation.cms.core.serivce.impl.SiteContentRepositoryService;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.MutableCollectionRepository;
import com.condation.cms.api.repository.MutableContentRepository;
import com.condation.cms.core.serivce.impl.SiteLinkService;
import com.condation.cms.core.serivce.impl.SitePropertiesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author thorstenmarx
 */
@Slf4j
@RequiredArgsConstructor
public class Initializer {

	private final VHost host;

	void initServices () {
		var db = host.injector.getInstance(DB.class);
		ServiceRegistry.getInstance().register(host.id(), SiteDBService.class, new SiteDBService(db));
		ServiceRegistry.getInstance().register(host.id(), SiteCollectionRepositoryService.class,
				new SiteCollectionRepositoryService(
						host.injector.getInstance(CollectionRepository.class),
						host.injector.getInstance(MutableCollectionRepository.class)));
		ServiceRegistry.getInstance().register(host.id(), SiteContentRepositoryService.class,
				new SiteContentRepositoryService(
						host.injector.getInstance(ContentRepository.class),
						host.injector.getInstance(MutableContentRepository.class)));

		var config = host.injector.getInstance(Configuration.class);
		ServiceRegistry.getInstance().register(host.id(), SiteLinkService.class, new SiteLinkService(config));
		
		ServiceRegistry.getInstance().register(host.id(), SitePropertiesService.class, new SitePropertiesService(config));
		
		ServiceRegistry.getInstance().register(host.id(), NodeTranslationService.class,
				new NodeTranslationService(host.injector.getInstance(MutableContentRepository.class)));
	}
}
