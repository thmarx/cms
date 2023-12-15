package com.github.thmarx.cms.server.jetty.handler;

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
import com.github.thmarx.cms.api.content.TaxonomyResponse;
import com.github.thmarx.cms.content.ContentResolver;
import com.github.thmarx.cms.api.db.DB;
import com.github.thmarx.cms.api.db.taxonomy.Taxonomy;
import com.github.thmarx.cms.api.request.ThreadLocalRequestContext;
import com.github.thmarx.cms.content.ContentRenderer;
import com.github.thmarx.cms.content.TaxonomyResolver;
import com.github.thmarx.cms.request.RequestContextFactory;
import com.github.thmarx.cms.utils.HTTPUtil;
import java.util.HashMap;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
@Slf4j
public class JettyTaxonomyHandler extends Handler.Abstract {

	private final TaxonomyResolver taxonomyResolver;
	private final RequestContextFactory requestContextFactory;
	private final DB db;

	private Optional<Taxonomy> getTaxonomy (final Request request) {
		var uri = request.getHttpURI().getPath();
		
		var slug = uri.split("/")[1];
		
		return db.getTaxonomies().forSlug(slug);
	}
	
	private Optional<String> getTaxonomyValue (final Request request) {
		var uri = request.getHttpURI().getPath();
		var uriParts = uri.split("/");
		
		return uriParts.length == 3 ? Optional.of(uriParts[2]) : Optional.empty();
	}
	
	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		try (
				var requestContext = requestContextFactory.create(request)) {
			
			ThreadLocalRequestContext.REQUEST_CONTEXT.set(requestContext);
			
			if (!taxonomyResolver.isTaxonomy(requestContext)) {
				return false;
			}
			
			Optional<TaxonomyResponse> taxonomyResponse = taxonomyResolver.getTaxonomyResponse(requestContext);
			if (taxonomyResponse.isPresent()) {
				response.setStatus(200);
				response.getHeaders().add("Content-Type", "%s; charset=utf-8".formatted(taxonomyResponse.get().contentType()));
				Content.Sink.write(response, true, taxonomyResponse.get().content(), callback);
				return true;
			}
			
		} catch (Exception e) {
			log.error("", e);
		} finally {
			ThreadLocalRequestContext.REQUEST_CONTEXT.remove();
		}
		return false;
	}

}
