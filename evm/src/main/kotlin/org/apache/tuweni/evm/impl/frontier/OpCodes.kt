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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.eth.Address
import org.apache.tuweni.evm.CallKind
import org.apache.tuweni.evm.EVMExecutionStatusCode
import org.apache.tuweni.evm.EVMMessage
import org.apache.tuweni.evm.impl.Opcode
import org.apache.tuweni.evm.impl.Result
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei

private val add = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.add(item2))
    Result()
  }
}

private val addmod = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(8L)
  val operand1 = stack.pop()
  val operand2 = stack.pop()
  val mod = stack.pop()
  if (null == operand1 || null == operand2 || null == mod) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(if (mod.isZero) UInt256.ZERO else operand1.addMod(operand2, mod))
    Result()
  }
}

private val not = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  if (null == item) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.not())
    Result()
  }
}

private val eq = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    if (item.equals(item2)) {
      stack.push(UInt256.ONE)
    } else {
      stack.push(UInt256.ZERO)
    }
    Result()
  }
}

private val lt = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    if (item.lessThan(item2)) {
      stack.push(UInt256.ONE)
    } else {
      stack.push(UInt256.ZERO)
    }
    Result()
  }
}

private val slt = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    if (item.toSignedBigInteger() < item2.toSignedBigInteger()) {
      stack.push(UInt256.ONE)
    } else {
      stack.push(UInt256.ZERO)
    }
    Result()
  }
}

private val gt = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    if (item.greaterThan(item2)) {
      stack.push(UInt256.ONE)
    } else {
      stack.push(UInt256.ZERO)
    }
    Result()
  }
}

private val sgt = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    if (item.toSignedBigInteger() > item2.toSignedBigInteger()) {
      stack.push(UInt256.ONE)
    } else {
      stack.push(UInt256.ZERO)
    }
    Result()
  }
}

private val isZero = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  if (null == item) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(if (item.isZero) UInt256.ONE else UInt256.ZERO)
    Result()
  }
}

private val and = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.and(item2))
    Result()
  }
}

private val pop = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(2L)
  stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  Result()
}

private val or = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.or(item2))
    Result()
  }
}

private val xor = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.xor(item2))
    Result()
  }
}

private val byte = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val offset = stack.pop()
  val stackElement = stack.pop()
  if (null == offset || null == stackElement) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    if (!offset.fitsInt() || offset.intValue() >= 32) {
      stack.push(UInt256.ZERO)
    } else {
      stack.push(Bytes32.leftPad(Bytes.of(stackElement.get(offset.intValue()))))
    }
    Result()
  }
}

private val mul = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(5L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.multiply(item2))
    Result()
  }
}

private val mod = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(5L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.mod0(item2))
    Result()
  }
}

private val smod = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(5L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.smod0(item2))
    Result()
  }
}

private val mulmod = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(8L)
  val item = stack.pop()
  val item2 = stack.pop()
  val item3 = stack.pop()
  if (null == item || null == item2 || null == item3) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    if (item3.isZero) {
      stack.push(Bytes32.ZERO)
    } else {
      stack.push(item.multiplyMod(item2, item3))
    }
    Result()
  }
}

private val sub = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.subtract(item2))
    Result()
  }
}

private val exp = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  val number = stack.pop()
  val power = stack.pop()
  if (null == number || null == power) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    val numBytes = (power.bitLength() + 7) / 8

    val cost = (Gas.valueOf(10).multiply(Gas.valueOf(numBytes.toLong())))
      .add(Gas.valueOf(10))
    gasManager.add(cost)

    val result: UInt256 = number.pow(power)

    stack.push(result)
    Result()
  }
}

private val div = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(5L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(if (item2.isZero) UInt256.ZERO else item.divide(item2))
    Result()
  }
}

fun push(length: Int): Opcode {
  return Opcode { gasManager, _, stack, _, code, currentIndex, _, _ ->
    gasManager.add(3)
    val minLength = Math.min(length, code.size() - currentIndex)
    stack.push(code.slice(currentIndex, minLength))
    Result(newCodePosition = currentIndex + minLength)
  }
}

