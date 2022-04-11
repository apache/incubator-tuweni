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
package org.apache.tuweni.evm.impl.berlin

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

val add = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val addmod = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val not = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop()
  if (null == item) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    stack.push(item.not())
    Result()
  }
}

val eq = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val lt = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val slt = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val gt = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val sgt = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val isZero = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(3L)
  val item = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  stack.push(if (item.isZero) UInt256.ONE else UInt256.ZERO)
  Result()
}

val and = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val pop = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(2L)
  stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  Result()
}

val or = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val xor = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val byte = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val mul = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val mod = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val smod = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val mulmod = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val sub = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val exp = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  val number = stack.pop()
  val power = stack.pop()
  if (null == number || null == power) {
    Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  } else {
    val numBytes: Int = (power.bitLength() + 7) / 8

    val cost = (Gas.valueOf(50).multiply(Gas.valueOf(numBytes.toLong())))
      .add(Gas.valueOf(10))
    gasManager.add(cost)

    val result: UInt256 = number.pow(power)

    stack.push(result)
    Result()
  }
}

val div = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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
    stack.push(Bytes32.leftPad(code.slice(currentIndex, minLength)))
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
    val elt0 = stack.get(0) ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
    stack.set(index, elt0)
    stack.set(0, eltN)
    Result()
  }
}

val sstore = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  val key = stack.pop()
  val value = stack.pop()
  if (null == key || null == value) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  if (msg.kind == CallKind.STATICCALL) {
    return@Opcode Result(EVMExecutionStatusCode.STATIC_MODE_VIOLATION)
  }
  val remainingGas = gasManager.gasLeft()
  if (remainingGas <= 2300) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }

  val address = msg.destination
  val slotIsWarm = hostContext.warmUpStorage(address, key)

  val currentValue = hostContext.getStorage(address, key)
  val cost = if (value.equals(currentValue)) {
    Gas.valueOf(100)
  } else {
    val originalValue = hostContext.getRepositoryStorage(address, key)
    if (originalValue.equals(currentValue)) {
      if (originalValue.isZero) {
        Gas.valueOf(20000L - 2100)
      } else Gas.valueOf(5000L - 2100)
    } else {
      Gas.valueOf(100)
    }
  }.add(if (slotIsWarm) Gas.ZERO else Gas.valueOf(2100))
  gasManager.add(cost)

  // frame.incrementGasRefund(gasCalculator().calculateStorageRefundAmount(account, key, value))

  hostContext.setStorage(address, key, value)

  Result()
}

val sload = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  val key = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)

  val address = msg.destination
  val slotIsWarm = hostContext.warmUpStorage(address, key)
  gasManager.add(if (slotIsWarm) 100 else 2600)

  stack.push(hostContext.getStorage(address, key))

  Result()
}

val stop = Opcode { gasManager, _, _, _, _, _, _, _ ->
  gasManager.add(0L)
  Result(EVMExecutionStatusCode.SUCCESS)
}

val invalid = Opcode { _, _, _, _, _, _, _, _ ->
  Result(EVMExecutionStatusCode.INVALID_INSTRUCTION)
}

val retuRn = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop()
  val length = stack.pop()
  if (null == location || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val memoryCost = memoryCost(memory.newSize(location, length).subtract(memory.size()))
  gasManager.add(memoryCost)
  val output = memory.read(location, length)
  Result(EVMExecutionStatusCode.SUCCESS, output = output)
}

val revert = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop()
  val length = stack.pop()
  if (null == location || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val memoryCost = memoryCost(memory.newSize(location, length).subtract(memory.size()))
  gasManager.add(memoryCost)
  val output = memory.read(location, length)
  Result(EVMExecutionStatusCode.REVERT, output = output)
}

val address = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(msg.destination))
  Result()
}

val origin = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(msg.origin))
  Result()
}

val caller = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(msg.sender))
  Result()
}

val callvalue = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(msg.value))
  Result()
}

val balance = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->

  val address = stack.pop()?.slice(12, 20)?.let { Address.fromBytes(it) }
    ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val accountIsWarm = hostContext.warmUpAccount(address)
  if (accountIsWarm) {
    gasManager.add(100)
  } else {
    gasManager.add(2600)
  }

  stack.push(hostContext.getBalance(address))

  Result()
}

val pc = Opcode { gasManager, _, stack, _, _, currentIndex, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(currentIndex.toLong() - 1))
  Result()
}

val gasPrice = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(hostContext.getGasPrice()))
  Result()
}

