package com.condation.cms.api.feature;

/*-
 * #%L
 * cms-api
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


import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author t.marx
 */
@Slf4j
public abstract class FeatureContainer {
	public Map<Class<? extends Feature>, Feature> features = new HashMap<>();
	
	public boolean has(Class<? extends Feature> featureClass) {
		return features.containsKey(featureClass);
	}

	public <T extends Feature> void add(Class<T> featureClass, T feature) {
		features.put(featureClass, feature);
	}

	public <T extends Feature> T get(Class<T> featureClass) {
		return (T) features.get(featureClass);
	}

	public void close() throws Exception {
		features.values()
				.stream()
				.filter(AutoCloseable.class::isInstance)
				.map(AutoCloseable.class::cast)
				.forEach(feature -> {
					try {
						feature.close();
					} catch (Exception e) {
						log.error(null, e);
					}
				});
	}
}
