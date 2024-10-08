package com.condation.cms.content.markdown.rules.block;

/*-
 * #%L
 * cms-content
 * %%
 * Copyright (C) 2023 - 2024 CondationCMS
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

import com.condation.cms.content.markdown.Block;
import com.condation.cms.content.markdown.BlockElementRule;
import com.condation.cms.content.markdown.InlineRenderer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author t.marx
 */
public class ParagraphBlockRule implements BlockElementRule {

	private static final Pattern PATTERN = Pattern.compile(
			"(\\A|(^\n)|\\Z)(?<content>.+?)((^\n)|\\Z)",
			Pattern.MULTILINE | Pattern.DOTALL);
	
	

	@Override
	public Block next(String md) {
		Matcher matcher = PATTERN.matcher(md);
		if (matcher.find()) {
			return new ParagraphBlock(matcher.start(), matcher.end(), matcher.group("content").trim());
		}
		return null;
	}

	public static record ParagraphBlock(int start, int end, String content) implements Block {

		@Override
		public String render(InlineRenderer inlineRenderer) {
			return "<p>%s</p>".formatted(inlineRenderer.render(content));
		}
	}

}
