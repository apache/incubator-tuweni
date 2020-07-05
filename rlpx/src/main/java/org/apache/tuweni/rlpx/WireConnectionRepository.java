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

import javax.annotation.Nullable;

/**
 * A repository managing wire connections.
 *
 */
public interface WireConnectionRepository {

  /**
   * Adds a new wire connection to the repository.
   * 
   * @param wireConnection the new wire connection
   * @return the id of the connection
   */
  String add(WireConnection wireConnection);

  /**
   * Gets a wire connection by its identifier, as provided by
   * <code>org.apache.tuweni.rlpx.wire.DefaultWireConnection#id</code>
   * 
   * @param id the identifier of the wire connection
   * @return the wire connection associated with the identifier, or <code>null</code> if no such wire connection exists.
   */
  @Nullable
  WireConnection get(String id);

  /**
   * Provides a view of the wire connections as an iterable. There is no guarantee of sorting wire connections.
   *
   * @return an Iterable object allowing to traverse all wire connections held by this repository
   */
  Iterable<WireConnection> asIterable();

  /**
   * Provides a subset of wire connections with a particular capabilities.
   *
   * @return an Iterable object allowing to traverse all wire connections held by this repository
   */
  Iterable<WireConnection> asIterable(SubProtocolIdentifier identifier);

  /**
   * Closes the repository. After it has been closed, the repository should no longer be able to add or retrieve
   * connections.
   *
   */
  void close();
}
