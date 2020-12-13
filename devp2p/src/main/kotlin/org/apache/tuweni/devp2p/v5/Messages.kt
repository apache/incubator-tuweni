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
package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.rlp.RLP
import java.net.InetAddress

internal class FindNodeMessage(
  val requestId: Bytes = Message.requestId(),
  val distance: Int = 0
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x03")

  override fun toRLP(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeInt(distance)
    }
  }

  override fun type(): MessageType = MessageType.FINDNODE

  companion object {
    fun create(content: Bytes): FindNodeMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val distance = reader.readInt()
        return@decodeList FindNodeMessage(requestId, distance)
      }
    }
  }
}

internal class NodesMessage(
  val requestId: Bytes = Message.requestId(),
  val total: Int,
  val nodeRecords: List<EthereumNodeRecord>
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x04")

  override fun type(): MessageType = MessageType.NODES

  override fun toRLP(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeInt(total)
      writer.writeList(nodeRecords) { listWriter, it ->
        listWriter.writeRLP(it.toRLP())
      }
    }
  }

  companion object {
    fun create(content: Bytes): NodesMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val total = reader.readInt()
        val nodeRecords = reader.readListContents { listReader ->
          listReader.readList { enrReader ->
            EthereumNodeRecord.fromRLP(enrReader)
          }
        }
        return@decodeList NodesMessage(requestId, total, nodeRecords)
      }
    }
  }
}

internal class PingMessage(
  val requestId: Bytes = Message.requestId(),
  val enrSeq: Long = 0
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x01")

  override fun type(): MessageType = MessageType.PING

  override fun toRLP(): Bytes {
    return RLP.encodeList { reader ->
      reader.writeValue(requestId)
      reader.writeLong(enrSeq)
    }
  }

  companion object {
    fun create(content: Bytes): PingMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val enrSeq = reader.readLong()
        return@decodeList PingMessage(requestId, enrSeq)
      }
    }
  }
}

internal class RandomMessage(
  val authTag: Bytes = Message.authTag(),
  val data: Bytes = randomData()
) : Message {

  companion object {
    fun randomData(): Bytes = Bytes.random(Message.RANDOM_DATA_LENGTH)

    fun create(authTag: Bytes, content: Bytes = randomData()): RandomMessage {
      return RandomMessage(authTag, content)
    }
  }

  override fun type(): MessageType = MessageType.RANDOM

  override fun toRLP(): Bytes {
    return data
  }
}

internal class TicketMessage(
  val requestId: Bytes = Message.requestId(),
  val ticket: Bytes,
  val waitTime: Long
) : Message {

  override fun type(): MessageType = MessageType.TICKET

  override fun toRLP(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeValue(ticket)
      writer.writeLong(waitTime)
    }
  }

  companion object {
    fun create(content: Bytes): TicketMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val ticket = reader.readValue()
        val waitTime = reader.readLong()
        return@decodeList TicketMessage(requestId, ticket, waitTime)
      }
    }
  }
}

internal class WhoAreYouMessage(
  val magic: Bytes,
  val token: Bytes,
  val idNonce: Bytes,
  val enrSeq: Long = 0
) : Message {

  companion object {
    fun create(magic: Bytes, content: Bytes): WhoAreYouMessage {
      return RLP.decodeList(content) { r ->
        val token = r.readValue()
        val idNonce = r.readValue()
        val enrSeq = r.readValue()
        WhoAreYouMessage(magic = magic, token = token, idNonce = idNonce, enrSeq = enrSeq.toLong())
      }
    }
  }

  override fun type(): MessageType = MessageType.WHOAREYOU

  override fun toRLP(): Bytes {
    return Bytes.concatenate(
      magic,
      RLP.encodeList { w ->
        w.writeValue(token)
        w.writeValue(idNonce)
        w.writeLong(enrSeq)
      }
    )
  }
}

internal class TopicQueryMessage(
  val requestId: Bytes = Message.requestId(),
  val topic: Bytes
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x08")

  override fun type(): MessageType = MessageType.TOPICQUERY

  override fun toRLP(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeValue(topic)
    }
  }

  companion object {
    fun create(content: Bytes): TopicQueryMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val topic = reader.readValue()
        return@decodeList TopicQueryMessage(requestId, topic)
      }
    }
  }
}

/**
 * Message to register a topic.
 */
internal class RegTopicMessage(
  val requestId: Bytes = Message.requestId(),
  val nodeRecord: EthereumNodeRecord,
  val topic: Bytes,
  val ticket: Bytes
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x05")

  override fun type(): MessageType = MessageType.REGTOPIC

  override fun toRLP(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeRLP(nodeRecord.toRLP())
      writer.writeValue(topic)
      writer.writeValue(ticket)
    }
  }

  companion object {
    fun create(content: Bytes): RegTopicMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val nodeRecord = reader.readList { enrReader ->
          EthereumNodeRecord.fromRLP(enrReader)
        }
        val topic = reader.readValue()
        val ticket = reader.readValue()
        return@decodeList RegTopicMessage(requestId, nodeRecord, topic, ticket)
      }
    }
  }
}

internal class PongMessage(
  val requestId: Bytes = Message.requestId(),
  val enrSeq: Long = 0,
  val recipientIp: String,
  val recipientPort: Int
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x02")

  override fun type(): MessageType = MessageType.PONG

  override fun toRLP(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeLong(enrSeq)

      val bytesIp = Bytes.wrap(InetAddress.getByName(recipientIp).address)
      writer.writeValue(bytesIp)
      writer.writeInt(recipientPort)
    }
  }

  companion object {
    fun create(content: Bytes): PongMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val enrSeq = reader.readLong()
        val address = InetAddress.getByAddress(reader.readValue().toArray())
        val recipientPort = reader.readInt()
        return@decodeList PongMessage(requestId, enrSeq, address.hostAddress, recipientPort)
      }
    }
  }
}

internal class RegConfirmationMessage(
  val requestId: Bytes = Message.requestId(),
  val topic: Bytes
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x07")

  override fun type(): MessageType = MessageType.REGCONFIRM

  override fun toRLP(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeValue(topic)
    }
  }

  companion object {
    fun create(content: Bytes): RegConfirmationMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val topic = reader.readValue()
        return@decodeList RegConfirmationMessage(requestId, topic)
      }
    }
  }
}