fun dup(index: Int): Opcode {
  return Opcode { gasManager, _, stack, _, _, _, _, _ ->
    gasManager.add(3)
    val value = stack.get(index - 1) ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
    stack.push(value)
    Result()
  }
}

fun swap(index: Int): Opcode {
  return Opcode { gasManager, _, stack, _, _, _, _, _ ->
    gasManager.add(3L)
    val eltN = stack.get(index) ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
    val elt0 = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
    stack.push(eltN)
    stack.set(index, elt0)
    Result()
  }
}

private val sstore = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  val key = stack.pop()
  val value = stack.pop()
  if (null == key || null == value) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }

  val address = msg.destination

  val currentValue = hostContext.getStorage(address, key) ?: UInt256.ZERO

  val cost = if (!value.isZero && currentValue.isZero) Gas.valueOf(20000) else Gas.valueOf(5000)
  gasManager.add(cost)

  hostContext.setStorage(address, key, value.toMinimalBytes())

  Result()
}

private val sload = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  val key = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)

  val address = msg.destination
  gasManager.add(50)

  stack.push(hostContext.getStorage(address, key) ?: UInt256.ZERO)

  Result()
}

private val stop = Opcode { gasManager, _, _, _, _, _, _, _ ->
  gasManager.add(0L)
  Result(EVMExecutionStatusCode.SUCCESS)
}

private val invalid = Opcode { _, _, _, _, _, _, _, _ ->
  Result(EVMExecutionStatusCode.INVALID_INSTRUCTION)
}

private val retuRn = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop()
  val length = stack.pop()
  if (null == location || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val memoryCost = memoryCost(memory.newSize(location, length).subtract(memory.size()))
  gasManager.add(memoryCost)
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  val output = memory.read(location, length)
  Result(EVMExecutionStatusCode.SUCCESS, output = output)
}

private val address = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(msg.destination))
  Result()
}

private val origin = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(msg.sender))
  Result()
}

private val caller = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(msg.sender))
  Result()
}

private val callvalue = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(msg.value))
  Result()
}

private val balance = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->

  val address = stack.pop()?.slice(12, 20)?.let { Address.fromBytes(it) }
    ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  gasManager.add(20)

  stack.push(hostContext.getBalance(address))

  Result()
}

private val pc = Opcode { gasManager, _, stack, _, _, currentIndex, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(currentIndex.toLong() - 1))
  Result()
}

private val gasPrice = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(hostContext.getGasPrice()))
  Result()
}

private val gas = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(gasManager.gasLeft().toLong()))
  Result()
}

private val coinbase = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(hostContext.getCoinbase()))
  Result()
}

private val gasLimit = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(hostContext.getGasLimit()))
  Result()
}

private val difficulty = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(hostContext.getDifficulty())
  Result()
}

private val number = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(hostContext.getBlockNumber())
  Result()
}

private val blockhash = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(20)
  val number = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  stack.push(UInt256.fromBytes(hostContext.getBlockHash(number)))

  Result()
}

private val codesize = Opcode { gasManager, _, stack, _, code, _, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(code.size().toLong()))
  Result()
}

private val timestamp = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(hostContext.timestamp())
  Result()
}

fun memoryCost(length: UInt256): Gas {
  val base = length.multiply(length).divide(UInt256.valueOf(512))
  return Gas.valueOf(UInt256.valueOf(3).multiply(length).add(base))
}

private val codecopy = Opcode { gasManager, _, stack, _, code, _, memory, _ ->
  val memOffset = stack.pop()
  val sourceOffset = stack.pop()
  val length = stack.pop()
  if (null == memOffset || null == sourceOffset || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val numWords: UInt256 = length.divideCeil(Bytes32.SIZE.toLong())
  val copyCost = Gas.valueOf(3).multiply(Gas.valueOf(numWords)).addSafe(Gas.valueOf(3))
  val memoryCost = memoryCost(memory.newSize(memOffset, length).subtract(memory.size()))

  gasManager.add(copyCost.addSafe(memoryCost))
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }

  memory.write(memOffset, sourceOffset, length, code)

  Result()
}

