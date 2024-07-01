package com.github.thmarx.cms.utils;

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

import com.github.thmarx.cms.api.utils.NodeUtil;
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.db.ContentNode;
import com.github.thmarx.cms.filesystem.metadata.memory.MemoryMetaData;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author t.marx
 */
public class NodeUtilNGTest {

	public NodeUtilNGTest() {
	}

	@Test
	public void getName_returns_default_name() {
		ContentNode node = new ContentNode("/", "index", Map.of());

		var name = NodeUtil.getName(node);

		Assertions.assertThat(name).isNotBlank().isEqualTo("index");
	}

	@Test
	public void getName_returns_title() {
		ContentNode node = new ContentNode("/", "index", Map.of(
				"title", "The Title"
		));

		var name = NodeUtil.getName(node);

		Assertions.assertThat(name).isNotBlank().isEqualTo("The Title");
	}

	@Test
	public void getName_returns_title_if_emtpy_menu() {
		ContentNode node = new ContentNode("/", "index", Map.of(
				"title", "The Title",
				"menu", Map.of(
				)
		));
		
		var name = NodeUtil.getName(node);

		Assertions.assertThat(name).isNotBlank().isEqualTo("The Title");
	}
	
	@Test
	public void getName_returns_menu_title() {
		ContentNode node = new ContentNode("/", "index", Map.of(
				"title", "The Title",
				"menu", Map.of(
						"title", "Menu title"
				)
		));
		
		var name = NodeUtil.getName(node);

		Assertions.assertThat(name).isNotBlank().isEqualTo("Menu title");
	}
	
	@Test
	public void getMenuPosition() {
		ContentNode node = new ContentNode("/", "index", Map.of(
				"menu", Map.of(
						"position", 1.5
				)
		));
		var order = NodeUtil.getMenuPosition(node);
		Assertions.assertThat(order).isEqualTo(1.5f);
	}
	
	@Test
	public void getDefaultMenuPosition() {
		ContentNode node = new ContentNode("/", "index", Map.of(
				"menu", Map.of()
		));
		var position = NodeUtil.getMenuPosition(node);
		Assertions.assertThat(position).isEqualTo(Constants.DEFAULT_MENU_POSITION);
	}
	
	@Test
	public void getDefaultMenuPositionNoMenuMap() {
		ContentNode node = new ContentNode("/", "index", Map.of());
		var position = NodeUtil.getMenuPosition(node);
		Assertions.assertThat(position).isEqualTo(Constants.DEFAULT_MENU_POSITION);
	}
}
