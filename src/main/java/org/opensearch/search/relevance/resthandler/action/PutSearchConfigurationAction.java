/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.resthandler.action;

import org.opensearch.action.ActionListener;
import org.opensearch.action.ActionType;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.common.inject.Inject;
import org.opensearch.search.relevance.client.SearchConfigurationsClient;
import org.opensearch.search.relevance.resthandler.model.PutSearchConfigurationRequest;
import org.opensearch.search.relevance.resthandler.model.PutSearchConfigurationResponse;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

public class PutSearchConfigurationAction extends SearchConfigurationBaseAction<PutSearchConfigurationRequest, PutSearchConfigurationResponse> {
    public static final String NAME = "cluster:admin/plugin/search_configuration/put";

    public static final ActionType<PutSearchConfigurationResponse> ACTION_TYPE = new ActionType<>(NAME, PutSearchConfigurationResponse::new);

    @Inject
    public PutSearchConfigurationAction(TransportService transportService, ActionFilters actionFilters,
                                           SearchConfigurationsClient client) {
        super(NAME, transportService, client, actionFilters, PutSearchConfigurationRequest::new);
    }

    @Override
    protected void doExecute(Task task, PutSearchConfigurationRequest request, ActionListener<PutSearchConfigurationResponse> listener) {
        client.putSearchConfigurationAsync(request.getName(), request.getSearchConfiguration(), ActionListener.wrap(
                r -> listener.onResponse(new PutSearchConfigurationResponse(r)),
                listener::onFailure
        ));
    }
}
