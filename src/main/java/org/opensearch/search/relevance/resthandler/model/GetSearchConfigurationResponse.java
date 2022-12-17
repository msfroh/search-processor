/*
 * SPDX-License-Identifier: Apache-2.0
 *
 *  The OpenSearch Contributors require contributions made to
 *  this file be licensed under the Apache-2.0 license or a
 *  compatible open source license.
 *
 */

package org.opensearch.search.relevance.resthandler.model;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.ToXContent;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.search.relevance.SearchRelevancePlugin;
import org.opensearch.search.relevance.configuration.SearchConfigurationExtBuilder;
import org.opensearch.search.relevance.resthandler.util.Helpers;

import java.io.IOException;

public class GetSearchConfigurationResponse extends SearchConfigurationBaseResponse {
    private final SearchConfigurationExtBuilder searchConfiguration;

    public GetSearchConfigurationResponse(SearchConfigurationExtBuilder searchConfiguration) {
        this.searchConfiguration = searchConfiguration;
    }

    public GetSearchConfigurationResponse(XContentParser xContentParser) throws IOException {
        this.searchConfiguration = SearchConfigurationExtBuilder.parse(xContentParser,
                SearchRelevancePlugin.getResultTransformerConfigFactoryMap());
    }

    public GetSearchConfigurationResponse(StreamInput input) throws IOException {
        this(Helpers.createJsonParser(input));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        toXContent(XContentFactory.jsonBuilder(out), ToXContent.EMPTY_PARAMS);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        searchConfiguration.toXContent(builder, params);
        return builder.endObject();
    }
}
