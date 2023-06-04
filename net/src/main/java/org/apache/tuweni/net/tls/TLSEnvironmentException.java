// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.tls;

final class TLSEnvironmentException extends RuntimeException {

  TLSEnvironmentException(String message) {
    super(message);
  }

  TLSEnvironmentException(String message, Throwable cause) {
    super(message, cause);
  }
}
