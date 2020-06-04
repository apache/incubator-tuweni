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
package org.apache.tuweni.rlpx;

import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;
import org.apache.tuweni.rlpx.wire.WireConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the wire connections repository.
 *
 */
public class MemoryWireConnectionsRepository implements WireConnectionRepository {

  private final Map<String, WireConnection> connections = new ConcurrentHashMap<>();

  @Override
  public void add(WireConnection wireConnection) {
    connections.put(wireConnection.id(), wireConnection);
  }

  @Override
  public WireConnection get(String id) {
    return connections.get(id);
  }

  @Override
  public Iterable<WireConnection> asIterable() {
    return connections.values();
  }

  @Override
  public Iterable<WireConnection> asIterable(SubProtocolIdentifier identifier) {
    return connections.values().stream().filter(conn -> conn.supports(identifier)).collect(Collectors.toList());
  }

  @Override
  public void close() {
    connections.clear();
  }

  public Map<String, WireConnection> asMap() {
    return connections;
  }
}
