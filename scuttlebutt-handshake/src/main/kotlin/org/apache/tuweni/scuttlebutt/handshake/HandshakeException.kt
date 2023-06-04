// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.handshake

/**
 * Exceptions thrown during handshake because of invalid messages or different network identifiers.
 */
class HandshakeException internal constructor(message: String) : RuntimeException(message)
