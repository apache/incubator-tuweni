// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.crawler.rest

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server

@OpenAPIDefinition(
  info = Info(
    title = "Apache Tuweni Network Crawler",
    version = "1.0.0",
    description = "REST API for the Ethereum network crawler"
  ),
  servers = arrayOf(Server(url = "/api"))
)
class Metadata
