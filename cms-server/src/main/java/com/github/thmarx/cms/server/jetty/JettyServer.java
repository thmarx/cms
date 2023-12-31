package com.github.thmarx.cms.server.jetty;

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
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.ServerProperties;
import com.github.thmarx.cms.api.configuration.Config;
import com.github.thmarx.cms.api.configuration.Configuration;
import com.github.thmarx.cms.api.configuration.configs.ServerConfiguration;
import com.github.thmarx.cms.api.configuration.configs.SiteConfiguration;
import com.github.thmarx.cms.api.configuration.configs.TaxonomyConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.github.thmarx.cms.server.HttpServer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class JettyServer implements HttpServer {

	private final ServerProperties properties;
	private Server server;
	private Timer timer = new Timer(true);

	@Override
	public void startup() throws IOException {
		List<JettyVHost> vhosts = new ArrayList<>();
		Files.list(Path.of("hosts")).forEach((hostPath) -> {
			var props = hostPath.resolve("site.yaml");
			if (Files.exists(props)) {
				try {
					Configuration configuration = new Configuration(hostPath);
					configuration.add(ServerConfiguration.class, new ServerConfiguration(properties));
					var host = new JettyVHost(hostPath, configuration);
					host.init(Path.of(Constants.Folders.MODULES));
					vhosts.add(host);

					final List<ReloadResource> reloadableResources = new ArrayList<>();

					reloadableResources.add(
							new ReloadResource(
									props,
									Files.getLastModifiedTime(props).toMillis(),
									() -> host.reloadConfiguration(SiteConfiguration.class)
							));

					var taxo = hostPath.resolve("config/taxonomy.yaml");
					if (Files.exists(taxo)) {
						reloadableResources.add(
								new ReloadResource(
										taxo,
										Files.getLastModifiedTime(taxo).toMillis(),
										() -> host.reloadConfiguration(TaxonomyConfiguration.class)
								)
						);
						Files.list(taxo.getParent())
								.filter(path -> Constants.TAXONOMY_VALUE.matcher(path.getFileName().toString()).matches())
								.forEach(path -> {
									try {
										reloadableResources.add(
												new ReloadResource(
														path,
														Files.getLastModifiedTime(path).toMillis(),
														() -> host.reloadConfiguration(TaxonomyConfiguration.class)
												)
										);
									} catch (IOException ioe) {
										log.error(null, ioe);
									}
								});
					}

					SiteConfig siteConfig = new SiteConfig(
							reloadableResources);
					timer.schedule(siteConfig, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));
				} catch (IOException ex) {
					log.error(null, ex);
				}
			}
		});

		ContextHandlerCollection handlers = new ContextHandlerCollection();
		vhosts.forEach(host -> {
			log.debug("add virtual host : " + host.hostnames());
			var httpHandler = host.httpHandler();
			handlers.addHandler(httpHandler);
		});

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.debug("shutting down");

			vhosts.forEach(host -> {
				log.debug("shutting down vhost : " + host.hostnames());
				host.shutdown();
			});
			timer.cancel();
		}));

		HttpConfiguration httpConfig = new HttpConfiguration();
		httpConfig.setSendServerVersion(false);
		httpConfig.setSendXPoweredBy(false);
		HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());

		server = new Server(threadPool);
		server.setRequestLog(new CustomRequestLog(new Slf4jRequestLogWriter(), CustomRequestLog.EXTENDED_NCSA_FORMAT));

		ServerConnector connector = new ServerConnector(server, http11);
		connector.setPort(properties.serverPort());
		connector.setHost(properties.serverIp());

		server.addConnector(connector);

		server.setHandler(handlers);
		try {
			server.start();
		} catch (Exception ex) {
			log.error(null, ex);
		}
	}

	@Override
	public void close() throws Exception {
		server.stop();
	}

	@Slf4j
	@AllArgsConstructor
	public static class SiteConfig extends TimerTask {

		public final List<ReloadResource> resources;

		@Override
		public void run() {

			resources.forEach(resource -> {
				try {
					var tempMod = Files.getLastModifiedTime(resource.getPath()).toMillis();

					if (tempMod != resource.getLastModified()) {
						log.debug("modified: " + resource.getPath().getFileName().toString());
						resource.setLastModified(tempMod);
						resource.onChange.run();
					}
				} catch (IOException ex) {
					log.error(null, ex);
				}
			});
		}
	}

	@Data
	@AllArgsConstructor
	public static class ReloadResource {

		private final Path path;
		private long lastModified;
		private final Runnable onChange;
	}
}
