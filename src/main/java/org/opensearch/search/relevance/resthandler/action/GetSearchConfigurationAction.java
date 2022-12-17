/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.resthandler.action;

import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.common.inject.Inject;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.relevance.client.SearchConfigurationsClient;
import org.opensearch.search.relevance.resthandler.model.GetSearchConfigurationRequest;
import org.opensearch.search.relevance.resthandler.model.GetSearchConfigurationResponse;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import java.io.IOException;

public class GetSearchConfigurationAction extends SearchConfigurationBaseAction<GetSearchConfigurationRequest, GetSearchConfigurationResponse> {
    public static final String NAME = "cluster:admin/plugin/search_configuration/get";

    public static final ActionType<GetSearchConfigurationResponse> ACTION_TYPE = new ActionType<>(NAME, GetSearchConfigurationResponse::new);

    @Inject
    public GetSearchConfigurationAction(TransportService transportService, ActionFilters actionFilters,
                                        SearchConfigurationsClient client) {
        super(NAME, transportService, client, actionFilters, GetSearchConfigurationRequest::new);
    }

    @Override
    protected void doExecute(Task task, GetSearchConfigurationRequest request, ActionListener<GetSearchConfigurationResponse> listener) {
        client.getSearchConfigurationAsync(request.getName(), ActionListener.wrap(r -> {
                    if (r != null) {
                        listener.onResponse(new GetSearchConfigurationResponse(r));
                    } else {
                        listener.onFailure(new OpenSearchStatusException("Search configuration " + request.getName() + " not found", RestStatus.NOT_FOUND));
                    }
                },
                listener::onFailure));
    }
}
