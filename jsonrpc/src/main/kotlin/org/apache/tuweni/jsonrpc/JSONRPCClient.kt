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
package org.apache.tuweni.jsonrpc

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.client.WebClient
import kotlinx.coroutines.CompletableDeferred
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.units.bigints.UInt256
import java.io.Closeable

val mapper = ObjectMapper()

/**
 * JSON-RPC client to send requests to an Ethereum client.
 */
class JSONRPCClient(
  vertx: Vertx,
  val serverPort: Int,
  val serverHost: String
) : Closeable {

  val client = WebClient.create(vertx)

  /**
   * Sends a signed transaction to the Ethereum network.
   * @param tx the transaction object to send
   * @return the hash of the transaction, or an empty string if the hash is not available yet.
   * @throws ClientRequestException if the request is rejected
   * @throws ConnectException if it cannot dial the remote client
   */
  suspend fun sendRawTransaction(tx: Transaction): String {
    val body = mapOf(
      Pair("jsonrpc", "2.0"),
      Pair("method", "eth_sendRawTransaction"),
      Pair("id", 1),
      Pair("params", listOf(tx.toBytes().toHexString()))
    )
    val deferred = CompletableDeferred<String>()

    client.post(serverPort, serverHost, "/")
      .putHeader("Content-Type", "application/json")
      .sendBuffer(Buffer.buffer(mapper.writeValueAsBytes(body))) { response ->
        if (response.failed()) {
          deferred.completeExceptionally(response.cause())
        } else {
          val jsonResponse = response.result().bodyAsJsonObject()
          if (jsonResponse.containsKey("error")) {
            val err = jsonResponse.getJsonObject("error")
            val errorMessage = "Code ${err.getInteger("code")}: ${err.getString("message")}"
            deferred.completeExceptionally(ClientRequestException(errorMessage))
          } else {
            deferred.complete(jsonResponse.getString("result"))
          }
        }
      }

    return deferred.await()
  }

  /**
   * Gets the account balance.
   * @param tx the transaction object to send
   * @return the hash of the transaction, or an empty string if the hash is not available yet.
   * @throws ClientRequestException if the request is rejected
   * @throws ConnectException if it cannot dial the remote client
   */
  suspend fun getBalance_latest(address: Address): UInt256 {
    val body = mapOf(
      Pair("jsonrpc", "2.0"),
      Pair("method", "eth_getBalance"),
      Pair("id", 1),
      Pair("params", listOf(address.toHexString(), "latest"))
    )
    val deferred = CompletableDeferred<UInt256>()

    client.post(serverPort, serverHost, "/")
      .putHeader("Content-Type", "application/json")
      .sendBuffer(Buffer.buffer(mapper.writeValueAsBytes(body))) { response ->
        if (response.failed()) {
          deferred.completeExceptionally(response.cause())
        } else {
          val jsonResponse = response.result().bodyAsJsonObject()
          deferred.complete(UInt256.fromHexString(jsonResponse.getString("result")))
        }
      }

    return deferred.await()
  }

  /**
   * Gets the number of transactions sent from an address.
   * @param tx the transaction object to send
   * @return the hash of the transaction, or an empty string if the hash is not available yet.
   * @throws ClientRequestException if the request is rejected
   * @throws ConnectException if it cannot dial the remote client
   */
  suspend fun getTransactionCount_latest(address: Address): UInt256 {
    val body = mapOf(
      Pair("jsonrpc", "2.0"),
      Pair("method", "eth_getTransactionCount"),
      Pair("id", 1),
      Pair("params", listOf(address.toHexString(), "latest"))
    )
    val deferred = CompletableDeferred<UInt256>()

    client.post(serverPort, serverHost, "/")
      .putHeader("Content-Type", "application/json")
      .sendBuffer(Buffer.buffer(mapper.writeValueAsBytes(body))) { response ->
        if (response.failed()) {
          deferred.completeExceptionally(response.cause())
        } else {
          val jsonResponse = response.result().bodyAsJsonObject()
          deferred.complete(UInt256.fromHexString(jsonResponse.getString("result")))
        }
      }

    return deferred.await()
  }

  override fun close() {
    client.close()
  }
}
