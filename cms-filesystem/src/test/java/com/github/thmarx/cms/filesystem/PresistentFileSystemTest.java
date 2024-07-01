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
import com.github.thmarx.cms.api.eventbus.EventBus;
import com.github.thmarx.cms.api.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author t.marx
 */
public class PresistentFileSystemTest extends AbstractFileSystemTest {

	static FileSystem fileSystem;
	
	
	@BeforeAll
	static void setup() throws IOException {
		
		var eventBus = Mockito.mock(EventBus.class);
		
		fileSystem = new FileSystem(Path.of("src/test/resources"), eventBus, (file) -> {
			try {
				return new Yaml().load(Files.readString(file));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		fileSystem.init(MetaData.Type.PERSISTENT);
	}
	
	@AfterAll
	static void shutdown () throws IOException {
		fileSystem.shutdown();
		
		if (Files.exists(Path.of("src/test/resources/data"))) {
			FileUtils.deleteFolder(Path.of("src/test/resources/data"));
		}
	}

	@Override
	protected FileSystem getFileSystem() {
		return fileSystem;
	}
}
