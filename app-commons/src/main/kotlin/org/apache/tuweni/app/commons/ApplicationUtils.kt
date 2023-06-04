// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
