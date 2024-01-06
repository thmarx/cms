package com.github.thmarx.cms.git;

/*-
 * #%L
 * cms-git
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

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;

/**
 *
 * @author t.marx
 */
public class GitSchedulerTest {
	
	static GitScheduler scheduler;
	static TaskRunner runner = new TaskRunner();
	
	@BeforeAll
	static void setup () throws Exception {
		scheduler = new GitScheduler(runner);
		scheduler.open();
	}
	@AfterAll
	static void shutdown () throws Exception {
		scheduler.close();
		runner.executor.shutdown();
	}

	@Test
	public void testSomeMethod() throws IOException, SchedulerException, InterruptedException {
		var config = Config.load(Path.of("git.yaml"));
		scheduler.schedule(config.getRepos().get(0));
		Thread.sleep(Duration.ofSeconds(15));
	}
	
}
