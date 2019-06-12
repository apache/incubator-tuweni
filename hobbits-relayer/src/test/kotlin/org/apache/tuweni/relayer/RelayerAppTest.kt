/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.relayer

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.hobbits.HobbitsTransport
import org.apache.tuweni.hobbits.Message
import org.apache.tuweni.hobbits.Transport
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(VertxExtension::class)
class RelayerAppTest {

  @Test
  fun testRelayerApp(@VertxInstance vertx: Vertx) {
    val ref = AtomicReference<Message>()
    val client1 = HobbitsTransport(vertx)
    val client2 = HobbitsTransport(vertx)
    RelayerApp.main(arrayOf("-b", "tcp://localhost:12000", "-t", "tcp://0.0.0.0:10000"))
    runBlocking {
      client1.createTCPEndpoint("foo", port = 10000, handler = ref::set)
      client1.start()
      client2.start()
      client2.sendMessage(
        Message(command = "WHO", body = Bytes.fromHexString("deadbeef"), headers = Bytes.random(16)),
        Transport.TCP,
        "localhost",
        12000
      )
    }
    Thread.sleep(1000)
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), ref.get().body)
    client1.stop()
    client2.stop()
  }
}
