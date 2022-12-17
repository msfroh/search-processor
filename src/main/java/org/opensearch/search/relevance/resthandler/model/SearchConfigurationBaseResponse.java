/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.resthandler.model;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.xcontent.ToXContentObject;
import org.opensearch.rest.RestStatus;

public abstract class SearchConfigurationBaseResponse extends ActionResponse implements ToXContentObject {
    public RestStatus getStatus() {
        return RestStatus.OK;
    }
}
