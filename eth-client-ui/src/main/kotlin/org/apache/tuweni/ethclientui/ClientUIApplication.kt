// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
