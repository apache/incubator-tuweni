// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.crawler.rest

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.ServletContext
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.crawler.RESTMetrics
import org.apache.tuweni.eth.crawler.StatsJob
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("clients")
class ClientsService {

  companion object {
    val mapper = ObjectMapper()

    init {
      mapper.registerModule(EthJsonModule())
    }
  }

  @javax.ws.rs.core.Context
  var context: ServletContext? = null

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("all")
  fun getClientIds(): String {
    val stats = context!!.getAttribute("stats") as StatsJob
    val metrics = context!!.getAttribute("metrics") as RESTMetrics
    metrics.clientsCounter.add(1)
    val peers = stats.getClientIds()
    val result = mapper.writeValueAsString(peers)
    return result
  }

  @Path("{upgrade}/stats")
  fun getClientStats(@PathParam("upgrade") upgrade: String): String {
    val stats = context!!.getAttribute("stats") as StatsJob
    val peers = stats.getUpgradeStats()[upgrade]
    val result = mapper.writeValueAsString(peers)
    return result
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("stats")
  fun getClientStats(): String {
    val statsjob = context!!.getAttribute("stats") as StatsJob
    val stats = statsjob.getClientStats()
    val result = mapper.writeValueAsString(stats)
    return result
  }
}
