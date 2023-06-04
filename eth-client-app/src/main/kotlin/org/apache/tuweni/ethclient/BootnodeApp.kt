// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclient

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.app.commons.ApplicationUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import picocli.CommandLine
import java.security.Security
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking {
  ApplicationUtils.renderBanner("Apache Tuweni bootnode loading")
  Security.addProvider(BouncyCastleProvider())
  val opts = CommandLine.populateCommand(BootnodeAppOptions(), *args)

  if (opts.help) {
    CommandLine(opts).usage(System.out)
    exitProcess(0)
  }
  if (opts.version) {
    println("Apache Tuweni Bootnode #{ApplicationUtils.version}")
    exitProcess(0)
  }

  val config = EthereumClientConfig.fromString(this.javaClass.getResource("/bootnode.toml")!!.readText())
  val ethClient = EthereumClient(Vertx.vertx(), config)
  Runtime.getRuntime().addShutdownHook(Thread { ethClient.stop() })
  ethClient.start()
}

class BootnodeAppOptions {
  @CommandLine.Option(names = ["-h", "--help"], description = ["Prints usage prompt"])
  var help: Boolean = false

  @CommandLine.Option(names = ["-v", "--version"], description = ["Prints version"])
  var version: Boolean = false
}
