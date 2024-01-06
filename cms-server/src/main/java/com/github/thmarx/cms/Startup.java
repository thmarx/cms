package com.github.thmarx.cms;

/*-
 * #%L
 * cms-server
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
import com.github.thmarx.cms.api.ServerContext;
import com.github.thmarx.cms.api.ServerProperties;
import com.github.thmarx.cms.git.RepositoryManager;
import com.github.thmarx.cms.server.jetty.JettyServer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public class Startup {

	public static void main(String[] args) throws Exception {

		printStartup();
		
		System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
		System.setProperty("polyglotimpl.DisableClassPathIsolation", "true");

		ServerProperties properties = PropertiesLoader.serverProperties(Path.of("server.yaml"));
		
		ServerContext.IS_DEV = properties.dev();

		initGitRepositoryManager();
		
		var server = new JettyServer(properties);
		server.startup();
	}
	
	private static void initGitRepositoryManager () throws IOException {
		Path gitConfig = Path.of("git.yaml");
		if (!Files.exists(gitConfig)) {
			log.info("no repository configuration found");
			return;
		}
		log.info("repository configuration found");
		final RepositoryManager repositoryManager = new RepositoryManager();
		repositoryManager.init(gitConfig);
		
		Runtime.getRuntime().addShutdownHook(new Thread (() -> {
			try {
				repositoryManager.close();
			} catch (IOException ex) {
				log.error("error closing repo manager", ex);
			}
		}));
	}
	
	private static void printStartup () throws IOException {
		try (var in = Startup.class.getResourceAsStream("application.properties")) {
			Properties props = new Properties();
			props.load(in);
			
			log.info("starting {} version {}", props.getProperty("name"), props.getProperty("version"));
			log.info("build {}", props.getProperty("build.date"));
		}
	}

}
