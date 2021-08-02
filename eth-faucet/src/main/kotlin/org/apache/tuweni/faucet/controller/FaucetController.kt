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
package org.apache.tuweni.faucet.controller

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.Address
import org.apache.tuweni.jsonrpc.ClientRequestException
import org.apache.tuweni.jsonrpc.JSONRPCClient
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.apache.tuweni.wallet.Wallet
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import java.net.ConnectException
import javax.annotation.PostConstruct

val logger = LoggerFactory.getLogger("faucet")

@Controller
class FaucetController(@Autowired val vertx: Vertx, @Autowired val wallet: Wallet) {

  var jsonrpcClient: JSONRPCClient? = null

  @Value("\${faucet.chainId}")
  var chainId: Int? = null

  @Value("\${faucet.gasPrice}")
  var gasPrice: Long? = null

  @Value("\${faucet.gas}")
  var gas: Long? = null

  @Value("\${faucet.maxETH}")
  var maxETH: Long? = null

  @Value("\${faucet.rpcPort}")
  var rpcPort: Int? = null

  @Value("\${faucet.rpcHost}")
  var rpcHost: String? = null

  @PostConstruct
  fun createClient() {
    jsonrpcClient = JSONRPCClient(vertx, "http://$rpcHost:$rpcPort")
  }

  @GetMapping("/")
  fun index(model: Model): String {
    model.addAttribute("faucetRequest", FaucetRequest("", null, ""))
    return "index"
  }

  @PostMapping("/")
  fun send(@ModelAttribute request: FaucetRequest, model: Model): String {
    model.addAttribute("faucetRequest", request)
    val addr: Address
    try {
      addr = Address.fromHexString(request.addr ?: "")
    } catch (e: IllegalArgumentException) {
      request.message = e.message ?: "Invalid address"
      request.alertClass = "alert-danger"
      return "index"
    }
    try {
      return runBlocking {
        // check if the address has more than the maxETH. If it does, we don't need to send money there.
        val balance = jsonrpcClient!!.getBalance_latest(addr)
        val lessThanMax = Wei.fromEth(maxETH!!).compareTo(balance) == 1
        if (!lessThanMax) {
          request.message = "Balance is more than this faucet gives."
          request.alertClass = "alert-primary"
          return@runBlocking "index"
        }
        val missing = Wei.fromEth(maxETH!!).subtract(balance)
        val nonce = jsonrpcClient!!.getTransactionCount_latest(wallet.address())
        // Otherwise, send money with the faucet account.
        logger.info("Sending $missing to $addr")
        val tx = wallet.sign(
          nonce,
          Wei.valueOf(gasPrice!!),
          Gas.valueOf(gas!!),
          addr,
          missing,
          Bytes.EMPTY,
          chainId!!
        )

        logger.info("Transaction ready to send")
        val txHash = jsonrpcClient!!.sendRawTransaction(tx)
        logger.info("Transaction sent to client with hash $txHash")
        request.message = "Transaction hash: $txHash"
        request.alertClass = "alert-success"
        return@runBlocking "index"
      }
    } catch (e: ClientRequestException) {
      request.message = e.message
      request.alertClass = "alert-danger"
      return "index"
    } catch (e: ConnectException) {
      request.message = "Could not connect to a client. Try again later."
      request.alertClass = "alert-danger"
      return "index"
    }
  }
}
