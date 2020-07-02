package org.apache.tuweni.ethclient

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import picocli.CommandLine
import java.nio.file.Path
import java.security.Security

fun main(args: Array<String>) = runBlocking {

  Security.addProvider(BouncyCastleProvider())
  val opts = CommandLine.populateCommand(EthereumClientConfig(), *args)
  try {
    opts.validate()
  } catch (e: IllegalArgumentException) {
    System.err.println("Invalid configuration detected.\n\n" + e.message)
    CommandLine(opts).usage(System.out)
    System.exit(1)
  }

  if (opts.help) {
    CommandLine(opts).usage(System.out)
    System.exit(0)
  }

  val ethClient = EthereumClient(Vertx.vertx(), opts)
  Runtime.getRuntime().addShutdownHook(Thread(Runnable({ ethClient.stop() })))
  ethClient.start()
}

class EthereumClientAppOptions {

}
