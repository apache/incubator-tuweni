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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.stream.IntStream
import java.util.stream.LongStream
import kotlin.coroutines.CoroutineContext

fun main(args: Array<String>) {
  run(arrayOf("localhost", "9000", "46", "1000"))
}

fun run(args: Array<String>) {
  val target = InetSocketAddress(InetAddress.getByName(args[0]), args[1].toInt())

  println("Starting")
  val random = SecureRandom()
  val messages = (0..100).map { Bytes.random(46, random) }
  IntStream.range(0, 1000).parallel().forEach {
    println("Creating executor $it")
    val scope = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    val sender = Sender(10000 + it, args[3].toLong(), scope)

    sender.send(target, messages)
  }
  runBlocking {
    delay(10000)
  }
}

class Sender(val port: Int, val rounds: Long, override val coroutineContext: CoroutineContext = Dispatchers.Default) :
  CoroutineScope {

  fun send(target: SocketAddress, messages: List<Bytes>) =
    LongStream.range(0, rounds).forEach {
      launch {
        if (it % 10L == 0L) {
          println("Launching $port round $it")
        }
        launch {
          if (it % 10L == 0L) {
            println("Prepared $port round $it")
          }
          val index = (it % 100).toInt()
          val toSend = DatagramPacket(messages.get(index).toArrayUnsafe(), 46, target)
          val ds = DatagramSocket()
          ds.send(toSend)
          ds.close()
          if (it % 10L == 0L) {
            println("Sent $port round $it")
          }
        }
      }
    }
}
