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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.condation.cms.api.content.DefaultContentResponse;
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.feature.features.RequestFeature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentDocument;
import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.repository.Section;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContentResolverTest {

	@Test
	void loadsAndRendersPageAndSectionsOnlyThroughRepository() throws Exception {
		var repository = mock(ContentRepository.class);
		var renderer = mock(ContentRenderer.class);
		var node = new ContentNode("index.md", "/", "index.md", Map.of("template", "page.html"));
		var document = new ContentDocument(node, "page body");
		var section = new Section(
				"index.hero.md",
				"index.md",
				"hero",
				Map.of("template", "section.html"),
				"hero body");
		Map<String, List<SectionEntry>> renderedSections = Map.of(
				"hero",
				List.of(new SectionEntry("hero", "rendered hero", section.data())));

		when(repository.findByUrl("/")).thenReturn(Optional.of(node));
		when(repository.isVisible(node)).thenReturn(true);
		when(repository.variants(node)).thenReturn(List.of());
		when(repository.load(node)).thenReturn(Optional.of(document));
		when(repository.sections(node)).thenReturn(List.of(section));
		when(renderer.renderSections(
				org.mockito.ArgumentMatchers.eq(List.of(section)),
				org.mockito.ArgumentMatchers.any()))
				.thenReturn(renderedSections);
		when(renderer.render(org.mockito.ArgumentMatchers.eq(document), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.eq(renderedSections))).thenReturn("rendered page");

		var context = new RequestContext();
		context.add(RequestFeature.class, new RequestFeature("/", Map.of()));
		var response = new ContentResolver(renderer, repository, new DefaultVariantSelector())
				.getContent(context);

		assertThat(response).containsInstanceOf(DefaultContentResponse.class);
		var rendered = (DefaultContentResponse) response.orElseThrow();
		assertThat(rendered.content()).isEqualTo("rendered page");
		assertThat(rendered.node()).isEqualTo(node);
		verify(repository).load(node);
		verify(repository).sections(node);
		verify(renderer).render(document, context, renderedSections);
	}
}