private val extcodecopy = Opcode { gasManager, hostContext, stack, _, _, _, memory, _ ->
  val address = stack.pop()
  val memOffset = stack.pop()
  val sourceOffset = stack.pop()
  val length = stack.pop()
  if (null == address || null == memOffset || null == sourceOffset || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val numWords: UInt256 = length.divideCeil(Bytes32.SIZE.toLong())
  val copyCost = Gas.valueOf(3).multiply(Gas.valueOf(numWords)).add(Gas.valueOf(20))
  val memoryCost = memoryCost(memory.newSize(memOffset, length).subtract(memory.size()))

  gasManager.add(copyCost.addSafe(memoryCost))
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }

  val code = hostContext.getCode(Address.fromBytes(address.slice(12, 20)))
  memory.write(memOffset, sourceOffset, length, code)

  Result()
}

private val returndatasize = Opcode { gasManager, _, stack, _, _, _, _, callResult ->
  gasManager.add(3)
  stack.push(UInt256.valueOf(callResult?.output?.size()?.toLong() ?: 0L))
  Result()
}

private val returndatacopy = Opcode { gasManager, _, stack, _, _, _, memory, callResult ->
  val memOffset = stack.pop()
  val sourceOffset = stack.pop()
  val length = stack.pop()
  val returnData = callResult?.output ?: Bytes.EMPTY
  if (null == memOffset || null == sourceOffset || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val numWords: UInt256 = length.divideCeil(Bytes32.SIZE.toLong())
  val copyCost = Gas.valueOf(3).multiply(Gas.valueOf(numWords)).add(Gas.valueOf(3))
  val memoryCost = memoryCost(memory.newSize(memOffset, length).subtract(memory.size()))
  gasManager.add(copyCost.addSafe(memoryCost))
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }

  memory.write(memOffset, sourceOffset, length, returnData)

  Result()
}

private val mstore = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop()
  val value = stack.pop()
  if (null == location || null == value) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val pre = memoryCost(memory.size())
  val post = memoryCost(memory.newSize(location, UInt256.valueOf(32)))
  val memoryCost = post.subtract(pre)
  gasManager.add(Gas.valueOf(3L).add(memoryCost))

  memory.write(location, UInt256.ZERO, UInt256.valueOf(32), value)
  Result()
}

private val mstore8 = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop()
  val value = stack.pop()
  if (null == location || null == value) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val pre = memoryCost(memory.size())
  val post: Gas = memoryCost(memory.newSize(location, UInt256.valueOf(1)))
  val memoryCost = post.subtract(pre)
  gasManager.add(Gas.valueOf(3L).add(memoryCost))

  memory.write(location, UInt256.ZERO, UInt256.valueOf(1), value.slice(31, 1))
  Result()
}

private val mload = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val memoryCost = memoryCost(memory.newSize(location, UInt256.valueOf(32)).subtract(memory.size()))
  gasManager.add(Gas.valueOf(3L).addSafe(memoryCost))

  stack.push(Bytes32.leftPad(memory.read(location, UInt256.valueOf(32)) ?: Bytes.EMPTY))
  Result()
}

private val extcodesize = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  gasManager.add(20)
  stack.push(UInt256.valueOf(hostContext.getCode(msg.destination).size().toLong()))
  Result()
}

private val extcodehash = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  gasManager.add(700)
  val code = hostContext.getCode(msg.destination)
  stack.push(if (code.isEmpty) UInt256.ZERO else Hash.keccak256(code))
  Result()
}

private val msize = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  gasManager.add(2)
  stack.push(memory.allocatedBytes())
  Result()
}

private val calldatasize = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(msg.inputData.size().toLong()))
  Result()
}

