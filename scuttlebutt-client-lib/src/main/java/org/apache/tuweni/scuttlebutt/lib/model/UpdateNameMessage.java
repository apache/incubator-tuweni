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

/**
 * A message that when persisted to the feed updates the name of the given user
 */
public class UpdateNameMessage implements ScuttlebuttMessageContent {

  private String about;
  public String type = "about";
  public String name = "name";

  public UpdateNameMessage() {}

  /**
   *
   * @param name the new name for the user
   * @param about the public key that the new name should be applied to
   */
  public UpdateNameMessage(String name, String about) {
    this.name = name;
    this.about = about;
  }

  public String getAbout() {
    return about;
  }

  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return type;
  }
}
