package com.github.thmarx.cms.api.template;

/*-
 * #%L
 * cms-server
 * %%
 * Copyright (C) 2023 Marx-Software
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.cms.CMSFile;
import com.github.thmarx.cms.api.theme.Theme;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author thmar
 */
public interface TemplateEngine {
	
	public void invalidateCache();

	void updateTheme (Theme theme);
	
	String render(final String template, final TemplateEngine.Model model) throws IOException;
	
	@RequiredArgsConstructor
	public static class Model {
		public final Map<String, Object> values = new HashMap<>();
		public final CMSFile contentFile;
		public final ContentNode contentNode;
	} 
}
