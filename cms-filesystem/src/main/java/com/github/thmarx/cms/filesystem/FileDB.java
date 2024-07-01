package com.github.thmarx.cms.filesystem;

/*-
 * #%L
 * cms-filesystem
 * %%
 * Copyright (C) 2023 Marx-Software
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.github.thmarx.cms.api.configuration.Configuration;
import com.github.thmarx.cms.api.db.Content;
import com.github.thmarx.cms.api.db.DB;
import com.github.thmarx.cms.api.db.DBFileSystem;
import com.github.thmarx.cms.api.db.taxonomy.Taxonomies;
import com.github.thmarx.cms.api.eventbus.EventBus;
import com.github.thmarx.cms.filesystem.taxonomy.FileTaxonomies;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

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
	
	private FileTaxonomies taxonomies;
	
	public void init () throws IOException {
		init(MetaData.Type.MEMORY);
	}
	
	public void init (MetaData.Type metaDataType) throws IOException {
		fileSystem = new FileSystem(hostBaseDirectory, eventBus, contentParser);
		fileSystem.init(metaDataType);
		
		content = new FileContent(fileSystem);
		
		taxonomies = new FileTaxonomies(configuration, fileSystem);
	}
		
	@Override
	public DBFileSystem getFileSystem() {
		return fileSystem;
	}

	@Override
	public void close() throws Exception {
		fileSystem.shutdown();
	}

	@Override
	public Content getContent() {
		return content;
	}

	@Override
	public Taxonomies getTaxonomies() {
		return taxonomies;
	}
	
}
