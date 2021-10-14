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
package org.apache.tuweni.eth.crawler.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.concurrent.ExpiringMap
import org.apache.tuweni.devp2p.eth.logger
import org.apache.tuweni.devp2p.parseEnodeUri
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.crawler.RESTMetrics
import org.apache.tuweni.eth.crawler.RelationalPeerRepository
import java.net.URI
import javax.servlet.ServletContext
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("peers")
class PeersService {

  companion object {
    val mapper = ObjectMapper()

    init {
      mapper.registerModule(EthJsonModule())
    }
  }

  private val localCache = ExpiringMap<String, String>()

  @javax.ws.rs.core.Context
  var context: ServletContext? = null

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  fun getPeers(@QueryParam("from") from: Int = 0, @QueryParam("limit") limit: Int = 10): String {
    val key = "from${from}limit$limit"
    return localCache.computeIfAbsent(key, 30 * 1000L) {
      val repo = context!!.getAttribute("repo") as RelationalPeerRepository
      val metrics = context!!.getAttribute("metrics") as RESTMetrics
      metrics.peersCounter.add(1)
      val peers = repo.getPeersWithInfo(System.currentTimeMillis(), from, limit)
      mapper.writeValueAsString(peers)
    }
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{publickey}")
  fun getPeer(@PathParam("publickey") publicKeyOrUri: String): Response {
    val publicKey = try {
      val uri = URI.create(publicKeyOrUri)
      parseEnodeUri(uri).nodeId.toHexString()
    } catch (e: IllegalArgumentException) {
      publicKeyOrUri
    }
    val repo = context!!.getAttribute("repo") as RelationalPeerRepository
    val peer = repo.getPeerWithInfo(System.currentTimeMillis(), publicKey)
    if (peer != null) {
      return Response.ok(mapper.writeValueAsString(peer)).build()
    } else {
      return Response.status(Response.Status.NOT_FOUND).build()
    }
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/{enode}")
  fun addPeer(@PathParam("enode") uri: String): Response {
    val repo = context!!.getAttribute("repo") as RelationalPeerRepository
    try {
      repo.getAsync(uri)
      return Response.status(Response.Status.OK).build()
    } catch (e: IllegalArgumentException) {
      logger.error("Error adding peer", e)
      return Response.status(Response.Status.BAD_REQUEST).build()
    }
  }
}
