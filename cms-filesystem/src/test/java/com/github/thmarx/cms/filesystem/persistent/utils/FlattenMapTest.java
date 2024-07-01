package com.github.thmarx.cms.filesystem.persistent.utils;

/*-
 * #%L
 * cms-filesystem
 * %%
 * Copyright (C) 2023 - 2024 Marx-Software
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

import com.github.thmarx.cms.filesystem.metadata.persistent.utils.FlattenMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;


public class FlattenMapTest {

    @Test
    public void testFlattenMap() {
        // Beispielinput
        Map<String, Object> nestedMap = new HashMap<>();
        Map<String, Object> nestedLevel1 = new HashMap<>();
        Map<String, Object> nestedLevel2 = new HashMap<>();
        
        nestedLevel2.put("key3", "value3");
        nestedLevel1.put("key2", nestedLevel2);
        nestedMap.put("key1", nestedLevel1);
        nestedMap.put("key4", "value4");
        
        // Erwartete flache Map
        Map<String, String> expectedFlatMap = new HashMap<>();
        expectedFlatMap.put("key1.key2.key3", "value3");
        expectedFlatMap.put("key4", "value4");
        
        // Flache Map erzeugen
        Map<String, Object> actualFlatMap = FlattenMap.flattenMap(nestedMap);
        
        // Überprüfen, ob die flache Map korrekt ist
        Assertions.assertThat(actualFlatMap).isEqualTo(expectedFlatMap);
    }

    @Test
    public void testFlattenMapWithEmptyMap() {
        // Leere Map
        Map<String, Object> emptyMap = new HashMap<>();
        
        // Erwartete flache Map
        Map<String, String> expectedFlatMap = new HashMap<>();
        
        // Flache Map erzeugen
        Map<String, Object> actualFlatMap = FlattenMap.flattenMap(emptyMap);
        
        // Überprüfen, ob die flache Map korrekt ist
        Assertions.assertThat(actualFlatMap).isEqualTo(expectedFlatMap);
    }

    @Test
    public void testFlattenMapWithSingleLevelMap() {
        // Einfache Map ohne Verschachtelung
        Map<String, Object> singleLevelMap = new HashMap<>();
        singleLevelMap.put("key1", "value1");
        singleLevelMap.put("key2", "value2");
        
        // Erwartete flache Map
        Map<String, String> expectedFlatMap = new HashMap<>();
        expectedFlatMap.put("key1", "value1");
        expectedFlatMap.put("key2", "value2");
        
        // Flache Map erzeugen
        Map<String, Object> actualFlatMap = FlattenMap.flattenMap(singleLevelMap);
        
        // Überprüfen, ob die flache Map korrekt ist
        Assertions.assertThat(actualFlatMap).isEqualTo(expectedFlatMap);
    }
}