private val calldatacopy = Opcode { gasManager, _, stack, msg, _, _, memory, _ ->
  val memOffset = stack.pop()
  val sourceOffset = stack.pop()
  val length = stack.pop()
  if (null == memOffset || null == sourceOffset || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val numWords: UInt256 = length.divideCeil(Bytes32.SIZE.toLong())
  val copyCost = Gas.valueOf(3).multiply(Gas.valueOf(numWords)).add(Gas.valueOf(3))
  val memoryCost = memoryCost(memory.newSize(memOffset, length).subtract(memory.size()))

  gasManager.add(copyCost.addSafe(memoryCost))
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  memory.write(memOffset, sourceOffset, length, msg.inputData)

  Result()
}

private val calldataload = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  val start = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  gasManager.add(3)
  var set = false
  if (start.fitsInt()) {
    if (msg.inputData.size() > start.intValue()) {
      stack.push(
        Bytes32.rightPad(
          msg.inputData.slice(
            start.intValue(),
            Math.min(32, msg.inputData.size() - start.intValue())
          )
        )
      )
      set = true
    }
  }
  if (!set) {
    stack.push(Bytes32.ZERO)
  }
  Result()
}

private val sha3 = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val from = stack.pop()
  val length = stack.pop()
  if (null == from || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  val numWords: UInt256 = length.divideCeil(32L)
  val copyCost = Gas.valueOf(6).multiply(Gas.valueOf(numWords)).add(Gas.valueOf(30))

  val memoryCost = if (length.isZero) {
    Gas.ZERO
  } else {
    memoryCost(memory.newSize(from, length).subtract(memory.size()))
  }
  gasManager.add(copyCost.addSafe(memoryCost))
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  val bytes = memory.read(from, length, false)
  stack.push(if (bytes == null) Bytes32.ZERO else Hash.keccak256(bytes))
  Result()
}

fun computeValidJumpDestinations(code: Bytes): Set<Int> {
  var index = 0
  val destinations = HashSet<Int>()
  while (index < code.size()) {
    val currentOpcode = code.get(index)
    if (currentOpcode == 0x5b.toByte()) {
      destinations.add(index)
    }
    if (currentOpcode.toInt() in 0x60..0x7f) {
      index += currentOpcode - 0x60 + 1
    }
    index++
  }

  return destinations
}

private val jump = Opcode { gasManager, _, stack, _, code, _, _, _ ->
  gasManager.add(8)
  val jumpDest = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  if (!jumpDest.fitsInt() || jumpDest.intValue() >= code.size()) {
    return@Opcode Result(EVMExecutionStatusCode.BAD_JUMP_DESTINATION)
  }
  val validDestinations = computeValidJumpDestinations(code)
  if (!validDestinations.contains(jumpDest.intValue())) {
    return@Opcode Result(EVMExecutionStatusCode.BAD_JUMP_DESTINATION)
  }
  Result(newCodePosition = jumpDest.intValue())
}

private val jumpi = Opcode { gasManager, _, stack, _, code, _, _, _ ->
  gasManager.add(10)
  val jumpDest = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val condition = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  if (condition.isZero) {
    return@Opcode Result()
  }
  if (!jumpDest.fitsInt() || jumpDest.intValue() >= code.size()) {
    return@Opcode Result(EVMExecutionStatusCode.BAD_JUMP_DESTINATION)
  }
  val validDestinations = computeValidJumpDestinations(code)
  if (!validDestinations.contains(jumpDest.intValue())) {
    return@Opcode Result(EVMExecutionStatusCode.BAD_JUMP_DESTINATION)
  }
  Result(newCodePosition = jumpDest.intValue())
}

private val jumpdest = Opcode { gasManager, _, _, _, _, _, _, _ ->
  gasManager.add(1)
  Result()
}

