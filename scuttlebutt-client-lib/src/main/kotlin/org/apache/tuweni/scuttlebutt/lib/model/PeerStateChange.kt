// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model

/**
 * A change in peer state.
 *
 * @param type the state change since the previous state
 * @param peer the new peer details
 */
data class PeerStateChange(val peer: Peer, val type: String)
