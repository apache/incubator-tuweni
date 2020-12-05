package org.apache.tuweni.jsonrpc
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
class JSONRPCServer(private val callback: (ApplicationRequest) -> Unit) {

  var server : NettyApplicationEngine? = null

  suspend fun start() {
    server = embeddedServer(Netty, port = 0) {
      routing {
        post("/") {
          callback(call.request)
        }
      }
    }
    server!!.start()
  }

  suspend fun stop() {
    server!!.stop(1000, 1000)
  }
}
