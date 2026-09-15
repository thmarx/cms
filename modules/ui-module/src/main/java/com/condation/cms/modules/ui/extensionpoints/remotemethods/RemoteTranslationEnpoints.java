package com.condation.cms.modules.ui.extensionpoints.remotemethods;

/*-
 * #%L
 * UI Module
 * %%
 * Copyright (C) 2023 - 2026 CondationCMS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import com.condation.cms.api.Constants;
import com.condation.cms.api.SiteProperties;
import com.condation.cms.api.auth.Permissions;
import com.condation.cms.api.configuration.configs.SiteConfiguration;
import com.condation.cms.api.eventbus.events.ReIndexContentMetaDataEvent;
import com.condation.cms.api.feature.features.ConfigurationFeature;
import com.condation.cms.api.feature.features.EventBusFeature;
import com.condation.cms.api.ui.extensions.UIRemoteMethodExtensionPoint;
import com.condation.modules.api.annotation.Extension;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.condation.cms.api.ui.annotations.RemoteMethod;
import com.condation.cms.api.ui.rpc.RPCException;
import com.condation.cms.modules.ui.extensionpoints.remotemethods.dto.TranslationDto;
import com.condation.cms.core.content.io.ContentFileParser;
import com.condation.cms.modules.ui.utils.MetaConverter;
import com.condation.cms.modules.ui.utils.TranslationHelper;
import com.condation.cms.core.content.io.YamlHeaderUpdater;
import com.condation.cms.core.serivce.ServiceRegistry;
import com.condation.cms.core.serivce.impl.NodeTranslationService;
import com.condation.cms.core.serivce.impl.SiteLinkService;
import com.condation.cms.core.serivce.impl.SitePropertiesService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author thorstenmarx
 */
@Slf4j
@Extension(UIRemoteMethodExtensionPoint.class)
public class RemoteTranslationEnpoints extends AbstractRemoteMethodeExtension {

	@RemoteMethod(name = "translations.get", permissions = {Permissions.CONTENT_EDIT})
	public Object get(Map<String, Object> parameters) throws RPCException {
		Map<String, Object> result = new HashMap<>();
		List<TranslationDto> translations = new ArrayList<>();
		result.put("translations", translations);

		var uri = (String) parameters.getOrDefault("uri", "");

		var contentNodeOpt = getContentRepository(parameters).get(uri);
		var contentNode = contentNodeOpt.orElseThrow(() -> new RPCException("content node for uri %s not found".formatted(uri)));

		var siteProperties = getContext().get(ConfigurationFeature.class).configuration().get(SiteConfiguration.class).siteProperties();

		var translationHelper = new TranslationHelper(siteProperties);
		translationHelper.getFilteredMapping().forEach(mapping -> {
			if (mapping.language().equals(siteProperties.language())) {
				return;
			}
			var translationUri = contentNode.getMetaValue("translations.%s".formatted(mapping.language()), "");
			var deepLink = "";
			if (!translationUri.equals("")) {
				deepLink = ServiceRegistry.getInstance().get(mapping.site(), SiteLinkService.class).get().managerDeepLink(translationUri);
			}
			var service = ServiceRegistry.getInstance().get(mapping.site(), SitePropertiesService.class).get();
			translations.add(new TranslationDto(
					mapping.site(), 
					mapping.language(), 
					service.siteProperties().locale().getCountry().toLowerCase(), 
					translationUri, 
					deepLink));
		});

		return result;
	}

	@RemoteMethod(name = "translations.remove", permissions = {Permissions.CONTENT_EDIT})
	public Object remove(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableContentRepository(parameters);

		var uri = (String) parameters.get("uri");
		var language = (String) parameters.get("language");

		Map<String, Object> result = new HashMap<>();
		result.put("uri", uri);
		var node = repository.get(uri);
		if (node.isPresent()) {
			try {
				var document = repository.load(node.get()).orElseThrow();
				Map<String, Object> meta = new HashMap<>(node.get().data());
				if (meta.containsKey("translations")) {
					var translations = (Map<String, Object>) meta.get("translations");
					if (!translations.containsKey(language)) {
						return result;
					}
					var oldTranslationUri = (String)translations.remove(language);
					repository.save(uri, meta, document.content());
					log.debug("file {} saved", uri);

					getContext().get(EventBusFeature.class).eventBus().publish(new ReIndexContentMetaDataEvent(uri));

					final SiteProperties siteProperties = getContext().get(ConfigurationFeature.class)
							.configuration().get(SiteConfiguration.class)
							.siteProperties();
					// update target site
					var translationSite = siteProperties
							.translation()
							.getMapping().stream()
							.filter(mapping -> mapping.language().equals(language)).findFirst();
					if (translationSite.isPresent()) {
						var nodeTranslationService = ServiceRegistry.getInstance().get(translationSite.get().site(), NodeTranslationService.class);

						if (nodeTranslationService.isPresent()) {
							nodeTranslationService.get().removeTranslation(oldTranslationUri, siteProperties.language());
						}
					}
				}
			} catch (IOException ex) {
				log.error("", ex);
				throw new RPCException(0, ex.getMessage());
			}
		}

		return result;
	}

	@RemoteMethod(name = "translations.add", permissions = {Permissions.CONTENT_EDIT})
	public Object add(Map<String, Object> parameters) throws RPCException {
		var repository = getMutableContentRepository(parameters);

		var uri = (String) parameters.get("uri");
		var language = (String) parameters.get("language");
		var translation_url = (String) parameters.get("translationUri");

		Map<String, Object> result = new HashMap<>();
		result.put("uri", uri);
		var node = repository.get(uri);
		if (node.isPresent()) {
			try {
				var document = repository.load(node.get()).orElseThrow();
				Map<String, Object> meta = new HashMap<>(node.get().data());
				var translations = (Map<String, Object>) meta.getOrDefault("translations", new HashMap<>());
				translations.put(language, translation_url);
				meta.put("translations", translations);

				repository.save(uri, meta, document.content());
				log.debug("file {} saved", uri);

				getContext().get(EventBusFeature.class).eventBus().publish(new ReIndexContentMetaDataEvent(uri));
				final SiteProperties siteProperties = getContext().get(ConfigurationFeature.class)
						.configuration().get(SiteConfiguration.class)
						.siteProperties();
				// update target site
				var translationSite = siteProperties
						.translation()
						.getMapping().stream()
						.filter(mapping -> mapping.language().equals(language)).findFirst();
				if (translationSite.isPresent()) {
					var nodeTranslationService = ServiceRegistry.getInstance().get(translationSite.get().site(), NodeTranslationService.class);

					if (nodeTranslationService.isPresent()) {
						nodeTranslationService.get().addTranslation(translation_url, siteProperties.id(), uri, siteProperties.language());
					}
				}
			} catch (IOException ex) {
				log.error("", ex);
				throw new RPCException(0, ex.getMessage());
			}
		}

		return result;
	}
}
