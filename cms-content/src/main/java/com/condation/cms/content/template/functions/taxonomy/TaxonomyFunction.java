package com.condation.cms.content.template.functions.taxonomy;

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


import com.condation.cms.api.db.taxonomy.Taxonomy;
import com.condation.cms.api.db.DB;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class TaxonomyFunction {
	
	private final DB db;
	
	public List<Taxonomy> all () {
		return db.getTaxonomies().all();
	}
	
	public Taxonomy get (String slug) {
		return db.getTaxonomies().forSlug(slug).orElse(null);
	}
	
	public Set<String> values (String slug) {
		var taxonomy = db.getTaxonomies().forSlug(slug);
		if (taxonomy.isEmpty()) {
			return Collections.emptySet();
		}
		return db.getTaxonomies().values(taxonomy.get());
	}
	
	public Set<String> values (Taxonomy taxonomy) {
		return db.getTaxonomies().values(taxonomy);
	}
	
	public String url (final String taxonomy, final String value) {
		return "/%s/%s".formatted(taxonomy, value);
	}
	
	public String getTitle (final String taxonomy) {
		
		var taxo = db.getTaxonomies().forSlug(taxonomy);
		if (taxo.isPresent()) {
			return taxo.get().getTitle();
		}
		
		return taxonomy;
	}
	
	public String getTitle (final String taxonomy, String value) {
		
		var taxo = db.getTaxonomies().forSlug(taxonomy);
		if (taxo.isPresent()) {
			
			return taxo.get().getValueTitle(value);
		}
		
		return taxonomy;
	}
}
