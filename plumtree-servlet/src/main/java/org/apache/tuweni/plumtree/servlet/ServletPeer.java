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
package org.apache.tuweni.plumtree.servlet;

import org.apache.tuweni.plumtree.Peer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;

public class ServletPeer implements Peer {

  private final String serverUrl;

  private final AtomicInteger errorsCounter = new AtomicInteger(0);

  public ServletPeer(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getAddress() {
    return serverUrl;
  }

  public AtomicInteger getErrorsCounter() {
    return errorsCounter;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ServletPeer that = (ServletPeer) o;
    return Objects.equals(serverUrl, that.serverUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serverUrl);
  }

  @Override
  public int compareTo(@NotNull Peer o) {
    return serverUrl.compareTo(((ServletPeer) o).serverUrl);
  }
}
