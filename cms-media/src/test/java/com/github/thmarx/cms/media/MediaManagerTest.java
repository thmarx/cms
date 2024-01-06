package com.github.thmarx.cms.media;

/*-
 * #%L
 * cms-media
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

import com.github.thmarx.cms.api.PropertiesLoader;
import com.github.thmarx.cms.api.ThemeProperties;
import com.github.thmarx.cms.api.configuration.Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author t.marx
 */
public class MediaManagerTest {
	
	static MediaManager mediaManager;
	
	@BeforeAll
	public static void setup () throws IOException {
		
		Configuration config = new Configuration(Path.of("src/test/resources"));
		
		mediaManager = new MediaManager(
				Path.of("src/test/resources/assets"), 
				Path.of("target/"), 
				new TestTheme(new ThemeProperties(PropertiesLoader.rawProperties(Path.of("src/test/resources/theme.yaml")))), 
				config
		);
	}

	@Test
	public void testSomeMethod() throws IOException {
		var content = mediaManager.getScaledContent("test.jpg", mediaManager.getMediaFormat("cropped"));
		
		Files.write(Path.of("target/test_cropped.jpg"), content.get());
	}
	
}
