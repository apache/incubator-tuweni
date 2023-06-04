// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model

/**
 *
 * @param address the address of the peer used to connect to it
 * @param port the port of the peer
 * @param key the public key of the peer
 * @param state the connection state of the peer
 */
data class Peer(val address: String, val port: Int, val key: String, val state: String? = "unknown")
