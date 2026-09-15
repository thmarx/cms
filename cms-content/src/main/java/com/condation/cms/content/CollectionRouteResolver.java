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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import com.condation.cms.api.configuration.configs.CollectionConfiguration;
import com.condation.cms.api.configuration.configs.CollectionDefinition;
import com.condation.cms.api.configuration.configs.CollectionDetailConfiguration;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.repository.CollectionRepository;
import com.condation.cms.content.utils.SlugUtil;
import java.util.Comparator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/** Resolves configured collection detail routes without rendering them. */
@RequiredArgsConstructor
public class CollectionRouteResolver {

	private final CollectionRepository collectionRepository;
	private final CollectionConfiguration configuration;

	public Optional<ResolvedRoute> resolve(String requestUri) {
		if (configuration == null) {
			return Optional.empty();
		}
		var uri = normalizeUri(requestUri);
		for (var definition : configuration.collections().values().stream()
				.sorted(Comparator.comparing(CollectionDefinition::name))
				.toList()) {
			var resolved = resolve(definition, uri);
			if (resolved.isPresent()) {
				return resolved;
			}
		}
		return Optional.empty();
	}

	private Optional<ResolvedRoute> resolve(CollectionDefinition definition, String uri) {
		var detail = definition.detailPage();
		if (detail.isEmpty()) {
			return Optional.empty();
		}
		var template = new CollectionRouteTemplate(detail.get());
		if (!template.matchesShape(uri)) {
			return Optional.empty();
		}

		var collection = collectionRepository.collection(definition.name());
		Optional<CollectionItem> item;
		if (isSimpleParameter(detail.get())) {
			var routeValue = simpleRouteValue(detail.get(), uri);
			if ("id".equals(detail.get().parameter())) {
				item = findById(collection, routeValue);
			} else {
				item = findByRouteValue(collection, detail.get().parameter(), routeValue);
			}
		} else {
			var matches = collection.metadataQuery().get().stream()
					.filter(candidate -> template.matches(uri, candidate.id(), candidate.meta()))
					.limit(2)
					.toList();
			item = matches.size() == 1 ? collection.item(matches.getFirst().id()) : Optional.empty();
		}
		return item.map(value -> new ResolvedRoute(definition, detail.get(), value, uri));
	}

	private static boolean isSimpleParameter(CollectionDetailConfiguration detail) {
		return detail.parameters().size() == 1
				&& detail.parameterOccurrences() == 1
				&& !detail.hasFormats()
				&& detail.mappings().isEmpty();
	}

	private static String simpleRouteValue(CollectionDetailConfiguration detail, String uri) {
		var token = "{" + detail.parameter() + "}";
		var tokenStart = detail.route().indexOf(token);
		var prefix = detail.route().substring(0, tokenStart);
		var suffix = detail.route().substring(tokenStart + token.length());
		var end = uri.length() - suffix.length();
		return uri.substring(prefix.length(), end);
	}

	private static Optional<CollectionItem> findByRouteValue(
			com.condation.cms.api.db.collection.Collection collection,
			String parameter,
			String routeValue) {
		var slug = SlugUtil.slugify(routeValue);
		if (slug.isBlank()) {
			return Optional.empty();
		}

		var exactMatches = collection.metadataQuery()
				.where(parameter, slug)
				.page(1, 2)
				.getItems();
		if (exactMatches.size() == 1) {
			return collection.item(exactMatches.getFirst().id());
		}
		return Optional.empty();
	}

	private static Optional<CollectionItem> findById(
			com.condation.cms.api.db.collection.Collection collection,
			String id) {
		try {
			return collection.item(id);
		} catch (IllegalArgumentException _) {
			return Optional.empty();
		}
	}

	private static String normalizeUri(String uri) {
		return CollectionRouteTemplate.normalizeUri(uri);
	}

	public record ResolvedRoute(
			CollectionDefinition definition,
			CollectionDetailConfiguration detail,
			CollectionItem item,
			String uri) {
	}
}
