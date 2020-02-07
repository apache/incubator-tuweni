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

final class NodeInfo {

  static final String CLIENT_VERSION = "0.1.0";

  private final String name;
  private final String node;
  private final int port;
  private final String network;
  private final String protocol;
  private final String api = "No";
  private final String os;
  private final String osVer;
  private final String client = CLIENT_VERSION;
  private final boolean history = true;

  NodeInfo(String name, String node, int port, String network, String protocol, String os, String osVer) {
    this.name = name;
    this.node = node;
    this.port = port;
    this.network = network;
    this.protocol = protocol;
    this.os = os;
    this.osVer = osVer;
  }

  @JsonGetter("name")
  public String getName() {
    return name;
  }

  @JsonGetter("node")
  public String getNode() {
    return node;
  }

  @JsonGetter("port")
  public int getPort() {
    return port;
  }

  @JsonGetter("net")
  public String getNetwork() {
    return network;
  }

  @JsonGetter("protocol")
  public String getProtocol() {
    return protocol;
  }

  @JsonGetter("api")
  public String getAPI() {
    return api;
  }

  @JsonGetter("os")
  public String getOS() {
    return os;
  }

  @JsonGetter("os_v")
  public String getOSVersion() {
    return osVer;
  }

  @JsonGetter("client")
  public String getClient() {
    return client;
  }

  @JsonGetter("canUpdateHistory")
  public boolean canUpdateHistory() {
    return history;
  }
}
