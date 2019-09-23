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

public class Peer {

  private String state;
  private String address;
  private int port;
  private String key;

  public Peer() {}

  /**
   *
   * @param address the address of the peer used to connect to it
   * @param port the port of the peer
   * @param key the public key of the peer
   * @param state the connection state of the peer
   */
  public Peer(String address, int port, String key, String state) {
    this.address = address;
    this.port = port;
    this.key = key;
    this.state = state;
  }

  public String getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }

  public String getKey() {
    return key;
  }

  public String getState() {
    if (state == null) {
      return "unknown";
    } else {
      return state;
    }
  }
}