val gas = Opcode { gasManager, _, stack, _, _, _, _, _ ->
  gasManager.add(2)
  if (!gasManager.hasGasLeft()) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  stack.push(UInt256.valueOf(gasManager.gasLeft().toLong()))
  Result()
}

val coinbase = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(Bytes32.leftPad(hostContext.getCoinbase()))
  Result()
}

val gasLimit = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(hostContext.getGasLimit()))
  Result()
}

val difficulty = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(hostContext.getDifficulty())
  Result()
}

val number = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(hostContext.getBlockNumber()))
  Result()
}

val blockhash = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(20)
  val number = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  if (!number.fitsLong() || number.toLong() < hostContext.getBlockNumber() - 256) {
    stack.push(UInt256.ZERO)
  } else {
    stack.push(UInt256.fromBytes(hostContext.getBlockHash(number.toLong())))
  }
  Result()
}

val codesize = Opcode { gasManager, _, stack, _, code, _, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(code.size().toLong()))
  Result()
}

val timestamp = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(hostContext.timestamp())
  Result()
}

fun memoryCost(length: UInt256): Gas {
  if (!length.fitsInt()) {
    return Gas.TOO_HIGH
  }
  val len: Gas = Gas.valueOf(length)
  val base: Gas = len.multiply(len).divide(Gas.valueOf(512))
  return Gas.valueOf(3).multiply(len).add(base)
}

val codecopy = Opcode { gasManager, _, stack, _, code, _, memory, _ ->
  val memOffset = stack.pop()
  val sourceOffset = stack.pop()
  val length = stack.pop()
  if (null == memOffset || null == sourceOffset || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val numWords: UInt256 = length.divideCeil(Bytes32.SIZE.toLong())
  val copyCost = Gas.valueOf(3).multiply(Gas.valueOf(numWords)).add(Gas.valueOf(3))
  val memoryCost = memoryCost(memory.newSize(memOffset, length).subtract(memory.size()))

  gasManager.add(copyCost.add(memoryCost))

  memory.write(memOffset, sourceOffset, length, code)

  Result()
}

val extcodecopy = Opcode { gasManager, hostContext, stack, _, _, _, memory, _ ->
  val address = stack.pop()
  val memOffset = stack.pop()
  val sourceOffset = stack.pop()
  val length = stack.pop()
  if (null == address || null == memOffset || null == sourceOffset || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val numWords: UInt256 = length.divideCeil(Bytes32.SIZE.toLong())
  val copyCost = Gas.valueOf(3).multiply(Gas.valueOf(numWords)).add(Gas.valueOf(3))
  val memoryCost = memoryCost(memory.newSize(memOffset, length).subtract(memory.size()))

  gasManager.add(copyCost.add(memoryCost))

  val code = hostContext.getCode(Address.fromBytes(address.slice(12, 20)))
  memory.write(memOffset, sourceOffset, length, code)

  Result()
}

val returndatasize = Opcode { gasManager, _, stack, _, _, _, _, callResult ->
  gasManager.add(3)
  stack.push(UInt256.valueOf(callResult?.output?.size()?.toLong() ?: 0L))
  Result()
}

val returndatacopy = Opcode { gasManager, _, stack, _, _, _, memory, callResult ->
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

  gasManager.add(copyCost.add(memoryCost))

  memory.write(memOffset, sourceOffset, length, returnData)

  Result()
}

val mstore = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop()
  val value = stack.pop()
  if (null == location || null == value) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val memoryCost = memoryCost(memory.newSize(location, UInt256.valueOf(32)).subtract(memory.size()))
  gasManager.add(Gas.valueOf(3L).add(memoryCost))

  memory.write(location, UInt256.ZERO, UInt256.valueOf(32), value)
  Result()
}

val mstore8 = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop()
  val value = stack.pop()
  if (null == location || null == value) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val memoryCost = memoryCost(memory.newSize(location, UInt256.ONE).subtract(memory.size()))

  gasManager.add(Gas.valueOf(3L).add(memoryCost))

  memory.write(location, UInt256.ZERO, UInt256.valueOf(1), value.slice(31, 1))
  Result()
}

val mload = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val location = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val memoryCost = memoryCost(memory.newSize(location, UInt256.valueOf(32)).subtract(memory.size()))
  gasManager.add(Gas.valueOf(3L).add(memoryCost))

  stack.push(Bytes32.leftPad(memory.read(location, UInt256.valueOf(32)) ?: Bytes.EMPTY))
  Result()
}

val extcodesize = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(700)
  val address = stack.pop()?.slice(12)?.let { Address.fromBytes(it) } ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  stack.push(UInt256.valueOf(hostContext.getCode(address).size().toLong()))
  Result()
}