fun log(topics: Int): Opcode {
  return Opcode { gasManager, hostContext, stack, msg, _, _, memory, _ ->
    val location = stack.pop()
    val length = stack.pop()
    if (null == location || null == length) {
      return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
    }

    val cost = Gas.valueOf(375).add((Gas.valueOf(8).multiply(Gas.valueOf(length)))).add(
      Gas.valueOf(375)
        .multiply(
          Gas.valueOf(
            topics.toLong()
          )
        )
    )
    gasManager.add(cost.addSafe(memoryCost(memory.newSize(location, length).subtract(memory.size()))))
    val address = msg.destination

    val data = memory.read(location, length)

    val topicList = mutableListOf<Bytes32>()
    for (i in 0 until topics) {
      topicList.add(stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW))
    }

    hostContext.emitLog(address, data ?: Bytes.EMPTY, topicList.toList())

    if (data == null) {
      Result(validationStatus = EVMExecutionStatusCode.INVALID_MEMORY_ACCESS)
    } else {
      Result()
    }
  }
}

private val sdiv = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(5L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.sdiv0(item2))
    Result()
  }
}

private val signextend = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(5L)
  val item = stack.pop()
  val item2 = stack.pop()
  if (null == item || null == item2) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    if (!item.fitsInt() || item.intValue() > 31) {
      stack.push(item2)
    } else {
      val byteIndex: Int = 32 - 1 - item.getInt(32 - 4)
      stack.push(
        UInt256.fromBytes(
          Bytes32.leftPad(
            item2.slice(byteIndex),
            if (item2.get(byteIndex) < 0) 0xFF.toByte() else 0x00
          )
        )
      )
    }

    Result()
  }
}

private val selfdestruct = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  gasManager.add(Gas.valueOf(0))
  val recipientAddress = stack.pop()?.slice(12, 20)?.let { Address.fromBytes(it) } ?: return@Opcode Result(
    EVMExecutionStatusCode.STACK_UNDERFLOW
  )

  val address = msg.destination
  val inheritance = hostContext.getBalance(address)
  hostContext.addRefund(recipientAddress, inheritance)

  hostContext.selfdestruct(address, recipientAddress)

  Result(EVMExecutionStatusCode.SUCCESS)
}

private val call = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->

  if (stack.size() < 7) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  if (message.kind == CallKind.STATICCALL) {
    return@Opcode Result(EVMExecutionStatusCode.STATIC_MODE_VIOLATION)
  }
  val stipend = Gas.valueOf(stack.pop()!!)
  val to = Address.fromBytes(stack.pop()!!.slice(12))
  val value = stack.pop()!!
  val inputDataOffset = stack.pop()!!
  val inputDataLength = stack.pop()!!

  val outputDataOffset = stack.pop()!!
  val outputDataLength = stack.pop()!!

  var cost = Gas.valueOf(40L)
  if (!value.isZero) {
    cost = cost.addSafe(Gas.valueOf(9000))
  }

  val inputSize = inputDataOffset.add(inputDataLength)
  val outputSize = outputDataOffset.add(outputDataLength)

  val callMemoryCost = if (inputSize.compareTo(outputSize) < 0) {
    memory.newSize(outputDataOffset, outputDataLength)
  } else {
    memory.newSize(inputDataOffset, inputDataLength)
  }

  cost = cost.addSafe(memoryCost(callMemoryCost.subtract(memory.size())).addSafe(stipend))
  if (!hostContext.accountExists(to)) {
    cost = cost.addSafe(Gas.valueOf(25000))
  }

  gasManager.add(cost)
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  val inputData = memory.read(inputDataOffset, inputDataLength)
  if (inputData == null) {
    return@Opcode Result(EVMExecutionStatusCode.INVALID_MEMORY_ACCESS)
  }

  val result = hostContext.call(
    EVMMessage(
      CallKind.CALL,
      0,
      message.depth + 1,
      stipend,
      to,
      to,
      message.destination,
      message.origin,
      inputData,
      value
    )
  )
  if (result.statusCode == EVMExecutionStatusCode.SUCCESS) {
    stack.push(UInt256.ONE)
    result.state.output?.let {
      memory.write(outputDataOffset, UInt256.ZERO, outputDataLength, it)
    }
  } else if (result.statusCode == EVMExecutionStatusCode.CALL_DEPTH_EXCEEDED) {
    return@Opcode Result(result.statusCode)
  } else {
    stack.push(UInt256.ZERO)
  }

  Result()
}

