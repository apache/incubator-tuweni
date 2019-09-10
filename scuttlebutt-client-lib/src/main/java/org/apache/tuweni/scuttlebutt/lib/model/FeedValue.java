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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The metadata and contents of a message
 */
public class FeedValue {

  private String previous;
  private Author author;
  private long sequence;
  private long timestamp;
  private String hash;

  private JsonNode content;

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   *
   * @param previous the ID of the previous message on the append only feed, prior to this one
   * @param author the author of the posted message
   * @param sequence the sequence number of this message
   * @param timestamp the time the client asserts that the message was posted at
   * @param hash the hash type of the message
   * @param content the content of the message
   */
  public FeedValue(String previous, Author author, long sequence, long timestamp, String hash, JsonNode content) {
    this.previous = previous;
    this.author = author;
    this.sequence = sequence;
    this.timestamp = timestamp;
    this.hash = hash;
    this.content = content;
  }

  public String getPrevious() {
    return previous;
  }

  public Author getAuthor() {
    return author;
  }

  public long getSequence() {
    return sequence;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getHash() {
    return hash;
  }

  /**
   * The message content as a JSON string.
   *
   * @return the contents of the message as a JSON string.
   *
   * @throws JsonProcessingException if the contents could not be serialized to JSON
   */
  public String getContentAsJsonString() throws JsonProcessingException {
    return mapper.writeValueAsString(content);
  }

  /**
   * The message content, deserialized to a Java class.
   *
   * @param mapper the mapper instance to use to deserialize the content
   * @param clazz the class to deserialize the content to
   * @param <T> the type to deserialize the content to
   * @return the deserialized content
   * @throws IOException if the content could not successfully be deserialized.
   */
  public <T> T getContentAs(ObjectMapper mapper, Class<T> clazz) throws IOException {
    return mapper.readValue(getContentAsJsonString(), clazz);
  }

}
