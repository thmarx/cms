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
import com.github.thmarx.cms.api.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NRTCachingDirectory;

/**
 *
 * @author t.marx
 */
@Slf4j
public class LuceneIndex implements AutoCloseable {

	private Directory directory;
	private IndexWriter writer = null;

	private SearcherManager nrt_manager;
	private NRTCachingDirectory nrt_index;

	@Override
	public void close() throws Exception {
		if (nrt_manager != null) {
			nrt_manager.close();

			writer.commit();
			writer.close();
			directory.close();
		}
	}

	public void commit() throws IOException {
		writer.flush();
		writer.commit();
		nrt_manager.maybeRefresh();
	}

	void add(Document document) throws IOException {
		writer.addDocument(document);
		commit();
	}

	void update(Term term, Document document) throws IOException {
		writer.updateDocument(term, document);
		commit();
	}

	void delete(Query query) throws IOException {
		writer.deleteDocuments(query);
		commit();
	}

	List<Document> query(Query query, Sort sort) throws IOException {
		IndexSearcher searcher = nrt_manager.acquire();
		try {
			var topDocs = searcher.search(query, Integer.MAX_VALUE, sort);

			List<Document> result = new ArrayList<>();
			for (var scoreDoc : topDocs.scoreDocs) {
				result.add(searcher.storedFields().document(scoreDoc.doc));
			}

			return result;
		} catch (IOException e) {
			log.error("", e);
		} finally {
			nrt_manager.release(searcher);
		}
		return Collections.emptyList();
	}

	List<Document> query(Query query) throws IOException {
		IndexSearcher searcher = nrt_manager.acquire();
		try {
			var topDocs = searcher.search(query, Integer.MAX_VALUE);

			List<Document> result = new ArrayList<>();
			for (var scoreDoc : topDocs.scoreDocs) {
				result.add(searcher.storedFields().document(scoreDoc.doc));
			}

			return result;
		} catch (IOException e) {
			log.error("", e);
		} finally {
			nrt_manager.release(searcher);
		}
		return Collections.emptyList();
	}

	public void open(Path path) throws IOException {
		if (Files.exists(path)) {
			FileUtils.deleteFolder(path);
		}
		Files.createDirectories(path);

		this.directory = FSDirectory.open(path);
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new KeywordAnalyzer());
		indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		indexWriterConfig.setCommitOnClose(true);
		nrt_index = new NRTCachingDirectory(directory, 5.0, 60.0);
		writer = new IndexWriter(nrt_index, indexWriterConfig);

		final SearcherFactory sf = new SearcherFactory();
		nrt_manager = new SearcherManager(writer, true, true, sf);
	}

	SortField.Type getFieldType(String fieldName) throws IOException {
		IndexSearcher searcher = nrt_manager.acquire();
		try {

			var fieldInfos = FieldInfos.getMergedFieldInfos(searcher.getIndexReader());

			var fieldInfo = fieldInfos.fieldInfo(fieldName);
			if (fieldInfo == null) {
				return null;
			}

			return switch (fieldInfo.getDocValuesType()) {
				case NUMERIC -> SortField.Type.INT;
				case BINARY -> SortField.Type.STRING;
				case SORTED -> SortField.Type.STRING;
				case SORTED_NUMERIC -> SortField.Type.INT;
				case SORTED_SET -> SortField.Type.STRING;
				default -> null;
			}; // Prüfen Sie, ob das Feld integer, long, float oder double ist
			// Diese Information ist nicht direkt in FieldInfo verfügbar,
			// Sie müssen dies möglicherweise aus dem Kontext wissen
			// Hier nehmen wir an, dass es ein Integer-Feld ist
			// Ähnlich wie bei NUMERIC müssen Sie den genauen Typ kennen
			// Hier nehmen wir an, dass es ein Integer-Feld ist
		} catch (Exception e) {
			log.error("", e);
		} finally {
			nrt_manager.release(searcher);
		}
		return null;
	}
}