private val delegatecall = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->

  if (stack.size() < 6) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val gas = Gas.valueOf(stack.pop()!!)
  val to = Address.fromBytes(stack.pop()!!.slice(12))
  val inputDataOffset = stack.pop()!!
  val inputDataLength = stack.pop()!!

  val outputDataOffset = stack.pop()!!
  val outputDataLength = stack.pop()!!

  var cost = Gas.valueOf(40L)

  val inputMemoryCost = memoryCost(
    memory.newSize(inputDataOffset, inputDataLength)
      .subtract(memory.size())
  )
  val outputMemoryCost = memoryCost(
    memory.newSize(outputDataOffset, outputDataLength)
      .subtract(memory.size())
  )
  val memoryCost = if (outputMemoryCost.compareTo(inputMemoryCost) < 0) inputMemoryCost else outputMemoryCost

  cost.add(
    if (!hostContext.warmUpAccount(to)) {
      Gas.valueOf(2600)
    } else {
      Gas.valueOf(100)
    }
  )

  gasManager.add(cost.add(memoryCost))
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  val inputData = memory.read(inputDataOffset, inputDataLength)
  if (inputData == null) {
    return@Opcode Result(EVMExecutionStatusCode.INVALID_MEMORY_ACCESS)
  }
  val result = hostContext.call(
    EVMMessage(
      CallKind.DELEGATECALL,
      0,
      message.depth + 1,
      gas,
      to,
      message.destination,
      message.sender,
      message.origin,
      inputData,
      Wei.valueOf(0)
    )
  )
  if (result.statusCode == EVMExecutionStatusCode.SUCCESS) {
    stack.push(UInt256.ONE)
    result.state.output?.let {
      memory.write(outputDataOffset, UInt256.ZERO, outputDataLength, it)
    }
  } else {
    stack.push(UInt256.ZERO)
  }

  Result()
}

private val callcode = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->

  if (stack.size() < 7) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val gas = Gas.valueOf(stack.pop()!!)
  val to = Address.fromBytes(stack.pop()!!.slice(12))
  val value = stack.pop()!!
  val inputDataOffset = stack.pop()!!
  val inputDataLength = stack.pop()!!

  val outputDataOffset = stack.pop()!!
  val outputDataLength = stack.pop()!!

  var cost = Gas.valueOf(700L)
  if (!value.isZero) {
    cost = cost.add(Gas.valueOf(9000))
  }

  val inputMemoryCost = memoryCost(
    memory.newSize(inputDataOffset, inputDataLength)
      .subtract(memory.size())
  )
  val outputMemoryCost = memoryCost(
    memory.newSize(outputDataOffset, outputDataLength)
      .subtract(memory.size())
  )
  val memoryCost = if (outputMemoryCost.compareTo(inputMemoryCost) < 0) inputMemoryCost else outputMemoryCost
  if (!hostContext.accountExists(to) && !value.isZero) {
    cost = cost.add(Gas.valueOf(25000))
  }

  cost.add(
    if (!hostContext.warmUpAccount(to)) {
      Gas.valueOf(2600)
    } else {
      Gas.valueOf(100)
    }
  )

  gasManager.add(cost.add(memoryCost))
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  val inputData = memory.read(inputDataOffset, inputDataLength)
  if (inputData == null) {
    return@Opcode Result(EVMExecutionStatusCode.INVALID_MEMORY_ACCESS)
  }
  val result = hostContext.call(
    EVMMessage(
      CallKind.CALLCODE,
      0,
      message.depth + 1,
      gas,
      to,
      to,
      to,
      message.origin,
      inputData,
      value
    )
  )
  if (result.statusCode == EVMExecutionStatusCode.SUCCESS) {
    stack.push(UInt256.ONE)
    result.state.output?.let {
      memory.write(outputDataOffset, UInt256.ZERO, outputDataLength, it)
    }
  } else {
    stack.push(UInt256.ZERO)
  }

  Result()
}

