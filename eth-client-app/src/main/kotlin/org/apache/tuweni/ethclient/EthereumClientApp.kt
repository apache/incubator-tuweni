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
package org.apache.tuweni.ethclient

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.app.commons.ApplicationUtils
import org.apache.tuweni.crypto.blake2bf.TuweniProvider
import org.apache.tuweni.ethclientui.UI
import org.bouncycastle.jce.provider.BouncyCastleProvider
import picocli.CommandLine
import java.nio.file.Path
import java.security.Security
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking {
  ApplicationUtils.renderBanner("Apache Tuweni client loading")
  Security.addProvider(BouncyCastleProvider())
  Security.addProvider(TuweniProvider())
  val opts = CommandLine.populateCommand(AppOptions(), *args)

  if (opts.help) {
    CommandLine(opts).usage(System.out)
    exitProcess(0)
  }
  if (opts.version) {
    println("Apache Tuweni #{ApplicationUtils.version}")
    exitProcess(0)
  }

  val config = EthereumClientConfig.fromFile(opts.configPath)
  val ethClient = EthereumClient(Vertx.vertx(), config)
  Runtime.getRuntime().addShutdownHook(Thread(Runnable { ethClient.stop() }))
  ethClient.start()
  val (host, port) = opts.web!!.split(":")
  val ui = UI(networkInterface = host, port = port.toInt(), client = ethClient)
  Runtime.getRuntime().addShutdownHook(Thread(Runnable { ui.stop() }))
  ui.start()
}

class AppOptions {
  @CommandLine.Option(names = ["-c", "--config"], description = ["Configuration file."])
  var configPath: Path? = null

  @CommandLine.Option(names = ["-h", "--help"], description = ["Prints usage prompt"])
  var help: Boolean = false

  @CommandLine.Option(names = ["-v", "--version"], description = ["Prints version"])
  var version: Boolean = false

  @CommandLine.Option(names = ["-w", "--web"], description = ["Web console host:port"], defaultValue = "127.0.0.1:8080")
  var web: String? = null
}
