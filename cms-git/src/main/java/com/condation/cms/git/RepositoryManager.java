package com.condation.cms.git;

/*-
 * #%L
 * cms-git
 * %%
 * Copyright (C) 2023 - 2024 CondationCMS
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

import com.condation.cms.git.tasks.CloneTask;
import java.io.IOException;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class RepositoryManager {

	private final Scheduler scheduler;
	
	Config config;
	TaskRunner taskRunner;

	GitScheduler gitScheduler;

	public void init(final Path configFile) throws IOException {
		config = Config.load(configFile);
		taskRunner = new TaskRunner();
		gitScheduler = new GitScheduler(scheduler, taskRunner);

		if (config.getRepos() != null) {
			log.debug("initial clone repositories");
			for (var repo : config.getRepos()) {
				log.debug("clone {}", repo.getName());
				var result = taskRunner.execute(new CloneTask(repo));
				try {
					log.debug("result : {} ", result.get());
					log.debug("schedule repo");
					gitScheduler.schedule(repo);
				} catch (Exception ex) {
					log.error("error cloneing repository", ex);
				}
			}

		}
	}

	public void close() throws IOException {
		taskRunner.executor.shutdown();
	}

}
