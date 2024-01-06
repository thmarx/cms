package com.github.thmarx.cms.modules.example;

/*-
 * #%L
 * example-module
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

import com.github.thmarx.cms.api.extensions.HttpRouteExtensionPoint;
import com.github.thmarx.modules.api.annotation.Extension;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

/**
 *
 * @author t.marx
 */
@Extension(HttpRouteExtensionPoint.class)
public class ExampleJettyHttpRouteExtension extends HttpRouteExtensionPoint {

	@Override
	public String getRoute() {
		return "example/route";
	}

	@Override
	public void handle(Request request, Response response, Callback callback) {
		response.write(true, ByteBuffer.wrap("example route".getBytes(StandardCharsets.UTF_8)), callback);
	}
}
