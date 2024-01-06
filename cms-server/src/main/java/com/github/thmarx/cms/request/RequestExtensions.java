package com.github.thmarx.cms.request;

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

import com.github.thmarx.cms.api.extensions.http.ExtensionHttpHandler;
import com.github.thmarx.cms.api.feature.Feature;
import com.github.thmarx.cms.content.ShortCodes;
import com.github.thmarx.cms.extensions.HttpHandlerExtension;
import com.github.thmarx.cms.extensions.TemplateFunctionExtension;
import com.github.thmarx.cms.extensions.TemplateSupplierExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.graalvm.polyglot.Context;

/**
 *
 * @author t.marx
 */
@RequiredArgsConstructor
public class RequestExtensions implements AutoCloseable, Feature {

	@Getter
	private final List<HttpHandlerExtension> httpHandlerExtensions = new ArrayList<>();
	@Getter
	private final List<TemplateSupplierExtension> registerTemplateSupplier = new ArrayList<>();
	@Getter
	private final List<TemplateFunctionExtension> registerTemplateFunctions = new ArrayList<>();
	@Getter
	private final Map<String, Function<ShortCodes.Parameter, String>> shortCodes = new HashMap<>();
	
	private final Map<String, BiConsumer<Request, Response>> routes = new HashMap<>();

	@Getter
	private final Context context;
	private final Context themeContext;
	
	public void registerHttpExtension(final String method, final String path, final ExtensionHttpHandler handler) {
		httpHandlerExtensions.add(new HttpHandlerExtension(method, path, handler));
	}
	
	public void registerRoute(final String path, final BiConsumer<Request, Response> handler) {
		routes.put(path, handler);
	}

	public Optional<HttpHandlerExtension> findHttpHandler (final String method, final String path) {
		return httpHandlerExtensions.stream().filter(handler -> handler.method().equalsIgnoreCase(method) && handler.path().equalsIgnoreCase(path)).findFirst();
	}
	
	public void registerTemplateSupplier(final String path, final Supplier<?> supplier) {
		registerTemplateSupplier.add(new TemplateSupplierExtension(path, supplier));
	}

	public void registerTemplateFunction(final String path, final Function<?, ?> function) {
		registerTemplateFunctions.add(new TemplateFunctionExtension(path, function));
	}
	
	public void registerShortCode(final String shortCode, final Function<ShortCodes.Parameter, String> function) {
		shortCodes.put(shortCode, function);
	}

	@Override
	public void close() throws Exception {
		context.close();
		if (themeContext != null) {
			themeContext.close();
		}
	}
}
