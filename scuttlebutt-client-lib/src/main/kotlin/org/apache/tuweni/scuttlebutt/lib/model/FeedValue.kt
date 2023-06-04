// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException

/**
 * The metadata and contents of a message
 *
 * @param previous the ID of the previous message on the append only feed, prior to this one
 * @param author the author of the posted message
 * @param sequence the sequence number of this message
 * @param timestamp the time the client asserts that the message was posted at
 * @param hash the hash type of the message
 * @param content the content of the message
*/
class FeedValue(
  val previous: String,
  val author: Author,
  val sequence: Long,
  val timestamp: Long,
  val hash: String,
  private val content: JsonNode
) {

  companion object {
    private val mapper = ObjectMapper()
  }

  /**
   * The message content as a JSON string.
   *
   * @return the contents of the message as a JSON string.
   *
   * @throws JsonProcessingException if the contents could not be serialized to JSON
   */
  @get:Throws(JsonProcessingException::class)
  val contentAsJsonString: String
    get() = mapper.writeValueAsString(content)

  /**
   * The message content, deserialized to a Java class.
   *
   * @param mapper the mapper instance to use to deserialize the content
   * @param clazz the class to deserialize the content to
   * @param <T> the type to deserialize the content to
   * @return the deserialized content
   * @throws IOException if the content could not successfully be deserialized.
   </T> */
  @Throws(IOException::class)
  fun <T> getContentAs(mapper: ObjectMapper, clazz: Class<T>?): T {
    return mapper.readValue(contentAsJsonString, clazz)
  }
}
