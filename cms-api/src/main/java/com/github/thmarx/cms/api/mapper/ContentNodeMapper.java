package com.github.thmarx.cms.api.mapper;

/*-
 * #%L
 * cms-api
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
import com.github.thmarx.cms.api.content.ContentParser;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.DB;
import com.github.thmarx.cms.api.feature.Feature;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.api.model.ListNode;
import com.github.thmarx.cms.api.request.RequestContext;
import com.github.thmarx.cms.api.feature.features.IsPreviewFeature;
import com.github.thmarx.cms.api.feature.features.SitePropertiesFeature;
import com.github.thmarx.cms.api.utils.HTTPUtil;
import com.github.thmarx.cms.api.utils.NodeUtil;
import com.github.thmarx.cms.api.utils.PathUtil;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor(onConstructor = @__({
	@Inject}))
@Slf4j
public class ContentNodeMapper implements Feature {

	private final DB db;
	private final ContentParser contentParser;

	protected Optional<ContentParser.Content> parse(Path node) {
		try {
			//Path rel = contentBase.relativize(node);
			if (Files.isDirectory(node)) {
				node = node.resolve("index.md");
			}
			var md = contentParser.parse(node);

			return Optional.of(md);
		} catch (IOException ex) {
			log.error(null, ex);
		}
		return Optional.empty();
	}

	public ListNode toListNode(final ContentNode node, final RequestContext context, final int excerptLength) {

		var name = NodeUtil.getName(node);
		final Path contentBase = db.getFileSystem().resolve("content/");
		var temp_path = contentBase.resolve(node.uri());
		var url = PathUtil.toURI(temp_path, contentBase);
		
		url = HTTPUtil.modifyUrl(url, context);
		
		var md = parse(temp_path);
		var excerpt = NodeUtil.excerpt(node, md.get().content(), excerptLength, context.get(MarkdownRenderer.class));
		return new ListNode(name, url, excerpt, node.data());

	}

	public ListNode toListNode(final ContentNode node, final RequestContext context) {
		return toListNode(node, context, Constants.DEFAULT_EXCERPT_LENGTH);
	}
}
