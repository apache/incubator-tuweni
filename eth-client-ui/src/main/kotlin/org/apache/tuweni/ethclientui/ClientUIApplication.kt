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
package org.apache.tuweni.ethclientui

import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.ethclient.EthereumClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.support.GenericApplicationContext
import java.net.InetAddress

@SpringBootApplication
open class ClientUIApplication(@Autowired val config: Config) {

  companion object {
    fun start(
      client: EthereumClient,
      port: Int = 0,
      networkInterface: String = "127.0.0.1",
      contextPath: String = ""
    ): ConfigurableApplicationContext {
      val parentContext = GenericApplicationContext()
      parentContext.beanFactory.registerSingleton("ethereumClient", client)
      parentContext.beanFactory.registerSingleton("config", Config(port, networkInterface, contextPath))

      val springApp =
        SpringApplicationBuilder(ClientUIApplication::class.java)
          .web(WebApplicationType.SERVLET)
          .parent(parentContext)
          .build()
      springApp.setRegisterShutdownHook(true)
      parentContext.refresh()
      return springApp.run()
    }
  }

  @Bean
  open fun jsonCustomizer(): Jackson2ObjectMapperBuilderCustomizer? {
    return Jackson2ObjectMapperBuilderCustomizer { builder ->
      builder.modules(EthJsonModule())
    }
  }

  @Bean
  open fun servletWebServerFactory(): ServletWebServerFactory {
    return TomcatServletWebServerFactory()
  }

  @Bean
  open fun webServerFactoryCustomizer(): WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    return WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> { factory ->
      run {
        factory.setContextPath(config.contextPath)
        factory.setPort(config.port)
        factory.setAddress(InetAddress.getByName(config.networkInterface))
      }
    }
  }
}

data class Config(val port: Int, val networkInterface: String, val contextPath: String)
