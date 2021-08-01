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
package org.apache.tuweni.app.commons

import java.nio.charset.StandardCharsets
import java.util.Properties

/**
 * Utilities to display versions and banners in programs.
 */
object ApplicationUtils {

  val version: String
  init {
    val infoProperties = Properties()
    infoProperties.load(ApplicationUtils::class.java.classLoader.getResourceAsStream("project-info.properties"))
    version = infoProperties["version"].toString()
  }

  fun renderBanner(message: String) {
    val asciiart =
      ApplicationUtils::class.java.getResourceAsStream("/tuweni.txt").readAllBytes().toString(StandardCharsets.UTF_8)
    if (System.console() != null) {
      asciiart.split("\n").forEachIndexed { lineNo, line ->
        if (lineNo % 2 == 0) {
          println("\u001b[5;37m$line\u001b[0m")
        } else {
          println(line)
        }
      }
    }
    println(message)
  }
}
