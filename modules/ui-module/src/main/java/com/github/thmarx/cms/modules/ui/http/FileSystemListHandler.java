package com.github.thmarx.cms.modules.ui.http;

/*-
 * #%L
 * ui-module
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

import com.github.thmarx.cms.modules.ui.services.FileSystemService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpHeader;
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
public class FileSystemListHandler extends Handler.Abstract {

	private final FileSystemService fileSystemService;
	
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		
		String path = "";
		var fields = Request.getParameters(request);
		if (fields.getNames().contains("path")) {
			path = fields.getValue("path");
		}
		
		var nodes = fileSystemService.listContent(path);
		response.setStatus(200);
        response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json; charset=UTF-8");
		Content.Sink.write(response, true, GSON.toJson(nodes), callback);
		
		return true;
	}
	
}
