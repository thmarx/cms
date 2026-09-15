package com.condation.cms.filesystem;

/*-
 * #%L
 * CMS FileSystem
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
import com.condation.cms.api.configuration.configs.SiteConfiguration;
import com.condation.cms.api.db.Content;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.DBFileSystem;
import com.condation.cms.api.db.taxonomy.Taxonomies;
import com.condation.cms.api.eventbus.EventBus;
import com.condation.cms.api.repository.MutableCollectionRepository;
import com.condation.cms.filesystem.taxonomy.FileTaxonomies;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import com.condation.cms.api.db.cms.ReadOnlyFileSystem;

/**
 *
 * @author thmar
 */
@RequiredArgsConstructor
public class FileDB implements DB {

	private final Path hostBaseDirectory;
	private final EventBus eventBus;
	final Function<Path, Map<String, Object>> contentParser;
	final Configuration configuration;
	
	private FileSystem fileSystem;
	private FileContent content;
	private FileSystemCollectionRepository localCollectionRepository;
	private MutableCollectionRepository collectionRepository;
	private ReadOnlyFileSystem readOnlyFileSystem;
	
	private FileTaxonomies taxonomies;
	
	
	public void init () throws IOException {
		var siteProperties = configuration.get(SiteConfiguration.class).siteProperties();
		fileSystem = new FileSystem(
				siteProperties.id(),
				hostBaseDirectory,
				eventBus,
				contentParser,
				indexFields(siteProperties.get("index.fields")));
		fileSystem.init();
		readOnlyFileSystem = new WrappedReadOnlyFileSystem(fileSystem);
		
		content = new FileContent(fileSystem);
		localCollectionRepository = new FileSystemCollectionRepository(
				siteProperties.id(), hostBaseDirectory, contentParser);
		localCollectionRepository.init();
		var collectionConfiguration = configuration.get(
				com.condation.cms.api.configuration.configs.CollectionConfiguration.class);
		collectionRepository = collectionConfiguration == null
				? localCollectionRepository
				: new ReferencedCollectionRepository(
						siteProperties.id(), localCollectionRepository, collectionConfiguration);
		
		taxonomies = new FileTaxonomies(configuration, content);	
	}

	private static Map<String, ?> indexFields(Object value) {
		if (!(value instanceof Map<?, ?> fields)) {
			return Map.of();
		}

		return fields.entrySet().stream()
				.filter(entry -> entry.getKey() instanceof String)
				.collect(java.util.stream.Collectors.toUnmodifiableMap(
						entry -> (String) entry.getKey(),
						Map.Entry::getValue));
	}

	public void reindex() {
		var siteProperties = configuration.get(SiteConfiguration.class).siteProperties();
		fileSystem.reindex(indexFields(siteProperties.get("index.fields")));
		localCollectionRepository.reindex();
	}

	@Deprecated
	@Override
	public ReadOnlyFileSystem getReadOnlyFileSystem() {
		return readOnlyFileSystem;
	}

	@Override
	public DBFileSystem getFileSystem() {
		return fileSystem;
	}

	@Override
	public void close() throws Exception {
		try {
			if (localCollectionRepository != null) {
				localCollectionRepository.close();
			}
		} finally {
			fileSystem.shutdown();
		}
	}

	@Override
	public Content getContent() {
		return content;
	}

	@Override
	public MutableCollectionRepository getCollectionRepository() {
		return collectionRepository;
	}

	@Override
	public Taxonomies getTaxonomies() {
		return taxonomies;
	}
	
}
