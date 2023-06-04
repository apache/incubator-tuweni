// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.crawler

import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.Meter
import io.swagger.v3.jaxrs2.integration.OpenApiServlet
import jakarta.servlet.DispatcherType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.eclipse.jetty.servlets.DoSFilter
import org.eclipse.jetty.util.resource.Resource
import org.glassfish.jersey.servlet.ServletContainer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.EnumSet
import kotlin.coroutines.CoroutineContext

class CrawlerRESTService(
  val port: Int = 0,
  val networkInterface: String = "127.0.0.1",
  val path: String = "/",
  val maxRequestsPerSec: Int = 30,
  val allowedOrigins: String = "*",
  val allowedMethods: String = "*",
  val allowedHeaders: String = "*",
  val repository: RelationalPeerRepository,
  val stats: StatsJob,
  val meter: Meter,
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
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
      "org.apache.tuweni.eth.crawler.rest",
    )

    val apiServlet = ctx.addServlet(OpenApiServlet::class.java.name, "/api/*")
    apiServlet.setInitParameter(
      "openApi.configuration.resourcePackages",
      "org.apache.tuweni.eth.crawler.rest",
    )
    apiServlet.initOrder = 2

    ctx.setBaseResource(Resource.newResource(CrawlerRESTService::class.java.getResource("/webapp")))
    val staticContent = ctx.addServlet(DefaultServlet::class.java, "/*")
    ctx.setWelcomeFiles(arrayOf("index.html"))
    staticContent.initOrder = 10

    val swagger = ServletHolder("swagger-ui", DefaultServlet::class.java)
    swagger.setInitParameter(
      "resourceBase",
      CrawlerRESTService::class.java.getClassLoader().getResource("META-INF/resources/webjars/swagger-ui/4.18.2/")
        .toString(),
    )
    swagger.setInitParameter("pathInfoOnly", "true")
    ctx.addServlet(swagger, "/swagger-ui/*")

    val filter = DoSFilter()
    filter.maxRequestsPerSec = maxRequestsPerSec
    ctx.addFilter(FilterHolder(filter), "/*", EnumSet.of(DispatcherType.REQUEST))
    val corsFilter = CrossOriginFilter()
    val corsFilterHolder = FilterHolder(corsFilter)
    corsFilterHolder.setInitParameter("allowedOrigins", allowedOrigins)
    corsFilterHolder.setInitParameter("allowedMethods", allowedMethods)
    corsFilterHolder.setInitParameter("allowedHeaders", allowedHeaders)
    ctx.addFilter(corsFilterHolder, "/*", EnumSet.of(DispatcherType.REQUEST))

    newServer.stopAtShutdown = true
    newServer.start()
    serHol.servlet.servletConfig.servletContext.setAttribute("repo", repository)
    serHol.servlet.servletConfig.servletContext.setAttribute("stats", stats)
    val restMetrics = RESTMetrics(
      meter.counterBuilder("peers").setDescription("Number of times peers have been requested").build(),
      meter.counterBuilder("clients").setDescription("Number of times client stats have been requested").build(),
    )
    serHol.servlet.servletConfig.servletContext.setAttribute("metrics", restMetrics)
    server = newServer
    actualPort = newServer.uri.port
    logger.info("REST service started on ${newServer.uri}")
  }

  fun stop() = async {
    server?.stop()
  }
}

data class RESTMetrics(val peersCounter: LongCounter, val clientsCounter: LongCounter)
