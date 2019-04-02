/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.ssz.experimental

import org.apache.tuweni.bytes.Bytes.fromHexString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
class SSZTest {

    @Test
    fun shouldEncodeUnsigned() {
        assertEquals(fromHexString("0000"), SSZ.encodeUInt16(0.toUInt()))
        assertEquals(fromHexString("00000000"), SSZ.encodeUInt32(0.toUInt()))
        assertEquals(fromHexString("0000000000000000"), SSZ.encodeUInt64(0L.toULong()))
        assertEquals(fromHexString("0000000000000000"), SSZ.encodeUInt64(0L.toULong()))

        assertEquals(fromHexString("FFFF"), SSZ.encodeUInt16(65535.toUInt()))
        assertEquals(fromHexString("FFFF0000"), SSZ.encodeUInt32(65535.toUInt()))
    }

    @Test
    fun shouldWriteUnsigned() {
        assertEquals(fromHexString("FFFF"), SSZ.encode { w -> w.writeUInt16(65535.toUInt()) })
        assertEquals(fromHexString("FFFF0000"), SSZ.encode { w -> w.writeUInt32(65535.toUInt()) })
    }
}
