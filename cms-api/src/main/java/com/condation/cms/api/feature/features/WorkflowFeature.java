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
import com.condation.cms.api.workflow.Workflow;

/**
 *
 * @author thorstenmarx
 */
@FeatureScope({FeatureScope.Scope.SITE, FeatureScope.Scope.MODULE, FeatureScope.Scope.REQUEST})
public record WorkflowFeature(Workflow workflow) implements Feature {
	
}
