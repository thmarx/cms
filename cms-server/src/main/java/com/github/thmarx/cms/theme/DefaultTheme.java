package com.github.thmarx.cms.theme;

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
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.SiteProperties;
import com.github.thmarx.cms.api.ThemeProperties;
import com.github.thmarx.cms.api.messages.EmptyMessageSource;
import com.github.thmarx.cms.api.messages.MessageSource;
import com.github.thmarx.cms.api.messages.ThemeMessageSource;
import com.github.thmarx.cms.api.theme.Theme;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author thmar
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultTheme implements Theme {

	public static final Theme EMPTY = new DefaultTheme(null, new ThemeProperties(Collections.emptyMap()), true, new EmptyMessageSource());

	private final Path themePath;
	private final ThemeProperties properties;
	private final MessageSource messages;
	private boolean empty = false;

	private DefaultTheme(final Path templatePath, final ThemeProperties themeProperties, final boolean empty, final MessageSource messages) {
		this(templatePath, themeProperties, messages);
		this.empty = empty;
	}

	public static Theme load(Path themePath, SiteProperties siteProperties, MessageSource siteMessages) throws IOException {
		Yaml yaml = new Yaml();
		Path themeYaml = themePath.resolve("theme.yaml");

		MessageSource messages = new ThemeMessageSource(siteProperties, themePath.resolve("messages/"), siteMessages);
		
		var content = Files.readString(themeYaml, StandardCharsets.UTF_8);
		Map<String, Object> config = (Map<String, Object>) yaml.load(content);

		return new DefaultTheme(themePath, new ThemeProperties(config), messages);
	}

	@Override
	public boolean empty() {
		return empty;
	}

	@Override
	public ThemeProperties properties() {
		return properties;
	}

	@Override
	public String getName() {
		return (String) properties.get("name");
	}

	@Override
	public Path assetsPath() {
		return themePath.resolve(Constants.Folders.ASSETS);
	}

	@Override
	public Path templatesPath() {
		return themePath.resolve(Constants.Folders.TEMPLATES);
	}

	@Override
	public Path extensionsPath() {
		return themePath.resolve(Constants.Folders.EXTENSIONS);
	}

	@Override
	public MessageSource getMessages() {
		return messages;
	}
}
