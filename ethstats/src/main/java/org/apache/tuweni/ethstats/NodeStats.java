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
package org.apache.tuweni.ethstats;

import com.fasterxml.jackson.annotation.JsonGetter;

public final class NodeStats {
  private final boolean active;
  private final boolean syncing;
  private final boolean mining;
  private final int hashrate;
  private final int peerCount;
  private final int gasPrice;
  private final int uptime;


  public NodeStats(
      boolean active,
      boolean syncing,
      boolean mining,
      int hashrate,
      int peerCount,
      int gasPrice,
      int uptime) {
    this.active = active;
    this.syncing = syncing;
    this.mining = mining;
    this.hashrate = hashrate;
    this.peerCount = peerCount;
    this.gasPrice = gasPrice;
    this.uptime = uptime;
  }

  @JsonGetter("active")
  public boolean isActive() {
    return active;
  }

  @JsonGetter("syncing")
  public boolean isSyncing() {
    return syncing;
  }

  @JsonGetter("mining")
  public boolean isMining() {
    return mining;
  }

  @JsonGetter("hashrate")
  public int getHashrate() {
    return hashrate;
  }

  @JsonGetter("peers")
  public int getPeerCount() {
    return peerCount;
  }

  @JsonGetter("gasPrice")
  public int getGasPrice() {
    return gasPrice;
  }

  @JsonGetter("uptime")
  public int getUptime() {
    return uptime;
  }
}