private val staticcall = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->

  if (stack.size() < 6) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val gas = Gas.valueOf(stack.pop()!!)
  val to = Address.fromBytes(stack.pop()!!.slice(12))
  val inputDataOffset = stack.pop()!!
  val inputDataLength = stack.pop()!!

  val outputDataOffset = stack.pop()!!
  val outputDataLength = stack.pop()!!

  var cost = Gas.valueOf(700L)

  val inputMemoryCost = memoryCost(
    memory.newSize(inputDataOffset, inputDataLength)
      .subtract(memory.size())
  )
  val outputMemoryCost = memoryCost(
    memory.newSize(outputDataOffset, outputDataLength)
      .subtract(memory.size())
  )
  val memoryCost = if (outputMemoryCost.compareTo(inputMemoryCost) < 0) inputMemoryCost else outputMemoryCost
  cost.add(
    if (!hostContext.warmUpAccount(to)) {
      Gas.valueOf(2600)
    } else {
      Gas.valueOf(100)
    }
  )

  gasManager.add(cost.add(memoryCost))
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  val inputData = memory.read(inputDataOffset, inputDataLength)
  if (inputData == null) {
    return@Opcode Result(EVMExecutionStatusCode.INVALID_MEMORY_ACCESS)
  }
  val result = hostContext.call(
    EVMMessage(
      CallKind.STATICCALL,
      0,
      message.depth + 1,
      gas,
      to,
      message.destination,
      message.destination,
      message.origin,
      inputData,
      Wei.valueOf(0)
    )
  )
  if (result.statusCode == EVMExecutionStatusCode.SUCCESS) {
    stack.push(UInt256.ONE)
    result.state.output?.let {
      memory.write(outputDataOffset, UInt256.ZERO, outputDataLength, it)
    }
  } else {
    stack.push(UInt256.ZERO)
  }

  Result()
}

private val create = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->
  val value = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputDataOffset = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputDataLength = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputMemoryCost = memoryCost(
    memory.newSize(inputDataOffset, inputDataLength)
      .subtract(memory.size())
  )
  gasManager.add(Gas.valueOf(32000).add(inputMemoryCost))
  val inputData = memory.read(inputDataOffset, inputDataLength)
  if (inputData == null) {
    return@Opcode Result(EVMExecutionStatusCode.INVALID_MEMORY_ACCESS)
  }
  val nonce = hostContext.getNonce(message.sender)
  val to = Address.fromSenderAndNonce(message.sender, nonce)

  val result = hostContext.call(
    EVMMessage(
      CallKind.CREATE,
      0,
      message.depth + 1,
      gasManager.gasLeft(),
      to,
      to,
      message.destination,
      message.destination,
      inputData,
      value
    )
  )
  if (result.statusCode == EVMExecutionStatusCode.SUCCESS) {
    stack.push(UInt256.ONE)
  } else {
    stack.push(UInt256.ZERO)
  }

  Result()
}

private val revert = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop()
  val length = stack.pop()
  if (null == location || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val memoryCost =
    memoryCost(memory.newSize(location, length).subtract(memory.size()))
  gasManager.add(memoryCost)
  val output = memory.read(location, length)
  Result(EVMExecutionStatusCode.REVERT, output = output)
}

private val shl = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3)
  var shiftAmount = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  if (shiftAmount.trimLeadingZeros().size() > 4) {
    stack.pop()
    stack.push(UInt256.ZERO)
    Result()
  } else {
    val shiftAmountInt = shiftAmount.getInt(28)
    val value = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
    if (shiftAmountInt >= 256 || shiftAmountInt < 0) {
      stack.push(UInt256.ZERO)
    } else {
      stack.push(value.shiftLeft(shiftAmountInt))
    }
    Result()
  }
}

private val shr = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3)

  var shiftAmount = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  if (shiftAmount.trimLeadingZeros().size() > 4) {
    stack.pop()
    stack.push(UInt256.ZERO)
    Result()
  } else {
    val shiftAmountInt = shiftAmount.getInt(28)
    val value = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
    if (shiftAmountInt >= 256 || shiftAmountInt < 0) {
      stack.push(UInt256.ZERO)
    } else {
      stack.push(value.shiftRight(shiftAmountInt))
    }
    Result()
  }
}

