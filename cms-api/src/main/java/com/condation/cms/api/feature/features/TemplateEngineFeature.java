package com.condation.cms.api.feature.features;

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
import com.condation.cms.api.annotations.FeatureScope;
import com.condation.cms.api.feature.Feature;
import com.condation.cms.api.request.RequestContext;
import com.condation.cms.api.template.TemplateEngine;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
@FeatureScope({FeatureScope.Scope.REQUEST})
public record TemplateEngineFeature(TemplateEngine templateEngine) implements Feature {

	public String render(String template, Map<String, Object> model, RequestContext requestContext) {
		try {
			var templateModel = new TemplateEngine.Model(null, requestContext);
			templateModel.values.putAll(model);
			return templateEngine.render(template, templateModel);
		} catch (IOException ioe) {
			log.error("", ioe);
		}
		return "";
	}
}
