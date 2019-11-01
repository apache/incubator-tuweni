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
package org.apache.tuweni.devp2p.v5.topic

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.ExpiringMap

class TicketHolder {

  private val ticketKeys: ExpiringMap<Bytes, Bytes> = ExpiringMap() // requestId to signing key
  private val selfTickets: MutableMap<Bytes, Bytes> = hashMapOf() // requestId to ticket


  fun putKey(requestId: Bytes, key: Bytes, expiry: Long) {
    ticketKeys.put(requestId, key, expiry)
  }

  fun getKey(requestId: Bytes): Bytes? = ticketKeys[requestId]

  fun removeKey(requestId: Bytes): Bytes? = ticketKeys.remove(requestId)


  fun putSelfTicket(requestId: Bytes, ticket: Bytes) {
    selfTickets[requestId] = ticket
  }

  fun getSelfTicket(requestId: Bytes): Bytes? = selfTickets[requestId]

  fun removeSelfTicket(requestId: Bytes): Bytes? = selfTickets.remove(requestId)

}
