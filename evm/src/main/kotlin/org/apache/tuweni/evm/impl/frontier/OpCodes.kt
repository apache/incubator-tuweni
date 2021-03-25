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

package org.apache.tuweni.evm.impl.frontier

import org.apache.tuweni.evm.EVMExecutionStatusCode
import org.apache.tuweni.evm.HostContext
import org.apache.tuweni.evm.impl.GasManager
import org.apache.tuweni.evm.impl.Opcode
import org.apache.tuweni.evm.impl.Stack

val add = Opcode { gasManager, _, stack ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    EVMExecutionStatusCode.STACK_UNDERFLOW
  } else {
    stack.push(item.add(item2))
    EVMExecutionStatusCode.SUCCESS
  }
}