val extcodehash = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  gasManager.add(700)
  stack.push(Hash.keccak256(hostContext.getCode(msg.destination)))
  Result()
}

val msize = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  gasManager.add(2)
  stack.push(memory.allocatedBytes())
  Result()
}

val calldatasize = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(2)
  stack.push(UInt256.valueOf(msg.inputData.size().toLong()))
  Result()
}

val calldatacopy = Opcode { gasManager, _, stack, msg, _, _, memory, _ ->
  val memOffset = stack.pop()
  val sourceOffset = stack.pop()
  val length = stack.pop()
  if (null == memOffset || null == sourceOffset || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  }
  val numWords: UInt256 = length.divideCeil(Bytes32.SIZE.toLong())
  val copyCost = Gas.valueOf(3).multiply(Gas.valueOf(numWords)).add(Gas.valueOf(3))
  val memoryCost = memoryCost(memory.newSize(memOffset, length).subtract(memory.size()))

  gasManager.add(copyCost.add(memoryCost))

  memory.write(memOffset, sourceOffset, length, msg.inputData)

  Result()
}

val calldataload = Opcode { gasManager, _, stack, msg, _, _, _, _ ->
  gasManager.add(3)
  val start = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
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

val sha3 = Opcode { gasManager, _, stack, _, _, _, memory, _ ->
  val from = stack.pop()
  val length = stack.pop()
  if (null == from || null == length) {
    return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
  }
  val numWords: UInt256 = length.divideCeil(Bytes32.SIZE.toLong())
  val copyCost = Gas.valueOf(6).multiply(Gas.valueOf(numWords)).add(Gas.valueOf(30))
  val memoryCost = memoryCost(memory.newSize(from, length).subtract(memory.size()))
  gasManager.add(copyCost.add(memoryCost))
  val bytes = memory.read(from, length)
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
    if (currentOpcode.toInt() >= 0x60 && currentOpcode < 0x80) {
      index += currentOpcode - 0x60 + 1
    }
    index++
  }

  return destinations
}

