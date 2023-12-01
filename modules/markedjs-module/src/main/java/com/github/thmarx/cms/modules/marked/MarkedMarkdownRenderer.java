package com.github.thmarx.cms.modules.marked;

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
import com.github.thmarx.cms.api.markdown.MarkdownRenderer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jsoup.Jsoup;

/**
 *
 * @author t.marx
 */
@Slf4j
public class MarkedMarkdownRenderer implements MarkdownRenderer {

	public final Context context;

	public final Value markedFunction;
	public final Source markedSource;

	public MarkedMarkdownRenderer(final Context context) {
		try {
			this.context = context;

			var content = new String(MarkedMarkdownRenderer.class.getResourceAsStream("marked.min.js").readAllBytes(), StandardCharsets.UTF_8);
			markedSource = Source.newBuilder("js", content, "marked.mjs").build();
			context.eval(markedSource);

			var markedFunctionSource = """
                              (function (param) {
								%s
                                    return marked.parse(param);
                              })
                              """.formatted(preview());

			markedFunction = context.eval("js", markedFunctionSource);
		} catch (IOException ex) {
			log.error(null, ex);
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void close() {
		context.close(true);
	}

	private String preview() {
		return """
			var PreviewContext = Java.type('com.github.thmarx.cms.api.PreviewContext');
			
			if (PreviewContext.IS_PREVIEW.get()) {
				console.log(PreviewContext.IS_PREVIEW.get())
                const cleanUrl = (href) => {
                  try {
                    href = encodeURI(href).replace(/%25/g, '%');
                  } catch (e) {
                    return null;
                  }
                  return href;
               }
				const renderer = {
				  link(href, title, text) {
					  const cleanHref = cleanUrl(href);
					  if (cleanHref === null) {
						return text;
					  }
					  href = cleanHref;
                      // is relativ internal url
                      if (!href.startsWith("http://") && !href.startsWith("https://")) {
						if (href.includes("?")) {
							href += "&preview"
					    } else {
							href += "?preview"
						}
					  }
                      
					  let out = '<a href="' + href + '"';
					  if (title) {
						out += ' title="' + title + '"';
					  }
					  out += '>' + text + '</a>';
					  return out;
					}
				}
				marked.use({ renderer });
         
			}
         """;
	}

	@Override
	public String excerpt(String markdown, int length) {
		var content = markedFunction.execute(markdown).asString();
		String text = Jsoup.parse(content).text();

		if (text.length() <= length) {
			return text;
		} else {
			return text.substring(0, length);
		}
	}

	@Override
	public String render(String markdown) {
		return markedFunction.execute(markdown).asString();
	}

}
