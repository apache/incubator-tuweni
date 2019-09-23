/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.lib.model;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class FeedMessageDeserializer extends JsonDeserializer<FeedMessage> {
  @Override
  public FeedMessage deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
      JsonProcessingException {

    JsonNode node = jp.getCodec().readTree(jp);

    String key = node.get("key").asText();
    JsonNode value = node.get("value");

    JsonNode content = value.get("content");
    Optional<String> type = getType(content);

    return new FeedMessage(key, type, toFeedValue(value));
  }

  private FeedValue toFeedValue(JsonNode value) {

    JsonNode content = value.get("content");
    String previous = value.get("previous").asText();
    long sequence = value.get("sequence").asLong();

    String authorString = value.get("author").asText();
    Author author = new Author(authorString);

    long timestamp = value.get("timestamp").asLong();
    String hash = value.get("hash").asText();

    return new FeedValue(previous, author, sequence, timestamp, hash, content);
  }

  private Optional<String> getType(JsonNode content) {

    if (content.getNodeType() != JsonNodeType.STRING) {
      JsonNode type = content.get("type");
      return Optional.of(type.asText());
    } else {
      return Optional.empty();
    }
  }
}