val jump = Opcode { gasManager, _, stack, _, code, _, _, _ ->
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

val jumpi = Opcode { gasManager, _, stack, _, code, _, _, _ ->
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

val jumpdest = Opcode { gasManager, _, _, _, _, _, _, _ ->
  gasManager.add(1)
  Result()
}

val call = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->

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

  var cost = Gas.valueOf(700L)
  if (!value.isZero) {
    cost = cost.add(Gas.valueOf(9000))
  }

  val inputMemoryCost = memoryCost(memory.newSize(inputDataOffset, inputDataLength).subtract(memory.size()))
  val outputMemoryCost = memoryCost(memory.newSize(outputDataOffset, outputDataLength).subtract(memory.size()))
  val memoryCost = if (outputMemoryCost.compareTo(inputMemoryCost) < 0) inputMemoryCost else outputMemoryCost
  if (!hostContext.accountExists(to) && !value.isZero) {
    cost = cost.add(Gas.valueOf(25000))
  }
  cost.add(
    if (hostContext.warmUpAccount(to)) {
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

  val gasLeft = gasManager.gasLeft()
  val gasAvailable = Gas.minimum(stipend, gasLeft.subtract(gasLeft.divide(Gas.valueOf(64))))
  val result = hostContext.call(
    EVMMessage(
      CallKind.CALL,
      0,
      message.depth + 1,
      gasAvailable,
      to,
      to,
      message.destination,
      message.origin,
      inputData,
      value
    )
  )
  gasManager.add(result.state.gasManager.gasCost)
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

val delegatecall = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->

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

  val inputMemoryCost = memoryCost(memory.newSize(inputDataOffset, inputDataLength).subtract(memory.size()))
  val outputMemoryCost = memoryCost(memory.newSize(outputDataOffset, outputDataLength).subtract(memory.size()))
  val memoryCost = if (outputMemoryCost.compareTo(inputMemoryCost) < 0) inputMemoryCost else outputMemoryCost

  cost.add(
    if (hostContext.warmUpAccount(to)) {
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

val callcode = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->

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

  val inputMemoryCost = memoryCost(memory.newSize(inputDataOffset, inputDataLength).subtract(memory.size()))
  val outputMemoryCost = memoryCost(memory.newSize(outputDataOffset, outputDataLength).subtract(memory.size()))
  val memoryCost = if (outputMemoryCost.compareTo(inputMemoryCost) < 0) inputMemoryCost else outputMemoryCost
  if (!hostContext.accountExists(to) && !value.isZero) {
    cost = cost.add(Gas.valueOf(25000))
  }

  cost.add(
    if (hostContext.warmUpAccount(to)) {
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

val staticcall = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->

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

  val inputMemoryCost = memoryCost(memory.newSize(inputDataOffset, inputDataLength).subtract(memory.size()))
  val outputMemoryCost = memoryCost(memory.newSize(outputDataOffset, outputDataLength).subtract(memory.size()))
  val memoryCost = if (outputMemoryCost.compareTo(inputMemoryCost) < 0) inputMemoryCost else outputMemoryCost
  cost.add(
    if (hostContext.warmUpAccount(to)) {
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

val create = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->
  val value = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputDataOffset = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputDataLength = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputMemoryCost = memoryCost(memory.newSize(inputDataOffset, inputDataLength).subtract(memory.size()))
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
      message.sender,
      message.origin,
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

val create2 = Opcode { gasManager, hostContext, stack, message, _, _, memory, _ ->
  val value = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputDataOffset = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputDataLength = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val salt = stack.pop() ?: return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
  val inputMemoryCost = memoryCost(memory.newSize(inputDataOffset, inputDataLength).subtract(memory.size()))
  gasManager.add(Gas.valueOf(32000).add(inputMemoryCost).add(Gas.valueOf(inputDataLength.divide(32).multiply(6))))
  val inputData = memory.read(inputDataOffset, inputDataLength)
  if (inputData == null) {
    return@Opcode Result(EVMExecutionStatusCode.INVALID_MEMORY_ACCESS)
  }
  val hash = Hash.keccak256(
    Bytes.concatenate(
      Bytes.fromHexString("0xFF"),
      message.sender,
      salt,
      Hash.keccak256(inputData)
    )
  )
  val to = Address.fromBytes(hash.slice(12))

  val result = hostContext.call(
    EVMMessage(
      CallKind.CREATE2,
      0,
      message.depth + 1,
      gasManager.gasLeft(),
      to,
      to,
      message.sender,
      message.origin,
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

val shl = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val shr = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val sar = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

fun log(topics: Int): Opcode {
  return Opcode { gasManager, hostContext, stack, msg, _, _, memory, _ ->
    val location = stack.pop()
    val length = stack.pop()
    if (null == location || null == length) {
      return@Opcode Result(EVMExecutionStatusCode.STACK_UNDERFLOW)
    }
    if (msg.kind == CallKind.STATICCALL) {
      return@Opcode Result(EVMExecutionStatusCode.STATIC_MODE_VIOLATION)
    }

    var cost = Gas.valueOf(375).add((Gas.valueOf(8).multiply(Gas.valueOf(length)))).add(
      Gas.valueOf(375)
        .multiply(
          Gas.valueOf(
            topics.toLong()
          )
        )
    )
    val memoryCost = memoryCost(memory.newSize(location, length).subtract(memory.size()))
    gasManager.add(cost.add(memoryCost))
    if (!gasManager.hasGasLeft()) {
      return@Opcode Result(EVMExecutionStatusCode.OUT_OF_GAS)
    }

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

val sdiv = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val signextend = Opcode { gasManager, _, stack, _, _, _, _, _ ->
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

val selfdestruct = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  val recipientAddress = stack.pop()?.slice(12, 20)?.let { Address.fromBytes(it) } ?: return@Opcode Result(
    EVMExecutionStatusCode.STACK_UNDERFLOW
  )

  val inheritance = hostContext.getBalance(recipientAddress)

  val accountIsWarm = hostContext.warmUpAccount(recipientAddress)

  val cost = if (hostContext.accountExists(recipientAddress) && !inheritance.isZero) {
    Gas.valueOf(30000)
  } else {
    Gas.valueOf(5000)
  }.add(if (accountIsWarm) Gas.ZERO else Gas.valueOf(2600))
  gasManager.add(cost)
  val address: Address = msg.destination

  hostContext.selfdestruct(address, recipientAddress)

  // frame.addRefund(recipient.getAddress(), account.getBalance())

  Result(EVMExecutionStatusCode.SUCCESS)
}

val selfbalance = Opcode { gasManager, hostContext, stack, msg, _, _, _, _ ->
  gasManager.add(5)
  val account = hostContext.getBalance(msg.sender)

  stack.push(account)
  Result()
}

val chainid = Opcode { gasManager, hostContext, stack, _, _, _, _, _ ->
  gasManager.add(2)
  stack.push(hostContext.getChaindId())
  Result()
}
