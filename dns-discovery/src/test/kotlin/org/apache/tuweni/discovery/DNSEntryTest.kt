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
package org.apache.tuweni.discovery

import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
class DNSEntryTest {

  @Test
  fun testInvalidEntry() {
    val exception: InvalidEntryException = assertThrows {
      DNSEntry.readDNSEntry("garbage")
    }
    assertEquals("garbage should contain enrtree-branch, enr, enrtree-root or enrtree", exception.message)
  }

  @Test
  fun testInvalidEntryMissingSeparator() {
    val exception: InvalidEntryException = assertThrows {
      DNSEntry.readDNSEntry("garbage=abc def")
    }
    assertNotNull(exception)
  }

  @Test
  fun testInvalidEntryMissingENR() {
    val exception: InvalidEntryException = assertThrows {
      DNSEntry.readDNSEntry("garbage=abc def=gfh")
    }
    assertEquals(
      "garbage=abc def=gfh should contain enrtree-branch, enr, enrtree-root or enrtree",
      exception.message
    )
  }

  @Test
  fun missingSigEntry() {
    val exception: InvalidEntryException = assertThrows {
      DNSEntry.readDNSEntry("enrtree-root:v1 hash=TO4Q75OQ2N7DX4EOOR7X66A6OM seq=3")
    }
    assertEquals("Missing attributes on root entry", exception.message)
  }

  @Test
  fun missingSeqEntry() {
    val exception: InvalidEntryException = assertThrows {
      DNSEntry.readDNSEntry(
        "enrtree-root:v1 e=TO4Q75OQ2N7DX4EOOR7X66A6OM l=TO4Q75OQ2N7DX4EOOR7X66A6OM" +
          " sig=N-YY6UB9xD0hFx1Gmnt7v0RfSxch5tKyry2SRDoLx7B4GfPXagwLxQqyf7gAMvApFn_ORwZQekMWa_pXrcGCtwE="
      )
    }
    assertEquals("Missing attributes on root entry", exception.message)
  }

  @Test
  fun testValidENRTreeRoot() {
    val entry = DNSEntry.readDNSEntry(
      "enrtree-root:v1 e=TO4Q75OQ2N7DX4EOOR7X66A6OM l=TO4Q75OQ2N7DX4EOOR7X66A6OM" +
        " seq=3 sig=N-YY6UB9xD0hFx1Gmnt7v0RfSxch5tKyry2SRDoLx7B4GfPXagwLxQqyf7gAMvApFn_ORwZQekMWa_pXrcGCtwE="
    )
      as ENRTreeRoot
    assertEquals("v1", entry.version)
    assertEquals(3, entry.seq)
  }

  @Test
  fun testValidENRTreeLink() {
    val entry = DNSEntry.readDNSEntry(
      "enrtree://aabb@morenodes.example.org"
    )
      as ENRTreeLink
    assertEquals("morenodes.example.org", entry.domainName)
    assertEquals("aabb", entry.pubKey)
  }

  @Test
  fun testValidENRNode() {
    val entry = DNSEntry.readDNSEntry(
      "enr:-H24QI0fqW39CMBZjJvV-EJZKyBYIoqvh69kfkF4X8DsJuXOZC6emn53SrrZD8P4v9Wp7Nxg" +
        "DYwtEUs3zQkxesaGc6UBgmlkgnY0gmlwhMsAcQGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOA"
    )
    val enr = entry as ENRNode
    val nodeRecord = enr.nodeRecord
    assertNotNull(nodeRecord)
    nodeRecord.validate()
  }

  @Test
  fun testValidENRTreeNode() {
    val entry = DNSEntry.readDNSEntry(
      "enrtree-branch:F4YWVKW4N6B2DDZWFS4XCUQBHY,JTNOVTCP6XZUMXDRANXA6SWXTM," +
        "JGUFMSAGI7KZYB3P7IZW4S5Y3A"
    )
    val enr = entry as ENRTree
    val entries = enr.entries
    assertEquals(
      listOf("F4YWVKW4N6B2DDZWFS4XCUQBHY", "JTNOVTCP6XZUMXDRANXA6SWXTM", "JGUFMSAGI7KZYB3P7IZW4S5Y3A"),
      entries
    )
  }

  @Test
  fun testRootToString() {
    val root = ENRTreeRoot(
      mapOf(
        Pair("enrtree-root", "v1"),
        Pair("l", "TO4Q75OQ2N7DX4EOOR7X66A6OM"),
        Pair("e", "TO4Q75OQ2N7DX4EOOR7X66A6OM"),
        Pair("seq", "3"),
        Pair("sig", "N-YY6UB9xD0hFx1Gmnt7v0RfSxch5tKyry2SRDoLx7B4GfPXagwLxQqyf7gAMvApFn_ORwZQekMWa_pXrcGCtwE=")
      )
    )
    assertEquals(
      "enrtree-root:v1 e=TO4Q75OQ2N7DX4EOOR7X66A6OM " +
        "l=TO4Q75OQ2N7DX4EOOR7X66A6OM seq=3 sig=N-YY6UB9xD0hFx1Gmnt7v0RfSxch5tKyry" +
        "2SRDoLx7B4GfPXagwLxQqyf7gAMvApFn_ORwZQekMWa_pXrcGCtwE=",
      root.toString()
    )
  }

  @Test
  fun testEntryToString() {
    val entry = DNSEntry.readDNSEntry(
      "enrtree-branch:F4YWVKW4N6B2DDZWFS4XCUQBHY,JTNOVTCP6XZUMXDRANXA6SWXTM," +
        "JGUFMSAGI7KZYB3P7IZW4S5Y3A"
    )
    assertEquals(
      "enrtree-branch:F4YWVKW4N6B2DDZWFS4XCUQBHY,JTNOVTCP6XZUMXDRANXA6SWXTM,JGUFMSAGI7KZYB3P7IZW4S5Y3A",
      entry.toString()
    )
  }

  @Test
  fun testEntryLinkToString() {
    val entry = DNSEntry.readDNSEntry(
      "enrtree://AM5FCQLWIZX2QFPNJAP7VUERCCRNGRHWZG3YYHIUV7B" +
        "VDQ5FDPRT2@morenodes.example.org"
    )
    assertEquals(
      "enrtree://AM5FCQLWIZX2QFPNJAP7VUERCCRNGRHWZG3YYHIUV7BVDQ5FDPRT2@morenodes.example.org",
      entry.toString()
    )
  }
}
