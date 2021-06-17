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
package org.apache.tuweni.eth.crawler

import io.swagger.v3.jaxrs2.integration.OpenApiServlet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlets.DoSFilter
import org.eclipse.jetty.util.resource.Resource
import org.glassfish.jersey.servlet.ServletContainer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.EnumSet
import javax.servlet.DispatcherType
import kotlin.coroutines.CoroutineContext

class CrawlerRESTService(
  val port: Int = 0,
  val networkInterface: String = "127.0.0.1",
  val path: String = "/",
  val repository: RelationalPeerRepository,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {

  companion object {
    internal val logger = LoggerFactory.getLogger(CrawlerRESTService::class.java)
  }

  private var server: Server? = null

  var actualPort: Int? = null

  fun start() = async {
    val newServer = Server(InetSocketAddress(networkInterface, port))

    val ctx = ServletContextHandler(ServletContextHandler.NO_SESSIONS)

    ctx.contextPath = path
    newServer.handler = ctx

    val serHol = ctx.addServlet(ServletContainer::class.java, "/rest/*")
    serHol.initOrder = 1
    serHol.setInitParameter(
      "jersey.config.server.provider.packages",
      "org.apache.tuweni.eth.crawler.rest"
    )

    val apiServlet = ctx.addServlet(OpenApiServlet::class.java, "/api/*")
    apiServlet.setInitParameter("openApi.configuration.resourcePackages", "org.apache.tuweni.eth.crawler.rest")
    apiServlet.initOrder = 2

    ctx.setBaseResource(Resource.newResource(CrawlerRESTService::class.java.getResource("/webapp")))
    val staticContent = ctx.addServlet(DefaultServlet::class.java, "/*")
    ctx.setWelcomeFiles(arrayOf("index.html"))
    staticContent.initOrder = 10

    val filter = DoSFilter()
    // TODO make it a config setting. Change for REST vs UI.
    filter.maxRequestsPerSec = 30
    ctx.addFilter(FilterHolder(filter), "/*", EnumSet.of(DispatcherType.REQUEST))

    newServer.stopAtShutdown = true
    newServer.start()
    serHol.servlet.servletConfig.servletContext.setAttribute("repo", repository)
    server = newServer
    actualPort = newServer.uri.port
    logger.info("REST service started on ${newServer.uri}")
  }

  fun stop() = async {
    server?.stop()
  }
}
