// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.kv

import org.apache.tuweni.bytes.Bytes
import org.infinispan.commons.dataconversion.MediaType
import org.infinispan.commons.io.ByteBuffer
import org.infinispan.commons.io.ByteBufferImpl
import org.infinispan.commons.marshall.AbstractMarshaller

/**
 * Utility class to store Bytes objects in Infinispan key-value stores.
 */
class PersistenceMarshaller : AbstractMarshaller() {

  override fun objectFromByteBuffer(buf: ByteArray?, offset: Int, length: Int) = Bytes.wrap(buf!!, offset, length)

  override fun objectToBuffer(o: Any?, estimatedSize: Int): ByteBuffer = ByteBufferImpl.create(
    (o as Bytes).toArrayUnsafe()
  )

  override fun isMarshallable(o: Any?): Boolean = o is Bytes

  override fun mediaType(): MediaType = MediaType.APPLICATION_OCTET_STREAM
}
