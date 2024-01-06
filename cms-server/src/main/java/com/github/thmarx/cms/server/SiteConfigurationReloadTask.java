package com.github.thmarx.cms.server;

/*-
 * #%L
 * cms-server
 * %%
 * Copyright (C) 2023 - 2024 Marx-Software
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

import com.github.thmarx.cms.api.configuration.Config;
import com.github.thmarx.cms.api.eventbus.EventBus;
import com.github.thmarx.cms.api.eventbus.events.ConfigurationFileChanged;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public class SiteConfigurationReloadTask extends TimerTask {
	
	@Inject
	private EventBus eventBus; 

	private List<ConfigurationResource> configurations = new ArrayList<>();
	
	public void addConfiguration (Path configFile, Class<? extends Config> configClass) throws IOException {
		if (!Files.exists(configFile)) {
			return;
		}
		configurations.add(
				new ConfigurationResource(configFile, configClass, Files.getLastModifiedTime(configFile).toMillis())
		);
	}

	@Override
	public void run() {
		configurations.forEach(config -> {
				try {
					var tempMod = Files.getLastModifiedTime(config.configFile).toMillis();

					if (tempMod != config.lastModified) {
						log.debug("modified: " + config.configFile.getFileName().toString());
						config.setLastModified(tempMod);
						
						eventBus.publish(new ConfigurationFileChanged(config.configClass));
					}
				} catch (IOException ex) {
					log.error(null, ex);
				}
			});
	}
	
	@AllArgsConstructor
	@Data
	public static class ConfigurationResource {
		Path configFile;
		Class<? extends Config> configClass;
		long lastModified;
	}
}
