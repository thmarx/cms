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
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.content.TaxonomyResponse;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.DB;
import com.github.thmarx.cms.api.db.taxonomy.Taxonomy;
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import com.github.thmarx.cms.api.request.RequestContext;
import com.github.thmarx.cms.api.request.features.RequestFeature;
import com.github.thmarx.cms.api.utils.NodeUtil;
import com.github.thmarx.cms.api.utils.PathUtil;
import com.github.thmarx.cms.filesystem.functions.list.Node;
import com.github.thmarx.cms.request.RenderContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class TaxonomyResolver {

	private final ContentRenderer contentRenderer;
	private final ContentParser contentParser;

	private final DB db;

	private Optional<Taxonomy> getTaxonomy(final RequestContext context) {
		var uri = context.get(RequestFeature.class).uri();
		var slug = uri.split("/")[1];
		return db.getTaxonomies().forSlug(slug);
	}

	public boolean isTaxonomy(final RequestContext context) {
		return getTaxonomy(context).isPresent();
	}

	private Optional<String> getTaxonomyValue(final RequestContext context) {
		var uri = context.get(RequestFeature.class).uri();
		var uriParts = uri.split("/");

		return uriParts.length == 3 ? Optional.of(uriParts[2]) : Optional.empty();
	}

	public Optional<TaxonomyResponse> getTaxonomyResponse(final RequestContext context) {

		var taxonomyOptional = getTaxonomy(context);
		if (taxonomyOptional.isEmpty()) {
			return Optional.empty();
		}

		try {

			var taxonomy = taxonomyOptional.get();

			String template = "taxonomy.html";
			var meta = new HashMap<String, Object>();

			Optional<String> value = getTaxonomyValue(context);
			List<Node> nodes = List.of();
			if (value.isPresent()) {
				template = "taxonomy-single.html";
				meta.put(Constants.MetaFields.TITLE, taxonomy.getTitle() + " - " + value.get());
				nodes = db.getTaxonomies().withValue(taxonomy, value.get()).stream().map(node -> {
					try {
						var name = NodeUtil.getName(node);
						final Path contentBase = db.getFileSystem().resolve("content/");
						var temp_path = contentBase.resolve(node.uri());
						var url = PathUtil.toURI(temp_path, contentBase);
						var md = contentParser.parse(temp_path);
						var excerpt = NodeUtil.excerpt(node, md.content(), Constants.DEFAULT_EXCERPT_LENGTH, context.get(RenderContext.class).markdownRenderer());
						return new Node(name, url, excerpt, node.data());
					} catch (IOException ioe) {
						log.error(null, ioe);
					}
					return null;
				}).filter(node -> node != null).toList();
			} else {
				meta.put(Constants.MetaFields.TITLE, taxonomy.getTitle());
			}
			meta.put(Constants.MetaFields.TEMPLATE, template);

			String content = contentRenderer.renderTaxonomy(taxonomy, context, meta, nodes);

			return Optional.of(new TaxonomyResponse(content, taxonomy));
		} catch (Exception ex) {
			log.error(null, ex);
			return Optional.empty();
		}
	}
}
