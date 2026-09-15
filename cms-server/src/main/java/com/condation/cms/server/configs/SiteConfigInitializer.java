package com.condation.cms.server.configs;

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

import com.condation.cms.api.ServerProperties;
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.cache.CacheManager;
import com.condation.cms.api.configuration.Configuration;
import com.condation.cms.api.eventbus.EventBus;
import com.condation.cms.api.feature.features.CacheManagerFeature;
import com.condation.cms.api.feature.features.ConfigurationFeature;
import com.condation.cms.api.feature.features.ContentRenderFeature;
import com.condation.cms.api.feature.features.CronJobSchedulerFeature;
import com.condation.cms.api.feature.features.DBFeature;
import com.condation.cms.api.feature.features.EventBusFeature;
import com.condation.cms.api.feature.features.InjectorFeature;
import com.condation.cms.api.feature.features.MessagingFeature;
import com.condation.cms.api.feature.features.MutableRepositoryFeature;
import com.condation.cms.api.feature.features.RepositoryFeature;
import com.condation.cms.api.feature.features.ServerPropertiesFeature;
import com.condation.cms.api.feature.features.SitePropertiesFeature;
import com.condation.cms.api.feature.features.ThemeFeature;
import com.condation.cms.api.feature.features.WorkflowFeature;
import com.condation.cms.api.workflow.WFStatusProvider;
import com.condation.cms.api.messaging.Messaging;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.MutableCollectionRepository;
import com.condation.cms.api.repository.MutableContentRepository;
import com.condation.cms.api.scheduler.CronJobContext;
import com.condation.cms.api.theme.Theme;
import com.condation.cms.api.workflow.Workflow;
import com.condation.cms.content.ContentResolver;
import com.condation.cms.core.scheduler.SiteCronJobScheduler;
import com.condation.cms.filesystem.FileDB;
import com.condation.cms.module.DefaultRenderContentFunction;
import com.condation.cms.request.RequestContextFactory;
import com.google.inject.Injector;

import lombok.RequiredArgsConstructor;

/**
 * This is an initializer for some site configurations that make problem due to circular dependencies in guice.
 * 
 */
@RequiredArgsConstructor
public class SiteConfigInitializer {

    final Injector injector;

    public void init () {
        initModuleContext();
        initCronJobContext();
    }

    private void initCronJobContext() {
        var context = injector.getInstance(CronJobContext.class);

        context.add(SitePropertiesFeature.class, new SitePropertiesFeature(injector.getInstance(SiteProperties.class)));
		context.add(ServerPropertiesFeature.class, new ServerPropertiesFeature(injector.getInstance(ServerProperties.class)));
		context.add(DBFeature.class, new DBFeature(injector.getInstance(FileDB.class)));
		context.add(EventBusFeature.class, new EventBusFeature(injector.getInstance(EventBus.class)));
		context.add(MessagingFeature.class, new MessagingFeature(injector.getInstance(Messaging.class)));
		context.add(ThemeFeature.class, new ThemeFeature(injector.getInstance(Theme.class)));
		context.add(ConfigurationFeature.class, new ConfigurationFeature(injector.getInstance(Configuration.class)));
		context.add(WorkflowFeature.class, new WorkflowFeature(injector.getInstance(Workflow.class)));
		context.add(RepositoryFeature.class, new RepositoryFeature(
				injector.getInstance(ContentRepository.class),
				injector.getInstance(CollectionRepository.class)));
		context.add(MutableRepositoryFeature.class, new MutableRepositoryFeature(
				injector.getInstance(MutableContentRepository.class),
				injector.getInstance(MutableCollectionRepository.class)));
    }

    private void initModuleContext () {
        var cmsModuleContext = injector.getInstance(SiteModuleContext.class);

        cmsModuleContext.add(SitePropertiesFeature.class, new SitePropertiesFeature(injector.getInstance(SiteProperties.class)));
		cmsModuleContext.add(ServerPropertiesFeature.class, new ServerPropertiesFeature(injector.getInstance(ServerProperties.class)));
		cmsModuleContext.add(DBFeature.class, new DBFeature(injector.getInstance(FileDB.class)));
		cmsModuleContext.add(EventBusFeature.class, new EventBusFeature(injector.getInstance(EventBus.class)));
		cmsModuleContext.add(MessagingFeature.class, new MessagingFeature(injector.getInstance(Messaging.class)));
		cmsModuleContext.add(ThemeFeature.class, new ThemeFeature(injector.getInstance(Theme.class)));
		cmsModuleContext.add(ConfigurationFeature.class, new ConfigurationFeature(injector.getInstance(Configuration.class)));
		cmsModuleContext.add(WorkflowFeature.class, new WorkflowFeature(injector.getInstance(Workflow.class)));
		cmsModuleContext.add(RepositoryFeature.class, new RepositoryFeature(
				injector.getInstance(ContentRepository.class),
				injector.getInstance(CollectionRepository.class)));
		cmsModuleContext.add(MutableRepositoryFeature.class, new MutableRepositoryFeature(
				injector.getInstance(MutableContentRepository.class),
				injector.getInstance(MutableCollectionRepository.class)));
		cmsModuleContext.add(CronJobSchedulerFeature.class, new CronJobSchedulerFeature(injector.getInstance(SiteCronJobScheduler.class)));
		cmsModuleContext.add(CacheManagerFeature.class, new CacheManagerFeature(injector.getInstance(CacheManager.class)));
		cmsModuleContext.add(InjectorFeature.class, new InjectorFeature(injector));

        var contentResolver = injector.getInstance(ContentResolver.class);
		var requestContextFactory = injector.getInstance(RequestContextFactory.class);

		cmsModuleContext.add(
				ContentRenderFeature.class,
				new ContentRenderFeature(new DefaultRenderContentFunction(contentResolver, requestContextFactory))
		);
    }
}
