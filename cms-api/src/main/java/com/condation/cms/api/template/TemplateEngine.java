package com.condation.cms.api.template;

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
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.theme.Theme;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author thmar
 */
public interface TemplateEngine {

	public void invalidateCache();

	void updateTheme(Theme theme);

	String render(final String template, final TemplateEngine.Model model) throws IOException;

	default String renderFromString(final String templateString, final TemplateEngine.Model model) throws IOException {
		return templateString;
	}

	@RequiredArgsConstructor
	public static class Model {

		public final Map<String, Object> values = new HashMap<>();
		public final ContentNode contentNode;
		public final RequestContext requestContext;

		public Model copy() {
			var copy = new Model(this.contentNode, this.requestContext);
			
			copy.values.putAll(this.values);
			
			return copy;
		}
	}
}
