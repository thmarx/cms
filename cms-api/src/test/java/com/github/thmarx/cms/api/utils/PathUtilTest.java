package com.github.thmarx.cms.api.utils;

/*-
 * #%L
 * cms-api
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

import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 *
 * @author t.marx
 */
public class PathUtilTest {
	
	

	@Test
	public void test_to_uri() {
		
		Path contentBase = Path.of("src/");
		
		String toURI = PathUtil.toURI(contentBase.resolve("index.md"), contentBase);
		assertThat(toURI).isEqualTo("/");
		
		toURI = PathUtil.toURI(contentBase.resolve("modules/index.md"), contentBase);
		assertThat(toURI).isEqualTo("/modules");
		
		toURI = PathUtil.toURI(contentBase.resolve("modules/test.md"), contentBase);
		assertThat(toURI).isEqualTo("/modules/test");
		
		toURI = PathUtil.toURI(contentBase.resolve("test.md"), contentBase);
		assertThat(toURI).isEqualTo("/test");
		
		toURI = PathUtil.toURI(contentBase.resolve(""), contentBase);
		assertThat(toURI).isEqualTo("/");
	}
	
}
