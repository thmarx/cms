package com.condation.cms.api.theme;

/*-
 * #%L
 * cms-api
 * %%
 * Copyright (C) 2023 - 2024 CondationCMS
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


import com.condation.cms.api.ThemeProperties;
import com.condation.cms.api.messages.MessageSource;
import java.nio.file.Path;

/**
 *
 * @author thmar
 */

public interface Theme {
	
	MessageSource getMessages ();
	
	String getName();
	
	Path templatesPath ();
	
	Path extensionsPath ();
	
	Path assetsPath ();
	
	ThemeProperties properties();
	
	Theme getParentTheme ();
	
	Path resolveExtension (String path);
	Path resolveAsset (String path);
	Path resolveTemplate (String path);
	
	/**
	 * empty theme is used for sites without configured theme
	 * @return 
	 */
	boolean empty();
}
