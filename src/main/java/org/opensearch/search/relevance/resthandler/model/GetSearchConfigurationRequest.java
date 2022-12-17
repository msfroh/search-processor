/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.resthandler.model;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.xcontent.ToXContentObject;
import org.opensearch.common.xcontent.XContentParser;

import java.io.IOException;

public class GetSearchConfigurationRequest extends ActionRequest  {
    private final String name;

    public GetSearchConfigurationRequest(String name) {
        this.name = name;
    }

    public GetSearchConfigurationRequest(StreamInput input) throws IOException {
        this.name = input.readString();
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    public String getName() {
        return name;
    }
}
