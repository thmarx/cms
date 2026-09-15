package com.condation.cms.content;

/*-
 * #%L
 * integration-tests
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



import com.condation.cms.content.DefaultContentRenderer;
import com.condation.cms.content.TaxonomyResolver;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.taxonomy.Taxonomies;
import com.condation.cms.api.db.taxonomy.Taxonomy;
import com.condation.cms.api.feature.features.RequestFeature;
import com.condation.cms.api.mapper.ContentNodeMapper;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.repository.ContentRepository;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @author t.marx
 */
@ExtendWith(MockitoExtension.class)
public class TaxonomyResolverTest {
	
	@Mock
	DB db;
	
	@Mock
	DefaultContentRenderer contentRenderer;
	
	@Mock
	ContentNodeMapper contentNodeMapper;

	@Mock
	ContentRepository contentRepository;
	
	@Mock
	Taxonomies taxonomies;
	
	TaxonomyResolver taxonomyResolver;
	
	@BeforeEach
	public void setup () {
		Mockito.lenient().when(db.getTaxonomies()).thenReturn(taxonomies);
		Mockito.lenient().when(taxonomies.forSlug("tags")).thenReturn(Optional.of(new Taxonomy()));
		
		taxonomyResolver = new TaxonomyResolver(
				contentRenderer, db, contentNodeMapper, contentRepository);
	}

	@Test
	public void test_is_taxonomy() {
		RequestContext requestContext = new RequestContext();
		RequestFeature requestFeature = new RequestFeature("tags", Map.of());	
		requestContext.add(RequestFeature.class, requestFeature);
	
		Assertions.assertThat(taxonomyResolver.isTaxonomy(requestContext)).isTrue();
	}
	
	@Test
	public void test_is_taxonomy_with_value() {
		RequestContext requestContext = new RequestContext();
		RequestFeature requestFeature = new RequestFeature("tags/red", Map.of());	
		requestContext.add(RequestFeature.class, requestFeature);
	
		Assertions.assertThat(taxonomyResolver.isTaxonomy(requestContext)).isTrue();
	}
	
	@Test
	public void test_get_taxonomy_value() {
		RequestContext requestContext = new RequestContext();
		RequestFeature requestFeature = new RequestFeature("tags/red", Map.of());	
		requestContext.add(RequestFeature.class, requestFeature);
	
		Assertions.assertThat(taxonomyResolver.getTaxonomyValue(requestContext))
				.isPresent()
				.hasValue("red")
				;
	}
}
