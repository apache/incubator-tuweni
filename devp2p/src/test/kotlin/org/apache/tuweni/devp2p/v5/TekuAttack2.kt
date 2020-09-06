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

import org.apache.tuweni.bytes.Bytes
import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

fun main(args: Array<String>) {
  run2(arrayOf("localhost", "9000"))
}

fun run2(args: Array<String>) {
  val target = InetSocketAddress(InetAddress.getByName(args[0]), args[1].toInt())
  val message =
    Bytes.fromHexString("0x968973076c2c5db4900594290e483bae3f236424531e984ae728ecec4e9c0b678c91f90e323557075403f" +
      "e6f5913c6bd7afacd231bac885d5586c3d5442ff66294ded7bf6eee093b89d09eef38b81bc5065c9ee7d3578cee43")
  val toSend = DatagramPacket(message.toArrayUnsafe(), message.size(), target)
  val ds = DatagramSocket()
  val message2 =
    Bytes.fromHexString("0x985b7d2f13aed28796758045f9016a8c5aa678ced5a054a1dfb3132ea01cbb69d10c087693b306b1" +
      "d488b3eec0ff53e48eddc74305325cb52665eabd288367636db8402c86edb358b75a84bed5e3b18f4fdb7ecf063d97530cfd84" +
      "55bd6d73eaa9ae35eeb05c676db03e2a4565f8b914aefb5a5f2f538641fbd540c27a49606df9ec84b8f40000000c00000000000000" +
      "00000000009cfc658c9d41570b515f1d11cd0f40f6982078b9f2433948ef1293d457c8b8df46dbdc5063c84f6896de35b7f888ce0d31" +
      "4a42e0c1d13335ebefe4943f925e682711d4cbfc081a5244bd3fe96c00e13148778f345a171f3286fb94206a6c213db45c915701d9f6" +
      "46974218f82955c70f997908be0d07aea207ea32bf0623621a86b60391fd0eb4b5950fa9d7acbd15c38dbbae4ae1c9dbe7310e8aee094" +
      "20669af436543bfbef477ad729a7d3011cec9118181e4867e2b6e0b2a1cc072e0d5c5b106f9410ecd541b264ad97548374cf231fbc630" +
      "49f703ed8c31203451ef0b8aa1dca3750000000c5aa678ced5a054a1dfb3132ebd1448d8d5e0b8fdaef828862741118713a70dc671f0" +
      "c9c865617a32")
  val toSend2 = DatagramPacket(message2.toArrayUnsafe(), message2.size(), target)
  ds.send(toSend)
  sleep(500)
  ds.send(toSend2)
  ds.close()
}
