package com.condation.cms.content;

/*-
 * #%L
 * CMS Content
 * %%
 * Copyright (C) 2023 - 2026 CondationCMS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import com.condation.cms.api.content.ContentResponse;
import com.condation.cms.api.content.DefaultContentResponse;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.CurrentNodeFeature;
import com.condation.cms.api.feature.features.RequestFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.content.views.ViewParser;
import com.condation.cms.extensions.request.RequestExtensions;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public class ViewResolver {

	private final ContentRenderer contentRenderer;

	private final ContentRepository contentRepository;

	public ViewResolver(
			ContentRenderer contentRenderer,
			ContentRepository contentRepository) {
		this.contentRenderer = contentRenderer;
		this.contentRepository = contentRepository;
	}

	public Optional<ContentResponse> getViewContent(final RequestContext context) {
		return getViewContent(context, true);
	}

	public Optional<ContentResponse> getViewContent(final RequestContext context, final boolean checkVisibility) {
		var requestUrl = context.get(RequestFeature.class).uri();
		var contentNodeOpt = contentRepository.findByUrl(requestUrl);
		if (contentNodeOpt.isEmpty()) {
			return Optional.empty();
		}

		final ContentNode contentNode = contentNodeOpt.get();
		if (checkVisibility && !contentRepository.isVisible(contentNode)) {
			return Optional.empty();
		}
		if (!contentNode.isView()) {
			return Optional.empty();
		}

		context.add(CurrentNodeFeature.class, new CurrentNodeFeature(contentNode));

		try {
			var document = contentRepository.load(contentNode);
			if (document.isEmpty()) {
				return Optional.empty();
			}
			var view = ViewParser.parse(document.get().content());
			var page = view.getNodes(
				contentRepository,
				contentNode,
				context.get(RequestExtensions.class).getContext(), 
				context.get(RequestFeature.class).queryParameters(), context);
			
			var content = contentRenderer.renderView(document.get(), view, context, page);
			return Optional.of(new DefaultContentResponse(content, contentNode));
		} catch (Exception ex) {
			log.error(null, ex);
		}
		return Optional.empty();
	}
}
