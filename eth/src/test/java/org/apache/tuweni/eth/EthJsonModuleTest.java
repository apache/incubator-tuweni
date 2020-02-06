package org.apache.tuweni.eth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class EthJsonModuleTest {

    @Test
    void testSerialize() throws Exception {
        BlockHeader header = BlockHeaderTest.generateBlockHeader();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new EthJsonModule());
        mapper.writer().writeValueAsString(header);
    }
}