private val sar = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3)

  var shiftAmount = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val value = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val negativeNumber = value[0] < 0
  if (shiftAmount.trimLeadingZeros().size() > 4) {
    stack.push(if (negativeNumber) UInt256.MAX_VALUE else UInt256.ZERO)
  } else {
    val shiftAmountInt = shiftAmount.getInt(28)
    if (shiftAmountInt >= 256 || shiftAmountInt < 0) {
      stack.push(if (negativeNumber) UInt256.MAX_VALUE else UInt256.ZERO)
    } else {
      var result = value.shiftRight(shiftAmountInt)

      if (negativeNumber) {
        val significantBits =
          UInt256.MAX_VALUE.shiftLeft(256 - shiftAmountInt)
        result = result.or(significantBits)
      }
      stack.push(result)
    }
  }
  Result()
}

private val selfbalance = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  gasManager.add(5)
  val account = hostContext.getBalance(msg.destination)

  stack.push(account)
  Result()
}

private val chainid = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(hostContext.getChaindId())
  Result()
}

val frontierOpcodes = buildMap {
  this[0x00] = stop
  this[0x01] = add
  this[0x02] = mul
  this[0x03] = sub
  this[0x04] = div
  this[0x05] = sdiv
  this[0x06] = mod
  this[0x07] = smod
  this[0x08] = addmod
  this[0x09] = mulmod
  this[0x10] = lt
  this[0x11] = gt
  this[0x12] = slt
  this[0x13] = sgt
  this[0x0a] = exp
  this[0x0b] = signextend
  this[0x14] = eq
  this[0x15] = isZero
  this[0x16] = and
  this[0x17] = or
  this[0x18] = xor
  this[0x19] = not
  this[0x1a] = byte
  this[0x1b] = shl
  this[0x1c] = shr
  this[0x1d] = sar
  this[0x20] = sha3
  this[0x30] = address
  this[0x31] = balance
  this[0x32] = origin
  this[0x33] = caller
  this[0x34] = callvalue
  this[0x35] = calldataload
  this[0x36] = calldatasize
  this[0x37] = calldatacopy
  this[0x38] = codesize
  this[0x39] = codecopy
  this[0x3a] = gasPrice
  this[0x3b] = extcodesize
  this[0x3c] = extcodecopy
  this[0x3d] = returndatasize
  this[0x3e] = returndatacopy
  this[0x3f] = extcodehash

  this[0x40] = blockhash
  this[0x41] = coinbase
  this[0x42] = timestamp
  this[0x43] = number
  this[0x44] = difficulty
  this[0x45] = gasLimit
  this[0x46] = chainid
  this[0x47] = selfbalance
  this[0x50] = pop
  this[0x51] = mload
  this[0x52] = mstore
  this[0x53] = mstore8
  this[0x54] = sload
  this[0x55] = sstore
  this[0x56] = jump
  this[0x57] = jumpi
  this[0x58] = pc
  this[0x59] = msize
  this[0x5a] = gas
  this[0x5b] = jumpdest
  this[0xf0.toByte()] = create
  this[0xf1.toByte()] = call
  this[0xf2.toByte()] = callcode
  this[0xf3.toByte()] = retuRn
  this[0xf4.toByte()] = delegatecall
  this[0xfa.toByte()] = staticcall
  this[0xfd.toByte()] = revert
  this[0xfe.toByte()] = invalid
  this[0xff.toByte()] = selfdestruct
  for (i in 1..32) {
    this[(0x60 + i - 1).toByte()] = push(i)
  }

  for (i in 1..16) {
    this[(0x80 + i - 1).toByte()] = dup(i)
  }

  for (i in 1..16) {
    this[(0x90 + i - 1).toByte()] = swap(i)
  }

  for (i in 0..4) {
    this[(0xa0 + i).toByte()] = log(i)
  }
}
