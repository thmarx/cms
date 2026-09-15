package com.condation.cms.modules.system.api.services;

/*-
 * #%L
 * CMS Api Module
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
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.configuration.Configuration;
import com.condation.cms.api.configuration.configs.SiteConfiguration;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.feature.features.ConfigurationFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.content.DefaultContentParser;
import com.condation.cms.core.eventbus.DefaultEventBus;
import com.condation.cms.filesystem.FileDB;
import com.condation.cms.filesystem.FileSystemContentRepository;
import com.condation.cms.filesystem.FileSystemContentStore;
import com.condation.cms.filesystem.NIOReadOnlyFile;
import java.nio.file.Path;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author thorstenmarx
 */
@ExtendWith(MockitoExtension.class)
public class ContentServiceTest {
	
	private static FileDB db;
	private static ContentService contentService;

	@Mock
	private Request request;
	
	@BeforeAll
	public static void setup() throws Exception {

		var contentParser = new DefaultContentParser();
		var hostBase = Path.of("src/test/resources/site");
		var config = new Configuration();
		var siteConfigMock = Mockito.mock(SiteConfiguration.class);
		var sitePropsMock = Mockito.mock(SiteProperties.class);
		Mockito.when(sitePropsMock.id()).thenReturn("test-site");
		Mockito.when(siteConfigMock.siteProperties()).thenReturn(sitePropsMock);
		config.add(SiteConfiguration.class, siteConfigMock);
		db = new FileDB(hostBase, new DefaultEventBus(), (file) -> {
			try {
				ReadOnlyFile cmsFile = new NIOReadOnlyFile(file, hostBase.resolve(Constants.Folders.CONTENT));
				return contentParser.parseMeta(cmsFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, config);
		db.init();
		
		var repository = new FileSystemContentRepository(
				db.getContent(), db.getFileSystem(),
				new FileSystemContentStore(db.getFileSystem()), contentParser);
		contentService = new ContentService(repository, Set.of("title"));
	}
	
	@BeforeEach
	public void setupRequestContext () {
		var requestContext = new RequestContext();
		var siteProperties = Mockito.mock(SiteProperties.class);
		Mockito.lenient().when(siteProperties.contextPath()).thenReturn("/");
		var siteConfiguration = new SiteConfiguration(siteProperties);
		
		var configuration = new Configuration();
		configuration.add(SiteConfiguration.class, siteConfiguration);
		ConfigurationFeature configFeature = new ConfigurationFeature(configuration);
		
		requestContext.add(ConfigurationFeature.class, configFeature);
		
		Mockito.lenient().when(request.getAttribute("_requestContext")).thenReturn(requestContext);
	}
	
	@AfterAll
	public static void shutdown () throws Exception {
		db.close();
	}

	@Test
	public void publised_content_is_returned() {
		var content = contentService.resolve("", request);
		
		Assertions.assertThat(content).isPresent();
	}
	
	@Test
	public void unpublised_content_is_not_returned() {
		var content = contentService.resolve("sub", request);
		
		Assertions.assertThat(content).isEmpty();
	}
}
