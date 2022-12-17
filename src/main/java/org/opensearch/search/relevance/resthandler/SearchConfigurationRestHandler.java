/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.resthandler;

import org.opensearch.client.node.NodeClient;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestToXContentListener;
import org.opensearch.search.relevance.resthandler.action.GetSearchConfigurationAction;
import org.opensearch.search.relevance.resthandler.action.PutSearchConfigurationAction;
import org.opensearch.search.relevance.resthandler.model.GetSearchConfigurationRequest;
import org.opensearch.search.relevance.resthandler.model.PutSearchConfigurationRequest;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.opensearch.rest.RestRequest.Method.DELETE;
import static org.opensearch.rest.RestRequest.Method.GET;
import static org.opensearch.rest.RestRequest.Method.PUT;

public class SearchConfigurationRestHandler extends BaseRestHandler {
    private static final String BASE_URI = "/_plugins/_search_processor/search_configuration";
    private static final String SEARCH_CONFIGURATION_ACTION = "search_configuration_action";

    public static final String SEARCH_CONFIGURATION_NAME_FIELD = "searchConfigName";

    @Override
    public String getName() {
        return SEARCH_CONFIGURATION_ACTION;
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        return (it) -> {
            switch (request.method()) {
                case PUT:
                    client.execute(PutSearchConfigurationAction.ACTION_TYPE,
                            new PutSearchConfigurationRequest(request.param(SEARCH_CONFIGURATION_NAME_FIELD), nextToken(request.contentParser())),
                            new RestToXContentListener<>(it));
                    break;
                case GET:
                    client.execute(GetSearchConfigurationAction.ACTION_TYPE,
                            new GetSearchConfigurationRequest(request.param(SEARCH_CONFIGURATION_NAME_FIELD)),
                            new RestToXContentListener<>(it));
                    break;
            }
        };
    }

    @Override
    protected Set<String> responseParams() {
        return Set.of(SEARCH_CONFIGURATION_NAME_FIELD);
    }

    private static XContentParser nextToken(XContentParser parser) throws IOException {
        parser.nextToken();
        return parser;
    }

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(PUT,
                        BASE_URI + "/{" + SEARCH_CONFIGURATION_NAME_FIELD + "}"),
                new Route(GET,
                        BASE_URI + "/{" + SEARCH_CONFIGURATION_NAME_FIELD + "}"),
                new Route(DELETE,
                        BASE_URI + "/{" + SEARCH_CONFIGURATION_NAME_FIELD + "}")
        );
    }

}
