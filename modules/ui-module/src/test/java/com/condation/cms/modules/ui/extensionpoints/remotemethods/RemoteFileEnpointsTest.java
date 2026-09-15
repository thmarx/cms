package com.condation.cms.modules.ui.extensionpoints.remotemethods;

/*-
 * #%L
 * UI Module
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
import com.condation.cms.api.db.ContentNode;
import com.condation.cms.api.db.DB;
import com.condation.cms.api.db.DBFileSystem;
import com.condation.cms.api.module.SiteModuleContext;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.repository.ContentDocument;
import com.condation.cms.api.repository.MutableContentRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author thmar
 */
@ExtendWith(MockitoExtension.class)
public class RemoteFileEnpointsTest {
	
	@Mock
	SiteModuleContext moduleContext;
	
	@Mock
	private DB db;
	
	@Mock
	private Path basePath;
	
	@Mock
	private DBFileSystem dbFileSystem;

	@Mock
	private MutableContentRepository contentRepository;

	public RemoteFileEnpointsTest() {
	}

	@Test
	void rejectsUnsupportedFileTypesInsteadOfReturningNullBases() {
		Assertions.assertThatThrownBy(() -> AbstractRemoteMethodeExtension.getBase(dbFileSystem, "unknown"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unsupported file type: unknown");
		Assertions.assertThatThrownBy(() -> AbstractRemoteMethodeExtension.getWritableBase(dbFileSystem, "unknown"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unsupported file type: unknown");
		Assertions.assertThatThrownBy(() -> AbstractRemoteMethodeExtension.getBase(dbFileSystem, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Unsupported file type: null");
	}

	@Test
	public void create_folder_with_absolut_path_throws_error() throws Exception {
		
		doThrow(new IllegalArgumentException("invalid content path"))
				.when(contentRepository).createDirectory(Mockito.anyString());
		RemoteFileEnpoints fileEndpoints = endpoints();
		fileEndpoints.setContext(moduleContext);
		
		Assertions.assertThatThrownBy(() -> fileEndpoints.createFolder(Map.of(
				"type", "content",
				"uri", "/test/absolut/path"
		))).isInstanceOf(RPCException.class);//.hasMessage("invalid path");
		
	}

	@Test
	void listUsesContentTitleButKeepsTechnicalName() throws Exception {
		var node = new ContentNode(
				"about.md",
				"/about",
				"about.md",
				Map.of(Constants.MetaFields.TITLE, "About us")
		);
		when(contentRepository.children("")).thenReturn(List.of(node));

		var endpoints = endpoints();
		endpoints.setContext(moduleContext);

		@SuppressWarnings("unchecked")
		var result = (Map<String, Object>) endpoints.list(Map.of(
				"type", "content",
				"uri", ""
		));
		var files = (List<RemoteFileEnpoints.File>) result.get("files");

		Assertions.assertThat(files).singleElement().satisfies(file -> {
			Assertions.assertThat(file.name()).isEqualTo("about.md");
			Assertions.assertThat(file.displayName()).isEqualTo("About us");
			Assertions.assertThat(((RemoteFileEnpoints.Content) file).url()).isEqualTo("/about");
			Assertions.assertThat(((RemoteFileEnpoints.Content) file).title()).isEqualTo("About us");
		});
	}

	@Test
	void renameMarkdownContentUpdatesTitleWithoutChangingFileName() throws Exception {
		var node = new ContentNode("about.md", "/about", "about.md",
				Map.of(Constants.MetaFields.TITLE, "Old title", Constants.MetaFields.TEMPLATE, "page"));
		when(contentRepository.get("about.md")).thenReturn(Optional.of(node));
		when(contentRepository.load(node)).thenReturn(Optional.of(new ContentDocument(node, "Body")));

		var endpoints = endpoints();
		endpoints.setContext(moduleContext);
		endpoints.renameFile(Map.of(
				"type", "content",
				"uri", "",
				"name", "about.md",
				"newName", "New title"
		));

		verify(contentRepository).save(
				Mockito.eq("about.md"),
				Mockito.argThat(meta -> "New title".equals(meta.get(Constants.MetaFields.TITLE))
						&& "page".equals(meta.get(Constants.MetaFields.TEMPLATE))),
				Mockito.eq("Body"));
	}

	private RemoteFileEnpoints endpoints() {
		var endpoints = new RemoteFileEnpoints() {
			@Override
			protected MutableContentRepository getMutableContentRepository(Map<String, Object> parameters) {
				return contentRepository;
			}

			@Override
			protected com.condation.cms.api.repository.ContentRepository getContentRepository(
					Map<String, Object> parameters) {
				return contentRepository;
			}
		};
		endpoints.setContext(moduleContext);
		return endpoints;
	}
	
}
