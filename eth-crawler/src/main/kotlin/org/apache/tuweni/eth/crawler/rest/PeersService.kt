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
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.crawler.RelationalPeerRepository
import javax.servlet.ServletContext
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

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
    return localCache.computeIfAbsent(key, 30 * 1000) {
      val repo = context!!.getAttribute("repo") as RelationalPeerRepository
      val peers = repo.getPeersWithInfo(System.currentTimeMillis(), from, limit)
      mapper.writeValueAsString(peers)
    }
  }
}
