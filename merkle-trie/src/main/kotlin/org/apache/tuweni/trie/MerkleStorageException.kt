// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.trie

/**
 * This exception is thrown when there is an issue retrieving or decoding values from [MerkleStorage].
 */
class MerkleStorageException : RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   * The cause is not initialized, and may subsequently be initialized by a
   * call to {@link #initCause}.
   *
   * @param message The detail message.
   */
  constructor(message: String) : super(message)

  /**
   * Constructs a new exception with the specified detail message and
   * cause.
   *
   * @param message The detail message.
   * @param cause The cause.
   */
  constructor(message: String, cause: Exception) : super(message, cause)
}
