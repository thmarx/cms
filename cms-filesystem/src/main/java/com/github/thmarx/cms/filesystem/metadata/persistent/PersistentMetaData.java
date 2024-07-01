package com.github.thmarx.cms.filesystem.metadata.persistent;

/*-
 * #%L
 * cms-filesystem
 * %%
 * Copyright (C) 2023 - 2024 Marx-Software
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
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.api.db.ContentQuery;
import com.github.thmarx.cms.filesystem.metadata.AbstractMetaData;
import com.github.thmarx.cms.filesystem.metadata.query.ExcerptMapperFunction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.h2.mvstore.MVStore;

/**
 *
 * @author t.marx
 */
@Slf4j
@RequiredArgsConstructor
public class PersistentMetaData extends AbstractMetaData implements AutoCloseable {

	private final Path hostPath;

	private LuceneIndex index;
	private MVStore store;

	@Override
	public void open() throws IOException {

		Files.createDirectories(hostPath.resolve("data/metadata/store"));
		Files.createDirectories(hostPath.resolve("data/metadata/index"));

		index = new LuceneIndex();
		index.open(hostPath.resolve("data/metadata/index"));

		store = MVStore.open(hostPath.resolve("data/metadata/store/data.db").toString());

		nodes = store.openMap("nodes");
		tree = store.openMap("tree");
	}

	@Override
	public void close() throws IOException {
		try {
			if (index != null) {
				index.close();
			}
			if (store != null) {
				store.close();
			}
		} catch (Exception ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public void addFile(String uri, Map<String, Object> data, LocalDate lastModified) {

		var parts = uri.split(Constants.SPLIT_PATH_PATTERN);
		final ContentNode node = new ContentNode(uri, parts[parts.length - 1], data, lastModified);

		nodes.put(uri, node);

		var folder = getFolder(uri);
		if (folder.isPresent()) {
			folder.get().children().put(node.name(), node);
		} else {
			tree.put(node.name(), node);
		}

		Document document = new Document();
		document.add(new StringField("_uri", uri, Field.Store.YES));
		//document.add(new StringField("_source", GSON.toJson(node), Field.Store.NO));

		DocumentHelper.addData(document, data);

		document.add(new StringField("content.type", node.contentType(), Field.Store.NO));

		try {
			this.index.update(new Term("_uri", uri), document);
		} catch (IOException ex) {
			log.error("", ex);
		}
	}

	@Override
	public void clear() {
		super.clear();
		try {
			index.delete(new MatchAllDocsQuery());
		} catch (IOException ex) {
			log.error("", ex);
		}
	}

	@Override
	public <T> ContentQuery<T> query(final BiFunction<ContentNode, Integer, T> nodeMapper) {
		return new LuceneQuery<>(this.index, this, new ExcerptMapperFunction<>(nodeMapper));
	}

	@Override
	public <T> ContentQuery<T> query(final String startURI, final BiFunction<ContentNode, Integer, T> nodeMapper) {

		final String uri;
		if (startURI.startsWith("/")) {
			uri = startURI.substring(1);
		} else {
			uri = startURI;
		}
		return new LuceneQuery<>(uri, this.index, this, new ExcerptMapperFunction<>(nodeMapper));
	}

}
