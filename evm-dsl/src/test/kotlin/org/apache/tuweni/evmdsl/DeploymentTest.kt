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
package org.apache.tuweni.evmdsl

import org.apache.tuweni.bytes.Bytes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeploymentTest {

  @Test
  fun testDeploymentCreation() {
    val deployment = Deployment(Code.read(Bytes.fromHexString("0x600035f660115760006000526001601ff35b60016000526001601ff3")))
    assertEquals(Bytes.fromHexString("0x7f600035f660115760006000526001601ff35b60016000526001601ff300000000600052601c6000f3"), deployment.toBytes())
  }
}
