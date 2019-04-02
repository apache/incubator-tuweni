/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.rlpx;

import net.consensys.cava.rlpx.wire.WireConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
  public void close() {
    connections.clear();
  }

  public Map<String, WireConnection> asMap() {
    return connections;
  }
}
