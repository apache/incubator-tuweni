package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

fun main(args: Array<String>) {
  run3(arrayOf("localhost", "9000"))
}

fun run3(args: Array<String>) {
  val target = InetSocketAddress(InetAddress.getByName(args[0]), args[1].toInt())
  val message =
    Bytes.wrap(ByteArray(65507))
  val toSend = DatagramPacket(message.toArrayUnsafe(), message.size(), target)
  val ds = DatagramSocket()
  ds.send(toSend)
  ds.close()
}


