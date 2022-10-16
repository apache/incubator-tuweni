/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.lib.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import java.io.IOException
import java.util.Optional

class FeedMessageDeserializer : JsonDeserializer<FeedMessage>() {
  @Throws(IOException::class, JsonProcessingException::class)
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): FeedMessage {
    val node = jp.codec.readTree<JsonNode>(jp)
    val key = node["key"].asText()
    val value = node["value"]
    val content = value["content"]
    val type = getType(content)
    return FeedMessage(key, type, toFeedValue(value))
  }

  private fun toFeedValue(value: JsonNode): FeedValue {
    val content = value["content"]
    val previous = value["previous"].asText()
    val sequence = value["sequence"].asLong()
    val authorString = value["author"].asText()
    val author = Author(authorString)
    val timestamp = value["timestamp"].asLong()
    val hash = value["hash"].asText()
    return FeedValue(previous, author, sequence, timestamp, hash, content)
  }

  private fun getType(content: JsonNode): Optional<String> {
    return if (content.nodeType != JsonNodeType.STRING) {
      val type = content["type"]
      Optional.of(type.asText())
    } else {
      Optional.empty()
    }
  }
}
