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
package org.apache.tuweni.eth.precompiles

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.Address
import org.hyperledger.besu.nativelib.bls12_381.LibEthPairings

/**
 * Registry of precompiles organized by hard forks.
 */
object Registry {
  val frontier: Map<Address, PrecompileContract>
  val bizantium: Map<Address, PrecompileContract>
  val istanbul: Map<Address, PrecompileContract>

  init {
    val emptyArray = Bytes.repeat(0, 19)
    val ecrec = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(1)))
    val sha256 = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(2)))
    val ripemd160 = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(3)))
    val id = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(4)))

    val ecrecContract = ECRECPrecompiledContract()
    val sha256PrecompiledContract = Sha256PrecompiledContract()
    val ripemD160PrecompiledContract = RIPEMD160PrecompiledContract()
    val idPrecompiledContract = IDPrecompiledContract()
    frontier = mapOf(
      Pair(ecrec, ecrecContract),
      Pair(sha256, sha256PrecompiledContract),
      Pair(ripemd160, ripemD160PrecompiledContract),
      Pair(id, idPrecompiledContract)
    )

    val modexp = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(5)))
    val modExpPrecompileContract = ModExpPrecompileContract()

    val altBn128add = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(6)))
    val altBn128mul = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(7)))
    val altBn128pairing = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(8)))
    val bizantiumAltBN128AddPrecompiledContract =
      AltBN128PrecompiledContract(LibEthPairings.EIP196_ADD_OPERATION_RAW_VALUE, 128, 500, 0)
    val bizantiumAltBN128MulPrecompiledContract =
      AltBN128PrecompiledContract(LibEthPairings.EIP196_MUL_OPERATION_RAW_VALUE, 96, 40000, 0)
    val bizantiumAltBN128PairingPrecompiledContract =
      AltBN128PrecompiledContract(
        LibEthPairings.EIP196_PAIR_OPERATION_RAW_VALUE,
        Int.MAX_VALUE / 192 * 192,
        100000,
        80000
      )

    bizantium = buildMap {
      this.putAll(frontier)
      this.put(modexp, modExpPrecompileContract)
      this.put(altBn128add, bizantiumAltBN128AddPrecompiledContract)
      this.put(altBn128mul, bizantiumAltBN128MulPrecompiledContract)
      this.put(altBn128pairing, bizantiumAltBN128PairingPrecompiledContract)
    }

    val istanbulAltBN128AddPrecompiledContract =
      AltBN128PrecompiledContract(LibEthPairings.EIP196_ADD_OPERATION_RAW_VALUE, 128, 150, 0)
    val istanbulAltBN128MulPrecompiledContract =
      AltBN128PrecompiledContract(LibEthPairings.EIP196_MUL_OPERATION_RAW_VALUE, 96, 6000, 0)
    val istanbulAltBN128PairingPrecompiledContract =
      AltBN128PrecompiledContract(
        LibEthPairings.EIP196_PAIR_OPERATION_RAW_VALUE,
        Int.MAX_VALUE / 192 * 192,
        45000,
        34000
      )
    val blake2bf = Address.fromBytes(Bytes.wrap(emptyArray, Bytes.of(9)))
    val blake2BFPrecompileContract = Blake2BFPrecompileContract()
    istanbul = buildMap {
      this.putAll(bizantium)
      this.put(altBn128add, istanbulAltBN128AddPrecompiledContract)
      this.put(altBn128mul, istanbulAltBN128MulPrecompiledContract)
      this.put(altBn128pairing, istanbulAltBN128PairingPrecompiledContract)
      this.put(blake2bf, blake2BFPrecompileContract)
    }
  }
}
