// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import org.apache.tuweni.junit.BouncyCastleExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class EthJsonModuleTest {

  @Test
  void testSerialize() throws Exception {
    BlockHeader header = BlockHeaderTest.generateBlockHeader();
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new EthJsonModule());
    mapper.writer().writeValueAsString(header);
  }
}
