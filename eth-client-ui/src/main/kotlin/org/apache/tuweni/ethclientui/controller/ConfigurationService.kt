// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclientui.controller

import org.apache.tuweni.ethclient.EthereumClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rest/config")
class ConfigurationService {

  @Autowired
  var client: EthereumClient? = null

  @GetMapping(value = [""], produces = [MediaType.TEXT_PLAIN_VALUE])
  fun get(): String {
    return client?.config?.toToml() ?: ""
  }
}
