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

import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A scuttlebutt feed message
 */
@JsonDeserialize(using = FeedMessageDeserializer.class)
public class FeedMessage {

  private final String key;
  private final FeedValue value;
  private final Optional<String> type;

  /**
   *
   * @param key the ID of the message
   * @param type the type of the content (is Empty if unknown because the message is private and not decryptable.)
   * @param value the metadata and contents of the message.
   */
  public FeedMessage(String key, Optional<String> type, FeedValue value) {
    this.key = key;
    this.value = value;
    this.type = type;
  }

  public String getKey() {
    return key;
  }

  public Optional<String> getType() {
    return type;
  }

  public FeedValue getValue() {
    return value;
  }
}
