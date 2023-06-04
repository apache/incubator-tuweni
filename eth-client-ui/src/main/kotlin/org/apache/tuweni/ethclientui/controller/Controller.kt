// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethclientui.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class Controller {

  @GetMapping("/")
  fun index(): String {
    return "index"
  }

  @GetMapping("/config")
  fun config(): String {
    return "config"
  }

  @GetMapping("/state")
  fun state(): String {
    return "state"
  }
}
