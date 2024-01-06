package com.github.thmarx.cms.media;

/*-
 * #%L
 * cms-media
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

import com.github.thmarx.cms.api.media.Media;
import com.github.thmarx.cms.api.media.MediaService;
import com.github.thmarx.cms.api.media.meta.Meta;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author t.marx
 */
@Slf4j
@RequiredArgsConstructor
public class FileMediaService implements MediaService {

	private final Path assetBase;

	public boolean hasMetaData(final String media) {
		var metaFile = getMetaFile(media);
		return Files.exists(metaFile);
	}
	
	private Path getMetaFile (final String media) {
		String mediaPath = media;
		if (mediaPath.startsWith("/")) {
			mediaPath = mediaPath.substring(1);
		}
		var mediaFile = assetBase.resolve(mediaPath);
		var metaFileName = mediaFile.getFileName().toString() + ".meta.yaml";
		var metaFile = mediaFile.getParent().resolve(metaFileName);
		
		return metaFile;
	}
	
	private Meta loadMeta (final String media) {
		
		var metaFile = getMetaFile(media);
		if (Files.exists(metaFile)) {
			try {
				var content = Files.readString(metaFile, StandardCharsets.UTF_8);
				return new Yaml().loadAs(content, Meta.class);
			} catch (IOException ex) {
				log.error(null, ex);
			}
		}
		
		return new Meta();
	}

	@Override
	public Media get(final String media) {
		String mediaPath = media;
		if (mediaPath.startsWith("/")) {
			mediaPath = mediaPath.substring(1);
		}
		var mediaFile = assetBase.resolve(mediaPath);
		var meta = loadMeta(media);
		
		return new Media(mediaPath, meta, Files.exists(mediaFile));
	}
}
