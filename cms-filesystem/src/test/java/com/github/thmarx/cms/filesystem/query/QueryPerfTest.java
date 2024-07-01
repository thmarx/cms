package com.github.thmarx.cms.filesystem.query;

/*-
 * #%L
 * cms-filesystem
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
import com.github.thmarx.cms.filesystem.metadata.memory.MemoryQuery;
import com.github.thmarx.cms.api.Constants;
import com.github.thmarx.cms.api.db.ContentNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;

/**
 *
 * @author t.marx
 */
public class QueryPerfTest {

	private static Collection<ContentNode> nodes;
	
	private static int COUNT = 1000;

	@BeforeAll
	public static void setup() {
		System.out.println("build elements");
		nodes = new ArrayList<>();
		for (int i = 0; i < COUNT; i++) {
			var node = new ContentNode("/test" + i, "test2.md", Map.of(
					"article", Map.of("featured", (i % 2 == 0 ? true : false)),
					"index", i,
					Constants.MetaFields.PUBLISH_DATE, Date.from(Instant.now().minus(1, ChronoUnit.DAYS)),
					"tags", List.of("one", "two"))
			);
			nodes.add(node);
		}
	}

	protected MemoryQuery<ContentNode> createQuery() {
		var query = new MemoryQuery<>(nodes, (node, i) -> node);
		
		return query;
	}

	@Nested
	@DisplayName("testing without secondary index")
	class NoIndex {

		@RepeatedTest(10)
		public void test_no_index() {
			System.out.println("run tests without index");
			
			MemoryQuery<ContentNode> query = createQuery();
			var nodes = query.where("article.featured", true).get();
			Assertions.assertThat(nodes).hasSize(COUNT / 2);

//			query = createQuery(false);
//			nodes = query.where("article.featured", "=", true).get();
//			Assertions.assertThat(nodes).hasSize(COUNT / 2);

//			query = createQuery(false);
//			nodes = query.where("article.featured", false).get();
//			Assertions.assertThat(nodes).hasSize(COUNT / 2);

//			query = createQuery(false);
//			nodes = query.where("article.featured", "=", false).get();
//			Assertions.assertThat(nodes).hasSize(COUNT / 2);
		}
	}

	@Nested
	@DisplayName("testing with secondary index")
	class WithIndex {

		@RepeatedTest(1)
		public void test_use_index() {
			System.out.println("run tests with index");
			
			MemoryQuery<ContentNode> query = createQuery();
			var nodes = query.where("article.featured", true).get();
			Assertions.assertThat(nodes).hasSize(COUNT / 2);

//			query = createQuery(true);
//			nodes = query.where("article.featured", "=", true).get();
//			Assertions.assertThat(nodes).hasSize(COUNT / 2);

//			query = createQuery(true);
//			nodes = query.where("article.featured", false).get();
//			Assertions.assertThat(nodes).hasSize(COUNT / 2);

//			query = createQuery(true);
//			nodes = query.where("article.featured", "=", false).get();
//			Assertions.assertThat(nodes).hasSize(COUNT / 2);
		}
	}

}
