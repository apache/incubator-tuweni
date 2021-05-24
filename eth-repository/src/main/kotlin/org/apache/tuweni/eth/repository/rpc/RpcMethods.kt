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
package org.apache.tuweni.eth.repository.rpc

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.invalidParams
import org.apache.tuweni.eth.repository.BlockchainRepository

class BlockchainRepositoryRpcMethods(val repository: BlockchainRepository) {

  fun getMostRecentBlockNumber(request: JSONRPCRequest): JSONRPCResponse {
    if (request.params.isNotEmpty()) {
      return invalidParams.copy(id = request.id)
    }
    return runBlocking {
      val number = repository.retrieveChainHead().header.number
      return@runBlocking JSONRPCResponse(id = request.id, result = number.toHexString())
    }
  }
}
