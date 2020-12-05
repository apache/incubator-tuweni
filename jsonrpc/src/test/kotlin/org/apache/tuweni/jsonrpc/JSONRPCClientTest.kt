package org.apache.tuweni.jsonrpc

import io.ktor.http.cio.Request
import io.ktor.request.ApplicationRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class JSONRPCClientTest {

  companion object {
    val lastMessage = AtomicReference<ApplicationRequest>()
    val server = JSONRPCServer { lastMessage.set(it)}
    @BeforeAll
    fun runServer() = runBlocking {
      server.start()
    }

    @AfterAll
    fun stopServer() = runBlocking {
      server.stop()
    }
  }

  @Test
  fun testVersion() {
    jsonrpc =
  }
}
