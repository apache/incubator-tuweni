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

/**
 * A repository managing wire connections.
 *
 */
public interface WireConnectionRepository {

  /**
   * Adds a new wire connection to the repository.
   * 
   * @param wireConnection the new wire connection
   */
  void add(WireConnection wireConnection);

  /**
   * Gets a wire connection by its identifier, as provided by
   * <code>net.consensys.cava.rlpx.wire.DefaultWireConnection#id</code>
   * 
   * @param id the identifier of the wire connection
   * @return the wire connection associated with the identifier, or <code>null</code> if no such wire connection exists.
   */
  WireConnection get(String id);

  /**
   * Provides a view of the wire connections as an iterable. There is no guarantee of sorting wire connections.
   *
   * @return an Iterable object allowing to traverse all wire connections held by this repository
   */
  Iterable<WireConnection> asIterable();

  /**
   * Closes the repository. After it has been closed, the repository should no longer be able to add or retrieve
   * connections.
   *
   */
  void close();
}
