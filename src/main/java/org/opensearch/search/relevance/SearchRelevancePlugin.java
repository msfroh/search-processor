/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.search.relevance;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.support.ActionFilter;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsFilter;
import org.opensearch.common.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.SearchPlugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;
import org.opensearch.script.ScriptService;
import org.opensearch.search.relevance.actionfilter.SearchActionFilter;
import org.opensearch.search.relevance.client.SearchConfigurationsClient;
import org.opensearch.search.relevance.configuration.ResultTransformerConfigurationFactory;
import org.opensearch.search.relevance.configuration.SearchConfigurationExtBuilder;
import org.opensearch.search.relevance.resthandler.SearchConfigurationRestHandler;
import org.opensearch.search.relevance.resthandler.action.GetSearchConfigurationAction;
import org.opensearch.search.relevance.resthandler.action.PutSearchConfigurationAction;
import org.opensearch.search.relevance.transformer.ResultTransformer;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.KendraIntelligentRanker;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraClientSettings;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.client.KendraHttpClient;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankerSettings;
import org.opensearch.search.relevance.transformer.kendraintelligentranking.configuration.KendraIntelligentRankingConfigurationFactory;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SearchRelevancePlugin extends Plugin implements ActionPlugin, SearchPlugin {
    public static final String LOG_PREFIX = "search-processor";

    // TODO: Make this configurable?
    public static final long OPERATION_TIMEOUT_MILLIS = 60_000L;

    private SearchConfigurationsClient searchConfigurationsClient;
    private KendraHttpClient kendraClient;
    private KendraIntelligentRanker kendraIntelligentRanker;

    private static final Map<String, ResultTransformerConfigurationFactory> resultTransformerConfigFactoryMap =
            getResultTransformerConfigurationFactories().stream()
                    .collect(Collectors.toMap(ResultTransformerConfigurationFactory::getName, i -> i));

    private Collection<ResultTransformer> getAllResultTransformers() {
        // Initialize and add other transformers here
        return List.of(this.kendraIntelligentRanker);
    }

    private static Collection<ResultTransformerConfigurationFactory> getResultTransformerConfigurationFactories() {
        // Register result transformer config factories here.
        return List.of(KendraIntelligentRankingConfigurationFactory.INSTANCE);
    }

    public static Map<String, ResultTransformerConfigurationFactory> getResultTransformerConfigFactoryMap() {
        return Collections.unmodifiableMap(resultTransformerConfigFactoryMap);
    }

    @Override
    public List<ActionFilter> getActionFilters() {
        return List.of(new SearchActionFilter(getAllResultTransformers(), searchConfigurationsClient));
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings, IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter, IndexNameExpressionResolver indexNameExpressionResolver, Supplier<DiscoveryNodes> nodesInCluster) {
        return List.of(
                new SearchConfigurationRestHandler()
        );
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return List.of(
                new ActionHandler<>(PutSearchConfigurationAction.ACTION_TYPE, PutSearchConfigurationAction.class),
                new ActionHandler<>(GetSearchConfigurationAction.ACTION_TYPE, GetSearchConfigurationAction.class)
        );
    }

    @Override
    public List<Setting<?>> getSettings() {
        // NOTE: cannot use kendraIntelligentRanker.getTransformerSettings because the object is not yet created
        List<Setting<?>> allTransformerSettings = new ArrayList<>();
        allTransformerSettings.addAll(KendraIntelligentRankerSettings.getAllSettings());
        // Add settings for other transformers here
        return allTransformerSettings;
    }

    @Override
    public Collection<Object> createComponents(
            Client client,
            ClusterService clusterService,
            ThreadPool threadPool,
            ResourceWatcherService resourceWatcherService,
            ScriptService scriptService,
            NamedXContentRegistry xContentRegistry,
            Environment environment,
            NodeEnvironment nodeEnvironment,
            NamedWriteableRegistry namedWriteableRegistry,
            IndexNameExpressionResolver indexNameExpressionResolver,
            Supplier<RepositoriesService> repositoriesServiceSupplier
    ) {
        this.searchConfigurationsClient = new SearchConfigurationsClient(clusterService, client, resultTransformerConfigFactoryMap);
        this.kendraClient = new KendraHttpClient(KendraClientSettings.getClientSettings(environment.settings()));
        this.kendraIntelligentRanker = new KendraIntelligentRanker(this.kendraClient);

        return Arrays.asList(
                this.searchConfigurationsClient,
                this.kendraClient,
                this.kendraIntelligentRanker
        );
    }

    @Override
    public List<SearchExtSpec<?>> getSearchExts() {
        return Collections.singletonList(
                new SearchExtSpec<>(SearchConfigurationExtBuilder.NAME,
                        input -> new SearchConfigurationExtBuilder(input, resultTransformerConfigFactoryMap),
                        parser -> SearchConfigurationExtBuilder.parse(parser, resultTransformerConfigFactoryMap)));
    }

    public static long getOperationTimeoutMillis() {
        return OPERATION_TIMEOUT_MILLIS;
    }

}
