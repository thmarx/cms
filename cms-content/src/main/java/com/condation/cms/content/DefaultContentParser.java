package com.condation.cms.content;

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
import com.condation.cms.api.cache.ICache;
import com.condation.cms.api.content.ContentParser;
import com.condation.cms.api.db.cms.ReadOnlyFile;
import com.condation.cms.api.repository.ContentResource;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author t.marx
 */
@Slf4j
public class DefaultContentParser implements ContentParser {

    private final ICache<String, Content> contentCache;
    
	public DefaultContentParser() {
        this(null);
	}
    public DefaultContentParser (ICache<String, Content> cache) {
        this.contentCache = cache;
    }

	@Override
	public void clearCache() {
        if (contentCache != null) {
            contentCache.invalidate();
        }
	}
    @Override
	public void clearCache(String uri) {
        if (contentCache != null) {
            contentCache.invalidate(uri);
        }
	}

	@Override
	public Content parse(final ContentResource contentResource) throws IOException {
		return parse(contentResource.path(), contentResource.content());
	}

	@Override
	public Map<String, Object> parseMeta(final ContentResource contentResource) throws IOException {
		return _parseMeta(readContent(contentResource.content()));
	}

	/**
	 * @deprecated use {@link #parse(ContentResource)} instead
	 */
	@Override
	@Deprecated(since = "8.4.0", forRemoval = false)
	public Content parse(final ReadOnlyFile contentFile) throws IOException {
		if (contentCache != null && contentCache.contains(contentFile.relativePath())) {
			return contentCache.get(contentFile.relativePath());
		}

		var readContent = readContent(contentFile.getAllLines());
		var content = new Content(readContent.content(), _parseMeta(readContent));
		if (contentCache != null) {
			contentCache.put(contentFile.uri(), content);
		}
		return content;
    }

	private Content parse(String cacheKey, String rawContent) {
		if (contentCache != null && contentCache.contains(cacheKey)) {
			return contentCache.get(cacheKey);
		}

		ContentRecord readContent = readContent(rawContent);
		var content = new Content(readContent.content(), _parseMeta(readContent));
		if (contentCache != null) {
			contentCache.put(cacheKey, content);
		}
		return content;
	}

	private Map<String, Object> _parseMeta(ContentRecord content) {
		if (Strings.isNullOrEmpty(content.meta().trim())) {
			return Collections.emptyMap();
		}
		try {
			return new Yaml().load(content.meta().trim());
		} catch (Exception e) {
			log.error("error parsing yaml: " + content.meta(), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * @deprecated use {@link #parseMeta(ContentResource)} instead
	 */
	@Override
	@Deprecated(since = "8.4.0", forRemoval = false)
	public Map<String, Object> parseMeta(final ReadOnlyFile contentFile) throws IOException {
		ContentRecord readContent = readContent(contentFile.getAllLines());

		return _parseMeta(readContent);
	}

	private ContentRecord readContent(final String rawContent) {
		return readContent(rawContent.lines().toList());
	}

	private ContentRecord readContent(final List<String> fileContent) {

		StringBuilder contentBuilder = new StringBuilder();
		StringBuilder metaBuilder = new StringBuilder();

		boolean inFrontMatter = false;
		boolean frontMatterClosed = false;

		for (String line : fileContent) {
			if (line.trim().equals("---") && !frontMatterClosed) {
				if (!inFrontMatter) {
					inFrontMatter = true; // Start Frontmatter
					continue;
				} else if (!frontMatterClosed) {
					frontMatterClosed = true; // Ende Frontmatter
					inFrontMatter = false;
					continue;
				}
			}

			if (inFrontMatter) {
				metaBuilder.append(line).append("\r\n");
			} else {
				contentBuilder.append(line).append("\r\n");
			}
		}

		return new ContentRecord(contentBuilder.toString(), metaBuilder.toString());
	}
}
