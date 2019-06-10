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
package org.apache.tuweni.relayer

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.hobbits.Relayer
import org.bouncycastle.jce.provider.BouncyCastleProvider
import picocli.CommandLine
import java.security.Security

internal class RelayerAppCommandlineArguments() {
  @CommandLine.Option(names = ["-b", "--bind"], description = ["Endpoint to bind to"]) var bind: String = ""
  @CommandLine.Option(names = ["-t", "--to"], description = ["Endpoint to relay to"]) var to: String = ""
}

/**
 * Runs a relayer between two hobbits endpoints.
 */
fun main(args: Array<String>) {
  Security.addProvider(BouncyCastleProvider())
  val opts = CommandLine.populateCommand<RelayerAppCommandlineArguments>(RelayerAppCommandlineArguments(), *args)
  val vertx = Vertx.vertx()
  val relayer = Relayer(vertx, opts.bind, opts.to, {
    System.out.println(it)
  })
  Runtime.getRuntime().addShutdownHook(Thread { relayer.stop()
    vertx.close() })
  runBlocking {
    relayer.start()
  }
}
