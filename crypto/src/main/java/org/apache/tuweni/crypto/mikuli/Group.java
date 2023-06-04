// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.mikuli;

/**
 * Group is an interface that define the allowed mathematical operators
 */
interface Group<G> {

  G add(G g);

  G mul(Scalar scalar);
}
