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
import org.bouncycastle.jce.provider.BouncyCastleProvider
import picocli.CommandLine
import java.security.Security
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking {

  Security.addProvider(BouncyCastleProvider())
  val opts = CommandLine.populateCommand(EthereumClientConfig(), *args)
  try {
    opts.validate()
  } catch (e: IllegalArgumentException) {
    System.err.println("Invalid configuration detected.\n\n" + e.message)
    CommandLine(opts).usage(System.out)
    exitProcess(1)
  }

  if (opts.help) {
    CommandLine(opts).usage(System.out)
    exitProcess(0)
  }

  val ethClient = EthereumClient(Vertx.vertx(), opts)
  Runtime.getRuntime().addShutdownHook(Thread(Runnable({ ethClient.stop() })))
  ethClient.start()
}
