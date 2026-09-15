package com.condation.cms.core.serivce.impl;

/*-
 * #%L
 * CMS Core
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

import com.condation.cms.api.Constants;
import com.condation.cms.api.repository.MutableContentRepository;
import com.condation.cms.api.utils.PathUtil;
import com.condation.cms.core.serivce.Service;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author thmar
 */
@Slf4j
public class NodeTranslationService implements Service {
	
	private final MutableContentRepository contentRepository;
	
	public NodeTranslationService (final MutableContentRepository contentRepository) {
		this.contentRepository = contentRepository;
	}
	
	public boolean removeTranslation (String uri, String language) {
		var node = contentRepository.findByUrl(uri).or(() -> contentRepository.get(uri));
		if (node.isPresent()) {
			try {
				var document = contentRepository.load(node.get()).orElseThrow();
				Map<String, Object> meta = new HashMap<>(node.get().data());
				var translations = (Map<String, Object>)meta.getOrDefault("translations", new HashMap<>());
				translations.remove(language);
				meta.put("translations", translations);
				
				var path = node.get().path();
				contentRepository.save(path, meta, document.content());
				log.debug("file {} saved", path);

				return true;
			} catch (IOException ex) {
				log.error("", ex);
				return false;
			}
		}
		
		return false;
	}
	
	public boolean addTranslation (String uri, String site, String translationUri, String language) {

		var node = contentRepository.findByUrl(uri).or(() -> contentRepository.get(uri));
		if (node.isPresent()) {
			try {
				var document = contentRepository.load(node.get()).orElseThrow();
				Map<String, Object> meta = new HashMap<>(node.get().data());
				var translations = (Map<String, Object>)meta.getOrDefault("translations", new HashMap<>());
				translations.put(language, PathUtil.toURL(translationUri));
				meta.put("translations", translations);
				
				var path = node.get().path();
				contentRepository.save(path, meta, document.content());
				log.debug("file {} saved", path);

				return true;
			} catch (IOException ex) {
				log.error("", ex);
				return false;
			}
		}
		
		return false;
	}
}
