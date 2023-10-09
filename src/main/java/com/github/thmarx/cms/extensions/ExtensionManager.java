/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.github.thmarx.cms.extensions;

import com.github.thmarx.cms.filesystem.FileSystem;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateMethodModelEx;
import io.undertow.server.HttpHandler;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.IOAccess;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class ExtensionManager implements AutoCloseable {

	private final FileSystem fileSystem;

	private Engine engine;

	@Getter
	private final List<HttpHandlerExtension> httpHandlerExtensions = new ArrayList<>();
	@Getter
	private final List<TemplateMethodExtension> templateMethodExtensions = new ArrayList<>();
	@Getter
	private final List<TemplateDirectiveExtension> templateDirectiveExtensions = new ArrayList<>();
	private Context context;

	public void registerHttpExtension(final String path, final HttpHandler handler) {
		httpHandlerExtensions.add(new HttpHandlerExtension(path, handler));
	}

	public void registerTemplateDirectiveExtensions(final String path, final TemplateDirectiveModel directive) {
		templateDirectiveExtensions.add(new TemplateDirectiveExtension(path, directive));
	}

	public void registerTemplateMethodExtensions(final String path, final TemplateMethodModelEx method) {
		templateMethodExtensions.add(new TemplateMethodExtension(path, method));
	}

	private ClassLoader getClassLoader() throws IOException {
		Path libs = fileSystem.resolve("libs/");
		List<URL> urls = new ArrayList<>();
		if (Files.exists(libs)) {
			Files.list(libs)
					.filter(path -> path.getFileName().toString().endsWith(".jar"))
					.forEach(path -> {
						try {
							urls.add(path.toUri().toURL());
						} catch (MalformedURLException ex) {
							log.error("", ex);
						}
					});
		}
		return new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getSystemClassLoader());
	}

	public void init() throws IOException {
		if (engine == null) {
			engine = Engine.newBuilder("js")
					.option("engine.WarnInterpreterOnly", "false")
					.build();
			context = Context.newBuilder()
					.allowAllAccess(true)
					.allowHostClassLookup(className -> true)
					.allowHostAccess(HostAccess.ALL)
					.hostClassLoader(getClassLoader())
					.allowIO(IOAccess.newBuilder()
							.fileSystem(new ExtensionFileSystem(fileSystem.resolve("extensions/")))
							.build())
					.engine(engine).build();


			var extPath = fileSystem.resolve("extensions/");
			if (Files.exists(extPath)) {
				Files.list(extPath)
						.filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".js"))
						.forEach(extFile -> {
							try {
								context.getBindings("js").putMember("extensions", this);
								Source source = Source.newBuilder(
										"js",
										Files.readString(extFile, StandardCharsets.UTF_8),
										extFile.getFileName().toString() + ".mjs")
										.encoding(StandardCharsets.UTF_8)
										.build();
								context.eval(source);
							} catch (IOException ex) {
								log.error("", ex);
							}
						});
			}

		}
	}

	@Override
	public void close() throws Exception {
		if (engine != null) {
			context.close(true);
			engine.close(true);
		}
	}

}
