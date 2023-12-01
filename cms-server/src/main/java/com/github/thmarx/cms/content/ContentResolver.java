package com.github.thmarx.cms.content;

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
import com.github.thmarx.cms.filesystem.FileSystem;
import com.github.thmarx.cms.filesystem.MetaData;
import com.github.thmarx.cms.api.utils.PathUtil;
import com.github.thmarx.cms.content.views.ViewParser;
import com.github.thmarx.cms.request.RequestContext;
import com.google.common.base.Strings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class ContentResolver {

	private final Path contentBase;

	private final ContentRenderer contentRenderer;

	private final FileSystem fileSystem;

	public Optional<String> getStaticContent(String uri) {
		if (uri.startsWith("/")) {
			uri = uri.substring(1);
		}
		Path staticFile = contentBase.resolve(uri);
		try {
			if (!PathUtil.isChild(contentBase, staticFile)) {
				return Optional.empty();
			}
			return Optional.ofNullable(Files.readString(staticFile, StandardCharsets.UTF_8));
		} catch (IOException ex) {
			log.error("", ex);
		}
		return Optional.empty();
	}

	public Optional<String> getContent(final RequestContext context) {
		try {
			String path;
			if (Strings.isNullOrEmpty(context.uri())) {
				path = "";
			} else {
				// remove leading slash
				path = context.uri().substring(1);
			}

			var contentPath = contentBase.resolve(path);
			Path contentFile = null;
			if (Files.exists(contentPath) && Files.isDirectory(contentPath)) {
				if (isView(contentPath)) {
					var viewFile = contentPath.resolve("view.yaml");
					var view = ViewParser.parse(viewFile);
					var content = contentRenderer.renderView(viewFile, view, context);
					return Optional.of(content);
				} else {
					// use index.md
					var tempFile = contentPath.resolve("index.md");
					if (Files.exists(tempFile)) {
						contentFile = tempFile;
					}
				}
			} else {
				var temp = contentBase.resolve(path + ".md");
				if (Files.exists(temp)) {
					contentFile = temp;
				} else {
					return Optional.empty();
				}
			}

			var uri = PathUtil.toRelativeFile(contentFile, contentBase);
			if (!fileSystem.isVisible(uri)) {
				return Optional.empty();
			}

			List<MetaData.MetaNode> sections = fileSystem.listSections(contentFile);

			Map<String, List<ContentRenderer.Section>> renderedSections = contentRenderer.renderSections(sections, context);

			var content = contentRenderer.render(contentFile, context, renderedSections);
			return Optional.of(content);
		} catch (Exception ex) {
			log.error(null, ex);
			return Optional.empty();
		}
	}

	private boolean isView(final Path path) {
		var viewFile = path.resolve("view.yaml");
		return Files.exists(viewFile);
	}
}
