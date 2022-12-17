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
import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.search.relevance.client.SearchConfigurationsClient;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

public abstract class SearchConfigurationBaseAction<Request extends ActionRequest, Response extends ActionResponse>
        extends HandledTransportAction<Request, Response> {
    private static final ForkJoinPool MY_WORKER_POOL = new ForkJoinPool();
    protected final SearchConfigurationsClient client;

    protected SearchConfigurationBaseAction(String actionName,
                                            TransportService transportService,
                                            SearchConfigurationsClient client,
                                            ActionFilters actionFilters,
                                            Writeable.Reader<Request> requestReader) {
        super(actionName, transportService, actionFilters, requestReader);
        this.client = client;
    }
}
