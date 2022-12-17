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
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.xcontent.ToXContent;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentParserUtils;

import java.io.IOException;

/**
 * <code>
 * {
 * "success": true
 * }
 * </code>
 */
public class PutSearchConfigurationResponse extends SearchConfigurationBaseResponse {
    private static final String SUCCESS_FIELD = "success";
    private final boolean success;

    public PutSearchConfigurationResponse(boolean success) {
        this.success = success;
    }

    public PutSearchConfigurationResponse(StreamInput in) throws IOException {
        this(in.readBoolean());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeBoolean(success);
    }

    public static PutSearchConfigurationResponse parse(XContentParser parser) throws IOException {
        XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.currentToken(), parser);
        Boolean success = null;
        while (XContentParser.Token.END_OBJECT != parser.nextToken()) {
            String fieldName = parser.currentName();
            parser.nextToken();
            if (SUCCESS_FIELD.equals(fieldName)) {
                success = parser.booleanValue();
            } else {
                parser.skipChildren();
            }
        }
        return new PutSearchConfigurationResponse(Boolean.TRUE.equals(success));
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return builder.startObject()
                .field(SUCCESS_FIELD, success)
                .endObject();
    }
}
