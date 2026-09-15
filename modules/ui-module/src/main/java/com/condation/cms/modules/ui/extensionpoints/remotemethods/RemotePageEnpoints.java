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
import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.db.Page;
import com.condation.cms.api.db.VariantSearchMode;
import com.condation.cms.api.eventbus.events.ReIndexContentMetaDataEvent;
import com.condation.cms.api.feature.features.EventBusFeature;
import com.condation.cms.api.feature.features.WorkflowFeature;
import com.condation.cms.api.ui.extensions.UIRemoteMethodExtensionPoint;
import com.condation.cms.api.utils.FileUtils;
import com.condation.modules.api.annotation.Extension;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.condation.cms.api.ui.annotations.RemoteMethod;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.api.utils.HTTPUtil;
import com.condation.cms.api.utils.PathUtil;
import com.condation.cms.modules.ui.utils.UIPathUtil;
import com.condation.cms.core.content.io.YamlHeaderUpdater;
import com.condation.cms.modules.ui.model.NodeDTO;
import com.condation.cms.modules.ui.utils.NumberUtils;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 *
 * @author thorstenmarx
 */
@Slf4j
@Extension(UIRemoteMethodExtensionPoint.class)
public class RemotePageEnpoints extends AbstractRemoteMethodeExtension {

	public record SearchResultDto(String uri, String url, String title) {
	}

	@RemoteMethod(name = "pages.search", permissions = {Permissions.CONTENT_EDIT})
    public Object searchPages (Map<String, Object> parameters) {
		String query = "";

		if (parameters.get("query") instanceof String stringValue) {
			query = stringValue;
		}

			var hits = getContentRepository(parameters).query()
					.searchByTitle(query).variants(VariantSearchMode.ORIGINAL).get().stream()
				.map(node -> {
					String url = node.url();
					String previewUrl = HTTPUtil.modifyUrl(url, context);

					String title = "";
					if (node.data().get(Constants.MetaFields.TITLE) instanceof String titleValue) {
						title = titleValue;
					}

					return new SearchResultDto(previewUrl, url, title);
				})
				.toList();

		Map<String, Object> result = new HashMap<>();
		result.put("result", hits);

		return result;
	}
	
	@RemoteMethod(name = "pages.filter", permissions = {Permissions.CONTENT_EDIT})
    public Object filterPages (Map<String, Object> parameters) throws RPCException {
        
		var query = getContentRepository(parameters).query();
        
        if (parameters.containsKey("contentType") && parameters.get("contentType") != null) {
            query.contentType(parameters.get("contentType").toString());
        }
        
        if (parameters.containsKey("query") && parameters.get("query") != null) {
            query.expression(parameters.get("query").toString());
        }
        
        if (parameters.containsKey("excerpt") && parameters.get("excerpt") != null) {
            try {
                long excerpt = Long.parseLong(parameters.get("excerpt").toString());
                query.excerpt(excerpt);
            } catch (NumberFormatException e) {
                log.error("Error parsing excerpt", e);
                throw new RPCException(0, "invalid excerpt value: " + parameters.get("excerpt"));
            }
        }
        
        if (parameters.get("where") instanceof List whereClauses) {
            for (Object clauseObj : whereClauses) {
                if (clauseObj instanceof Map clause) {
                    String field = (String) clause.get("field");
                    Object value = clause.get("value");
                    String operator = (String) clause.getOrDefault("operator", "=");
                    query.where(field, operator, value);
                }
            }
        }
        
        if (parameters.containsKey("orderby") && parameters.get("orderby") != null) {
            String field = parameters.get("orderby").toString();
            String direction = (String) parameters.getOrDefault("order", "asc");
            if ("desc".equalsIgnoreCase(direction)) {
                query.orderby(field).desc();
            } else {
                query.orderby(field).asc();
            }
        }
        
		long page = NumberUtils.toLong(parameters.getOrDefault("page", 1l));
		long size = NumberUtils.toLong(parameters.getOrDefault("size", 5l));
		
        var pageList = query.page(page, size);
		
		return new Page<>(pageList.getTotalItems(), pageList.getPageSize(), pageList.getTotalPages(), pageList.getPage(), 
				pageList.getItems().stream().map(node -> {
					var url = HTTPUtil.modifyUrl(node.url(), context);
					
					return new NodeDTO(url, node.data());
				}).toList()
		);
    }
    
	@RemoteMethod(name = "page.delete", permissions = {Permissions.CONTENT_EDIT})
	public Object deletePage(Map<String, Object> parameters) throws RPCException {
			var repository = getMutableContentRepository(parameters);

		Map<String, Object> result = new HashMap<>();

		try {
			var uri = (String) parameters.getOrDefault("uri", "");
			var name = (String) parameters.getOrDefault("name", "");

			if (Strings.isNullOrEmpty(name)) {
				throw new RPCException(0, "filename can not be null");
			}
			
				var path = uri.isBlank() ? name : uri + Constants.PATH_SEPARATOR + name;
				log.debug("deleting content {}", path);
				var node = repository.get(path);
				var sections = node.isPresent() ? repository.sections(node.get()) : java.util.List.<com.condation.cms.api.repository.Section>of();
				repository.delete(path);
				sections.forEach(section -> {
					try {
						log.debug("deleting section {}", section.id());
						repository.deleteRecursively(section.id());
				} catch (IOException ioe) {
						log.error("error deleting section {}", section.id(), ioe);
				}
			});
		} catch (Exception e) {
			log.error("", e);
			throw new RPCException(0, e.getMessage());
		}

		return result;
	}

	@RemoteMethod(name = "page.create", permissions = {Permissions.CONTENT_EDIT})
	public Object createPage(Map<String, Object> parameters) throws RPCException {
			var repository = getMutableContentRepository(parameters);

		Map<String, Object> result = new HashMap<>();

		try {
			var uri = (String) parameters.getOrDefault("uri", "");
			var name = (String) parameters.getOrDefault("name", "");
			
			if (Strings.isNullOrEmpty(name)) {
				throw new RPCException(1, "name must not be empty");
			}
			
			var contentType = (String) parameters.getOrDefault("contentType", "");
			
			var pageTemplate = uiHooks().contentTypes().getPageTemplate(contentType);
			
			if (pageTemplate.isEmpty()) {
				throw new RPCException(1, "no contentType selected");
			}
			
			Map<String, Object> meta = new HashMap<>();
			meta.put("createdAt", Date.from(Instant.now()));
			meta.put("createdBy", getUserName());
			meta.put(Constants.MetaFields.TITLE, name);
			meta.put(Constants.MetaFields.TEMPLATE, pageTemplate.get().template());
			meta.put(Constants.MetaFields.STATUS, getContext().get(WorkflowFeature.class).workflow().getStatusProvider().newNodeStatus());

			name = UIPathUtil.toValidFilename(name);
			
				String newFile;
				if (name.endsWith(".md")) {
					newFile = uri.isBlank() ? name : uri + "/" + name;
				} else {
					newFile = (uri.isBlank() ? name : uri + "/" + name) + "/index.md";
				}
				if (repository.resourceExists(newFile)) {
					throw new RPCException(1, "directory already exists");
				}
				repository.save(newFile, meta, "");
				result.put("uri", repository.get(newFile).map(node -> node.url()).orElse(newFile));
		} catch (Exception e) {
			log.error("", e);
			throw new RPCException(0, e.getMessage());
		}

		return result;
	}
}
