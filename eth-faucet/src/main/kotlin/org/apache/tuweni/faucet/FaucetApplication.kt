// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.faucet

import io.vertx.core.Vertx
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.tuweni.wallet.Wallet
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Security

@SpringBootApplication
class FaucetApplication {

  @Value("\${banner}")
  var banner: String? = null

  @Value("\${auth.disabledOrg}")
  var disabledOrgMembership: Boolean = false

  @Value("\${auth.org}")
  var authorizedOrg: String? = null

  @Bean("wallet")
  fun createWallet(@Value("\${wallet.path}") path: String, @Value("\${wallet.password}") password: String): Wallet {
    val walletPath = Paths.get(path).toAbsolutePath()
    if (!Files.exists(walletPath)) {
      return Wallet.create(walletPath, password)
    }
    return Wallet.open(walletPath, password)
  }

  val vertx = Vertx.vertx()

  @Bean
  fun createVertx(): Vertx {
    return vertx
  }

  @Bean
  fun createWebClient(): WebClient {
    return WebClient.create()
  }

  @Bean
  fun oauth2UserService(rest: WebClient): OAuth2UserService<OAuth2UserRequest, OAuth2User>? {
    val delegate = DefaultOAuth2UserService()
    return OAuth2UserService { request: OAuth2UserRequest ->
      val user = delegate.loadUser(request)
      if (!disabledOrgMembership) {
        authorizedOrg?.let {
          val client = OAuth2AuthorizedClient(request.clientRegistration, user.name, request.accessToken)
          val url = user.getAttribute<String>("organizations_url")
          val orgs = rest
            .get().uri(url ?: "")
            .attributes(oauth2AuthorizedClient(client))
            .retrieve()
            .bodyToMono(MutableList::class.java)
            .block()
          val found = orgs?.stream()?.anyMatch { org ->
            authorizedOrg == (org as Map<*, *>)["login"]
          } ?: false
          if (!found) {
            throw OAuth2AuthenticationException(OAuth2Error("invalid_token", "Not in authorized team", ""))
          }
        }
      }
      user
    }
  }

  @PostConstruct
  fun banner() {
    banner?.let {
      println(it)
    }
  }

  @PreDestroy
  fun close() {
    vertx.close()
  }
}

fun main(args: Array<String>) {
  Security.addProvider(BouncyCastleProvider())
  runApplication<FaucetApplication>(*args)
}

@Component("htmlConfig")
class HtmlConfig() {
  @Value("\${html.title}")
  var title: String? = null

  @Value("\${html.request_message}")
  var requestMessage: String? = null

  @Value("\${html.address_help}")
  var addressHelp: String? = null

  @Value("\${html.submit_button}")
  var submitBtn: String? = null

  @Value("\${faucet.maxETH}")
  var maxETH: Long? = null
}
