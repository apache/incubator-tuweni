// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.jsonrpc.downloader

import org.apache.tuweni.config.Configuration
import org.apache.tuweni.config.SchemaBuilder
import java.nio.file.Path

class DownloaderConfig(filePath: Path? = null, configContents: String? = null) {

  companion object {

    fun schema() = SchemaBuilder.create()
      .addInteger("numberOfThreads", 10, "Number of threads for each thread pool", null)
      .addString("outputPath", "", "Path to output block data", null)
      .addInteger("start", 0, "First block to scrape", null)
      .addInteger("end", null, "Last block to scrape. If unset, the scrape will continue to ask for new blocks", null)
      .addString("url", null, "URL of the JSON-RPC service to query for information", null)
      .toSchema()
  }

  val config = if (filePath != null) {
    Configuration.fromToml(filePath, schema())
  } else if (configContents != null) {
    Configuration.fromToml(configContents)
  } else {
    Configuration.empty(schema())
  }

  fun numberOfThreads() = config.getInteger("numberOfThreads")
  fun outputPath() = config.getString("outputPath")
  fun start() = config.getInteger("start")
  fun end() = config.getInteger("end")
  fun url() = config.getString("url")
}
