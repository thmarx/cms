package com.github.thmarx.cms.api;

/*-
 * #%L
 * cms-server
 * %%
 * Copyright (C) 2023 Marx-Software
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author t.marx
 */
public class ThemeProperties extends YamlProperties {
	
	public ThemeProperties (final Map<String, Object> properties) {
		super(properties);
	}
	
	public String templateEngine () {
		return (String)getSubMap("template").getOrDefault("engine", "freemarker");
	}
	
	public List<String> activeModules () {
		return (List<String>)getSubMap("modules").getOrDefault("active", List.of());
	}
}