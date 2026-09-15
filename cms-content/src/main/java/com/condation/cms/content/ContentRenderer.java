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

import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.db.collection.CollectionItem;
import com.condation.cms.api.db.taxonomy.Taxonomy;
import com.condation.cms.api.model.ListNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentDocument;
import com.condation.cms.api.repository.Section;
import com.condation.cms.content.views.model.View;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *
 * @author t.marx
 */
public interface ContentRenderer {

	String render(final ContentDocument document, final RequestContext context,
			final Map<String, List<SectionEntry>> sectionEntries) throws IOException;

	Map<String, List<SectionEntry>> renderSections(
			final List<Section> sections,
			final RequestContext context) throws IOException;

	String renderCollection(
			final ContentNode collectionNode,
			final CollectionItem item,
			final String template,
			final RequestContext context) throws IOException;

	String renderTaxonomyContent(final Optional<ContentDocument> document,
			final Taxonomy taxonomy, Optional<String> taxonomyValue,
			final RequestContext context, final Map<String, Object> meta,
			final Page<ListNode> page,
			Map<String, List<SectionEntry>> sectionEntries) throws IOException;

	String renderView(final ContentDocument document, final View view,
			final RequestContext requestContext, final Page<ListNode> page) throws IOException;

}
