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
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.ToXContent;
import org.opensearch.common.xcontent.ToXContentObject;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.search.relevance.SearchRelevancePlugin;
import org.opensearch.search.relevance.configuration.SearchConfigurationExtBuilder;
import org.opensearch.search.relevance.resthandler.util.Helpers;

import java.io.IOException;

public class PutSearchConfigurationRequest extends ActionRequest implements ToXContentObject {

    private final String name;
    private final SearchConfigurationExtBuilder searchConfiguration;

    public PutSearchConfigurationRequest(String name, XContentParser parser) throws IOException {
        this.name = name;
        this.searchConfiguration = SearchConfigurationExtBuilder.parse(parser,
                SearchRelevancePlugin.getResultTransformerConfigFactoryMap());
    }

    public PutSearchConfigurationRequest(StreamInput input) throws IOException {
        this.name = input.readString();
        this.searchConfiguration = SearchConfigurationExtBuilder.parse(Helpers.createJsonParser(input),
                SearchRelevancePlugin.getResultTransformerConfigFactoryMap());
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return searchConfiguration.toXContent(builder, params);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        toXContent(XContentFactory.jsonBuilder(out), ToXContent.EMPTY_PARAMS);
    }

    public String getName() {
        return name;
    }

    public SearchConfigurationExtBuilder getSearchConfiguration() {
        return searchConfiguration;
    }
}
