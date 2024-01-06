package com.github.thmarx.cms.content.views;

import com.github.thmarx.cms.content.views.model.View;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

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
/**
 *
 * @author t.marx
 */
public class ViewParser {

	public static View parse(final Path viewFile) throws IOException {
		try (var input = Files.newBufferedReader(viewFile, StandardCharsets.UTF_8)) {

			Representer representer = new Representer(new DumperOptions());
			representer.getPropertyUtils().setSkipMissingProperties(true);
			LoaderOptions loaderOptions = new LoaderOptions();
			Constructor constructor = new Constructor(View.class, loaderOptions);
			Yaml yaml = new Yaml(constructor, representer);

			return yaml.loadAs(input, View.class);
		}
	}
}
