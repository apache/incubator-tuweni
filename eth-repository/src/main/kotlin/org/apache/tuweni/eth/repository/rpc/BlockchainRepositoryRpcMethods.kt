// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
