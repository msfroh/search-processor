/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.ResourceAlreadyExistsException;
import org.opensearch.action.ActionFuture;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.admin.indices.settings.get.GetSettingsAction;
import org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.Client;
import org.opensearch.client.Requests;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.search.relevance.SearchRelevancePlugin;
import org.opensearch.search.relevance.configuration.ResultTransformerConfigurationFactory;
import org.opensearch.search.relevance.configuration.SearchConfigurationExtBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.opensearch.search.relevance.SearchRelevancePlugin.LOG_PREFIX;

public class SearchConfigurationsClient {
    private static final Logger logger = LogManager.getLogger(SearchConfigurationsClient.class);
    private static final String SEARCH_CONFIGURATIONS_INDEX_NAME = ".search_configurations";
    private static final String SEARCH_CONFIGURATIONS_MAPPING_FILE = "search-configurations-mapping.json";
    private static final String SEARCH_CONFIGURATIONS_SETTINGS_FILE = "search-configurations-settings.json";

    private final ClusterService clusterService;
    private final Client client;

    private final Map<String, ResultTransformerConfigurationFactory> resultTransformerConfigurationFactoryMap;

    public SearchConfigurationsClient(ClusterService clusterService, Client client,
                                      Map<String, ResultTransformerConfigurationFactory> resultTransformerConfigurationFactoryMap) {
        this.client = client;
        this.clusterService = clusterService;
        this.resultTransformerConfigurationFactoryMap = resultTransformerConfigurationFactoryMap;
    }

