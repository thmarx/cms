package com.condation.cms.modules.system.api.services;

/*-
 * #%L
 * CMS Api Module
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

import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.modules.system.api.helpers.NodeHelper;
import com.condation.cms.modules.system.api.helpers.WhitelistFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.server.Request;

/**
 *
 * @author thorstenmarx
 */
@RequiredArgsConstructor
public class ContentService {

	private final ContentRepository contentRepository;
	private final Set<String> metaWhiteList;
	
	public Optional<ApiContentNode> resolve (String uri, Request request) {
		var resolved = resolveContentNode(uri);
		if (resolved.isEmpty()) {
			return Optional.empty();
		}
		
		final ContentNode node = resolved.get();
		
		return Optional.of(new ApiContentNode(
				uri, 
				NodeHelper.getLinks(node, request), 
				WhitelistFilter.applyWhitelist(node.data(), metaWhiteList)
		));
	}
	
	private Optional<ContentNode> resolveContentNode(String uri) {
		var requestUrl = uri.startsWith("/") ? uri : "/" + uri;
		var contentNode = contentRepository.findByUrl(requestUrl)
				.or(() -> contentRepository.get(uri));
		if (contentNode.isEmpty() || !contentRepository.isVisible(contentNode.get())) {
			return Optional.empty();
		}
		return contentNode;
	}
}
