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
