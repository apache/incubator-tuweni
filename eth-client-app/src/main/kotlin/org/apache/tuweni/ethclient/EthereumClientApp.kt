// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclient

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.app.commons.ApplicationUtils
import org.apache.tuweni.crypto.blake2bf.TuweniProvider
import org.apache.tuweni.ethclientui.ClientUIApplication
import org.bouncycastle.jce.provider.BouncyCastleProvider
import picocli.CommandLine
import java.nio.file.Path
import java.security.Security
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = runBlocking {
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

  ClientUIApplication.start(networkInterface = host, port = port.toInt(), client = ethClient)
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
