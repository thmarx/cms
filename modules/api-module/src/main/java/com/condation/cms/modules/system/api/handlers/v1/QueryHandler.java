package com.condation.cms.modules.system.api.handlers.v1;

/*-
 * #%L
 * CMS Api Module
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

import com.condation.cms.api.repository.ContentRepository;
import com.condation.cms.api.extensions.http.HttpHandler;
import static com.condation.cms.core.configuration.GSONProvider.GSON;
import com.condation.cms.modules.system.api.helpers.QueryParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.Map;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class QueryHandler implements HttpHandler {

	private final ContentRepository contentRepository;
    private final QueryParser queryParser;

	public QueryHandler(final ContentRepository contentRepository) {
		this.contentRepository = contentRepository;
        this.queryParser = new QueryParser();
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        try {
            String body = Content.Source.asString(request);
			Object result = queryParser.parse(contentRepository, body);

            response.getHeaders().add(HttpHeader.CONTENT_TYPE, "application/json; charset=utf-8");
            Content.Sink.write(response, true, GSON.toJson(result), callback);

        } catch (JsonSyntaxException | IOException e) {
            response.setStatus(400);
            var respObject = Map.of(
                "status", 400,
                "error", "Bad Request: Invalid JSON or I/O error."
            );
            Content.Sink.write(response, true, GSON.toJson(respObject), callback);
        } catch (Exception e) {
            response.setStatus(500);
            var respObject = Map.of(
                "status", 500,
                "error", "Internal Server Error: " + e.getMessage()
            );
            Content.Sink.write(response, true, GSON.toJson(respObject), callback);
        }
        return true;
    }
}
