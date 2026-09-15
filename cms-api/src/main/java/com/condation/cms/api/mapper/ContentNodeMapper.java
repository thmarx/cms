package com.condation.cms.api.mapper;

/*-
 * #%L
 * CMS Api
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
import com.condation.cms.api.Constants;
import com.condation.cms.api.content.MapAccess;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.MarkdownRendererFeature;
import com.condation.cms.api.model.ListNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.cms.api.utils.NodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor()
@Slf4j
public class ContentNodeMapper {

	private final ContentRepository contentRepository;

	public ListNode toListNode(final ContentNode node, final RequestContext context, final int excerptLength) {

		var name = NodeUtil.getName(node);
		var url = HTTPUtil.modifyUrl(node.url(), context);
		var rawContent = "";
		try {
			rawContent = contentRepository.load(node)
					.map(document -> document.content())
					.orElse("");
		} catch (Exception ex) {
			log.error("could not load content for {}", node.path(), ex);
		}
		var excerpt = NodeUtil.excerpt(node, rawContent, excerptLength,
				context.get(MarkdownRendererFeature.class).markdownRenderer());
		return new ListNode(name, url, excerpt, MapAccess.of(node.data()));

	}

	public ListNode toListNode(final ContentNode node, final RequestContext context) {
		return toListNode(node, context, Constants.DEFAULT_EXCERPT_LENGTH);
	}
}
