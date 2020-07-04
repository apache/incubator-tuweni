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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.tuweni.rlpx.wire.WireConnection;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

class MemoryWireConnectionsRepositoryTest {

  @Test
  void testAddAndGet() {
    MemoryWireConnectionsRepository repo = new MemoryWireConnectionsRepository();
    WireConnection conn = mock(WireConnection.class);
    String id = repo.add(conn);
    assertEquals(conn, repo.get(id));
  }

  @Test
  void testIterable() {
    MemoryWireConnectionsRepository repo = new MemoryWireConnectionsRepository();
    WireConnection conn = mock(WireConnection.class);
    repo.add(conn);
    Iterator<WireConnection> iter = repo.asIterable().iterator();
    assertTrue(iter.hasNext());
    assertEquals(conn, iter.next());
    assertFalse(iter.hasNext());
  }

}