    private void createIndexIfNeeded(ActionListener<Void> completionListener) {
        if (!doesIndexExist()) {
            ClassLoader classLoader = SearchConfigurationsClient.class.getClassLoader();
            String mappingSource;
            String settingsSource;
            try (InputStream mappingStream = classLoader.getResourceAsStream(SEARCH_CONFIGURATIONS_MAPPING_FILE);
                 InputStream settingsStream = classLoader.getResourceAsStream(SEARCH_CONFIGURATIONS_SETTINGS_FILE)) {
                if (mappingStream == null) {
                    throw new IllegalStateException("Unable to load " + SEARCH_CONFIGURATIONS_MAPPING_FILE);
                }
                if (settingsStream == null) {
                    throw new IllegalStateException("Unable to load " + SEARCH_CONFIGURATIONS_SETTINGS_FILE);
                }
                mappingSource = new String(mappingStream.readAllBytes(), StandardCharsets.UTF_8);
                settingsSource = new String(settingsStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                completionListener.onFailure(e);
                return;
            }
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(SEARCH_CONFIGURATIONS_INDEX_NAME)
                    .mapping(mappingSource, XContentType.JSON)
                    .settings(settingsSource, XContentType.JSON);
            client.admin().indices().create(createIndexRequest, new ActionListener<CreateIndexResponse>() {
                @Override
                public void onResponse(CreateIndexResponse response) {
                    if (response.isAcknowledged()) {
                        logger.info(LOG_PREFIX + ": created index " + SEARCH_CONFIGURATIONS_INDEX_NAME);
                    } else {
                        logger.error(LOG_PREFIX + ": creation of index " + SEARCH_CONFIGURATIONS_INDEX_NAME +
                                " was not acknowledged.");
                    }
                    completionListener.onResponse(null);
                }

                @Override
                public void onFailure(Exception e) {
                    if (e instanceof ResourceAlreadyExistsException || e.getCause() instanceof ResourceAlreadyExistsException) {
                        logger.warn(LOG_PREFIX + ": exception on index creation", e);
                        // Treat this as success
                        completionListener.onResponse(null);
                    } else {
                        completionListener.onFailure(e);
                    }
                }
            });

        } else {
            completionListener.onResponse(null);
        }
    }

    public SearchConfigurationExtBuilder getSearchConfigurationSync(String name) throws Exception {
        CountDownLatch awaitLatch = new CountDownLatch(1);
        AtomicReference<SearchConfigurationExtBuilder> ref = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        getSearchConfigurationAsync(name, new ActionListener<>() {
            @Override
            public void onResponse(SearchConfigurationExtBuilder searchConfigurationExtBuilder) {
                ref.set(searchConfigurationExtBuilder);
            }

            @Override
            public void onFailure(Exception e) {
                failure.set(e);
            }
        });
        if (awaitLatch.await(SearchRelevancePlugin.getOperationTimeoutMillis(), TimeUnit.MILLISECONDS)) {
            if (failure.get() != null) {
                throw failure.get();
            }
            return ref.get();
        }
        throw new IllegalStateException("Operation timed out");
    }

    private interface ExceptionalConsumer<T> {
        void accept(T t) throws Exception;
    }


    private static <T, U> ActionListener<T> wrapListener(ActionListener<U> failurePropagator,
                                                         ExceptionalConsumer<T> callbackLogic) {
        return new ActionListener<T>() {
            @Override
            public void onResponse(T t) {
                try {
                    callbackLogic.accept(t);
                } catch (Exception e) {
                    failurePropagator.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                failurePropagator.onFailure(e);
            }
        };
    }

    public void getSearchConfigurationAsync(String name, ActionListener<SearchConfigurationExtBuilder> listener) {
        createIndexIfNeeded(ActionListener.wrap(v -> {
                    GetRequest getRequest = Requests.getRequest(SEARCH_CONFIGURATIONS_INDEX_NAME)
                            .id(name);
                    client.get(getRequest, ActionListener.wrap(response -> {
                        if (response.getSourceAsString() == null) {
                            logger.warn(LOG_PREFIX + ": search_configuratgion " + name + " not found; response: " + response);
                            listener.onResponse(null);
                        } else {
                            XContentParser parser = XContentType.JSON.xContent().createParser(
                                    NamedXContentRegistry.EMPTY,
                                    LoggingDeprecationHandler.INSTANCE,
                                    response.getSourceAsString()
                            );
                            parser.nextToken();
                            listener.onResponse(SearchConfigurationExtBuilder.parse(parser, resultTransformerConfigurationFactoryMap));
                        }
                    }, listener::onFailure));
                },
                listener::onFailure));
    }

    public void putSearchConfigurationAsync(String name, SearchConfigurationExtBuilder searchConfigurationExtBuilder, ActionListener<Boolean> listener) {
        createIndexIfNeeded(ActionListener.wrap(v -> {
            IndexRequest indexRequest = Requests.indexRequest(SEARCH_CONFIGURATIONS_INDEX_NAME)
                    .id(name)
                    .source(searchConfigurationExtBuilder.toXContent());
            client.index(indexRequest, ActionListener.wrap(response -> {
                switch (response.getResult()) {
                    case CREATED:
                    case UPDATED:
                    case NOOP:
                        listener.onResponse(true);
                        break;
                    default:
                        logger.warn(LOG_PREFIX + ": putSearchConfiguration - response: " + response);
                        listener.onResponse(false);
                }
            }, listener::onFailure));
        }, listener::onFailure));
    }

    private boolean doesIndexExist() {
        ClusterState clusterState = clusterService.state();
        return clusterState.routingTable().hasIndex(SEARCH_CONFIGURATIONS_INDEX_NAME);
    }

    public Settings getIndexSettings(String indexName, String[] settingNames) {
        GetSettingsRequest getSettingsRequest = new GetSettingsRequest()
                .indices(indexName);
        if (settingNames != null && settingNames.length > 0) {
            getSettingsRequest.names(settingNames);
        }
        GetSettingsResponse getSettingsResponse = client.execute(GetSettingsAction.INSTANCE, getSettingsRequest).actionGet();
        return getSettingsResponse.getIndexToSettings().get(indexName);
    }

    public Client getOpenSearchClient() {
        return client;
    }
}
