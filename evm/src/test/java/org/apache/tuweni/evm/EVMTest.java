// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.evm;

import org.apache.tuweni.eth.precompiles.Registry;
import org.apache.tuweni.eth.repository.BlockchainRepository;
import org.apache.tuweni.evm.impl.EvmVmImpl;
import org.apache.tuweni.genesis.Genesis;
import org.apache.tuweni.junit.BouncyCastleExtension;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
public class EVMTest {

  @Test
  void testStartAndStop() {
    BlockchainRepository repository =
        BlockchainRepository.Companion.inMemoryBlocking(Genesis.dev());

    EthereumVirtualMachine vm =
        new EthereumVirtualMachine(
            repository,
            repository,
            Registry.getIstanbul(),
            EvmVmImpl::create,
            Collections.singletonMap("DISABLE_TRANSFER_VALUE", "true"));
    vm.startAsync();
    vm.stopAsync();
  }
}
