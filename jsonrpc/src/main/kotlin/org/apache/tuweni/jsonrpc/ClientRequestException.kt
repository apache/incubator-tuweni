// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.jsonrpc

/**
 * Exception thrown when a JSON-RPC request is denied.
 */
class ClientRequestException(message: String) : RuntimeException(message)
