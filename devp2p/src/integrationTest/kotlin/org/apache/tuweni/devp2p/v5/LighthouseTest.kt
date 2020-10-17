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
package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.io.Base64URLSafe
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors

/**
 * Test a developer can run from their machine to contact a remote server.
 */
@ExtendWith(BouncyCastleExtension::class)
class LighthouseTest {

  @Test
  fun testConnect() = runBlocking {
    //    val enrRec =
//    "-Iu4QBpvavRrxplH2BS-XHJuMadzLfSItBRiw6kn5oc0eKkXAP1-4INi6waV1VEdludig-dr1Kh8o8eD6Hv0TWFD3goBgmlkgnY0gmlwhMCoWOyJc2VjcDI1NmsxoQMumeQTiLkWJyhFVxrPNhShEIdo8SukU-bIC0MLC185DIN0Y3CCIyiDdWRwgiMo"

    //val enrPrysm = "-Ku4QLglCMIYAgHd51uFUqejD9DWGovHOseHQy7Od1SeZnHnQ3fSpE4_nbfVs8lsy8uF07ae7IgrOOUFU0NFvZp5D4wBh2F0dG5ldHOIAAAAAAAAAACEZXRoMpAYrkzLAAAAAf__________gmlkgnY0gmlwhBLf22SJc2VjcDI1NmsxoQJxCnE6v_x2ekgY_uoE1rtwzvGy40mq9eD66XfHPBWgIIN1ZHCCD6A"
    val enr = "-KG4QFuKQ9eeXDTf8J4tBxFvs3QeMrr72mvS7qJgL9ieO6k9Rq5QuGqtGK4VlXMNHfe34Khhw427r7peSoIbGcN91fUDhGV0aDKQD8XYjwAAAAH__________4JpZIJ2NIJpcIQDhMExiXNlY3AyNTZrMaEDESplmV9c2k73v0DjxVXJ6__2bWyP-tK28_80lf7dUhqDdGNwgiMog3VkcIIjKA"
    val coroutineContext = Executors.newFixedThreadPool(100).asCoroutineDispatcher()
    val service = DiscoveryService.open(
      coroutineContext = coroutineContext,
      keyPair = SECP256K1.KeyPair.random(),
      localPort = 0,
      bindAddress = InetSocketAddress("0.0.0.0", 10000),
      bootstrapENRList = emptyList()
    )
    service.start().join()
    //service.addPeer(EthereumNodeRecord.fromRLP(Base64URLSafe.decode(enrPrysm))).join()
    service.addPeer(EthereumNodeRecord.fromRLP(Base64URLSafe.decode(enr))).join()
    // now go over the csv file:
    val path = Paths.get(System.getProperty("user.home")).resolve("medalla3.csv")
    val lines = Files.readAllLines(Paths.get(System.getProperty("user.home")).resolve("medalla3.csv"))
    var notBreak = true
    while(notBreak) {

      for (i in 0..10) {
        val line = lines.toSet().iterator().next()
        val enrStr = line.split(",").last()
        if (enrStr.isNotEmpty()) {
          service.addPeer(EthereumNodeRecord.fromRLP(Base64URLSafe.decode(enrStr))).join()
        }
      }


      //delay(10000)

      var newPeersDetected = true
      val nodes = HashSet<EthereumNodeRecord>()

      while (newPeersDetected) {
        newPeersDetected = false
        for (i in 0..256) {
          service.requestNodes(i).thenAccept {
            if (it.isNotEmpty()) {
              println("Found ${it.size} nodes")
            }
            for (node in it) {
              if (nodes.add(node)) {
                val str = node.ip().hostAddress + "," + node.udp() + "," + node.data["attnets"] + "," + Base64URLSafe.encode(node.toRLP()) + "\n"
                Files.writeString(path, str, StandardOpenOption.APPEND)
                newPeersDetected = true
                launch {
                  service.addPeer(node)
                }
              }
            }
          }
        }
        delay(20000)
      }

      nodes.forEach { node ->
        println(node.ip().hostAddress + "," + node.udp() + "," + node.data["attnets"] + "," + Base64URLSafe.encode(node.toRLP()))
      }
    }
    service.terminate()
  }
}
