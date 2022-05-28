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
import kotlin.reflect.KClass

/**
 * An EVM instruction. It is made of an opcode, optionally followed by bytes to be consumed by the opcode execution.
 */
interface Instruction {

  fun toBytes(): Bytes
}

data class InstructionModel(val opcode: Byte, val additionalBytesToRead: Int = 0, val instructionClass: KClass<out Instruction>)

/**
 * A registry of instructions that can be used to read code back into the DSL.
 */
object InstructionRegistry {
  val opcodes: Map<Byte, InstructionModel> = buildMap {
    for (i in 1..32) {
      val b = (0x60 + i - 1).toByte()
      this.put(b, InstructionModel(b, i, Push::class))
    }
  }
}

class Push(val bytesToPush: Bytes) : Instruction {

  init {
    if (bytesToPush.size() > 32) {
      throw IllegalArgumentException("Push can push at most 32 bytes, ${bytesToPush.size()} provided")
    }
    if (bytesToPush.isEmpty) {
      throw IllegalArgumentException("Push requires at least one byte")
    }
  }

  override fun toBytes(): Bytes = Bytes.wrap(Bytes.of((0x60 + bytesToPush.size() - 1).toByte()), bytesToPush)
}
