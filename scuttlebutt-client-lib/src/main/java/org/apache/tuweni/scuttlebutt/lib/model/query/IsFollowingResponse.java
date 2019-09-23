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
package org.apache.tuweni.scuttlebutt.lib.model.query;

public class IsFollowingResponse {

  private String destination;
  private String source;
  private boolean following;

  public IsFollowingResponse() {}

  /**
   * A response to a query on whether 'source' is following 'destination'
   *
   * @param source the source node
   * @param destination the destination node
   * @param following true if source is following destination, false otherwise
   */
  public IsFollowingResponse(String source, String destination, boolean following) {
    this.source = source;
    this.destination = destination;
    this.following = following;
  }

  public String getDestination() {
    return destination;
  }

  public String getSource() {
    return source;
  }

  public boolean isFollowing() {
    return following;
  }
}
