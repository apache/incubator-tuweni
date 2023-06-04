// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.evmdsl

import org.apache.tuweni.bytes.Bytes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeploymentTest {

  @Test
  fun testDeploymentCreation() {
    val deployment = Deployment(
      Code.read(Bytes.fromHexString("0x600035f660115760006000526001601ff35b60016000526001601ff3")),
    )
    assertEquals(
      Bytes.fromHexString("0x7f600035f660115760006000526001601ff35b60016000526001601ff300000000600052601c6000f3"),
      deployment.toBytes(),
    )
  }
}
