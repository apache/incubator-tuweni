// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx;

import static org.apache.tuweni.rlpx.wire.WireConnection.Event.*;

import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;
import org.apache.tuweni.rlpx.wire.WireConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** In-memory implementation of the wire connections repository. */
public class MemoryWireConnectionsRepository implements WireConnectionRepository {

  private final Map<String, WireConnection> connections = new ConcurrentHashMap<>();

  private final List<Listener> connectionListeners = new ArrayList<>();

  private final List<Listener> disconnectionListeners = new ArrayList<>();

  @Override
  public String add(WireConnection wireConnection) {
    String id = UUID.randomUUID().toString();
    connections.put(id, wireConnection);
    wireConnection.registerListener(
        (event) -> {
          if (event == CONNECTED) {
            for (Listener listener : connectionListeners) {
              listener.connectionEvent(wireConnection);
            }
          } else if (event == DISCONNECTED) {
            connections.remove(id);
            for (Listener listener : disconnectionListeners) {
              listener.connectionEvent(wireConnection);
            }
          }
        });
    return id;
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
    return connections.values().stream()
        .filter(conn -> conn.supports(identifier))
        .collect(Collectors.toList());
  }

  @Override
  public void close() {
    connections.clear();
  }

  public Map<String, WireConnection> asMap() {
    return connections;
  }

  @Override
  public void addConnectionListener(Listener listener) {
    connectionListeners.add(listener);
  }

  @Override
  public void addDisconnectionListener(Listener listener) {
    disconnectionListeners.add(listener);
  }
}
