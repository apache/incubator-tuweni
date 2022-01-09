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

  override fun objectToBuffer(o: Any?, estimatedSize: Int): ByteBuffer = ByteBufferImpl.create((o as Bytes).toArrayUnsafe())

  override fun isMarshallable(o: Any?): Boolean = o is Bytes

  override fun mediaType(): MediaType = MediaType.APPLICATION_OCTET_STREAM
}
