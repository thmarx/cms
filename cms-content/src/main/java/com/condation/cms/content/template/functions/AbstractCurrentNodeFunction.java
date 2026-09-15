package com.condation.cms.content.template.functions;

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
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.IsPreviewFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.request.RequestContextScope;
import com.condation.cms.api.repository.ContentRepository;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
public abstract class AbstractCurrentNodeFunction {

	protected final ContentNode currentNode;
	protected final ContentRepository contentRepository;
	protected final RequestContext context;

	protected boolean isPreview() {
		if (RequestContextScope.REQUEST_CONTEXT.isBound()
				&& RequestContextScope.REQUEST_CONTEXT.get().has(IsPreviewFeature.class)) {
			return true;
		}

		return false;
	}
	protected String getPreviewMode() {
		if (RequestContextScope.REQUEST_CONTEXT.isBound()
				&& RequestContextScope.REQUEST_CONTEXT.get().has(IsPreviewFeature.class)) {
			return RequestContextScope.REQUEST_CONTEXT.get().get(IsPreviewFeature.class).mode().getValue();
		}

		return IsPreviewFeature.Mode.PREVIEW.getValue();
	}

	protected String repositoryPath(String basePath, String path) {
		basePath = normalize(basePath);
		var childPath = path.replace('\\', '/');
		while (childPath.startsWith("./")) {
			childPath = childPath.substring(2);
		}
		while (childPath.startsWith("/")) {
			childPath = childPath.substring(1);
		}
		if (".".equals(childPath)) {
			childPath = "";
		}
		if (childPath.isEmpty()) {
			return basePath;
		}
		return basePath.isEmpty() ? childPath : basePath + "/" + childPath;
	}

	protected String currentDirectory() {
		if (currentNode == null) {
			return "";
		}
		var path = normalize(currentNode.path());
		if (currentNode.isDirectory()) {
			return path;
		}
		var separator = path.lastIndexOf('/');
		return separator < 0 ? "" : path.substring(0, separator);
	}

	protected static String normalize(String path) {
		var normalized = path == null ? "" : path.replace('\\', '/');
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		while (normalized.endsWith("/") && !normalized.isEmpty()) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}
}
