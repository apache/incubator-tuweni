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

public class IsFollowingQuery {

  private String source;
  private String dest;

  public IsFollowingQuery() {}

  /**
   * A query body to check whether the 'source' is following the 'destination' user.
   *
   * @param source the source to check
   * @param dest the target node
   */
  public IsFollowingQuery(String source, String dest) {
    this.source = source;
    this.dest = dest;
  }

  public String getSource() {
    return source;
  }

  public String getDest() {
    return dest;
  }
}
