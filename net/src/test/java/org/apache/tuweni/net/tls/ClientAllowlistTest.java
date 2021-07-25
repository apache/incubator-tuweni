/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.net.tls;

import static org.apache.tuweni.net.tls.SecurityTestUtils.startServer;
import static org.apache.tuweni.net.tls.TLS.certificateHexFingerprint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.net.ssl.SSLException;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
@ExtendWith(VertxExtension.class)
class ClientAllowlistTest {

  private static String caValidFingerprint;
  private static HttpServer caValidServer;
  private static String fooFingerprint;
  private static HttpServer fooServer;
  private static String barFingerprint;
  private static HttpServer barServer;

  private Path knownServersFile;
  private HttpClient client;

  @BeforeAll
  static void startServers(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    SelfSignedCertificate caSignedCert = SelfSignedCertificate.create("localhost");
    caValidFingerprint = certificateHexFingerprint(Paths.get(caSignedCert.keyCertOptions().getCertPath()));
    SecurityTestUtils.configureJDKTrustStore(tempDir, caSignedCert);
    caValidServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(caSignedCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(caValidServer);

    SelfSignedCertificate fooCert = SelfSignedCertificate.create("foo.com");
    fooFingerprint = certificateHexFingerprint(Paths.get(fooCert.keyCertOptions().getCertPath()));
    fooServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(fooCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(fooServer);

    SelfSignedCertificate barCert = SelfSignedCertificate.create("bar.com");
    barFingerprint = certificateHexFingerprint(Paths.get(barCert.keyCertOptions().getCertPath()));
    barServer = vertx
        .createHttpServer(new HttpServerOptions().setSsl(true).setPemKeyCertOptions(barCert.keyCertOptions()))
        .requestHandler(context -> context.response().end("OK"));
    startServer(barServer);
  }

  @BeforeEach
  void setupClient(@TempDirectory Path tempDir, @VertxInstance Vertx vertx) throws Exception {
    knownServersFile = tempDir.resolve("knownclients.txt");
    Files
        .write(
            knownServersFile,
            Arrays.asList("#First line", "localhost:" + fooServer.actualPort() + " " + fooFingerprint));

    HttpClientOptions options = new HttpClientOptions();
    options
        .setSsl(true)
        .setTrustOptions(VertxTrustOptions.allowlistServers(knownServersFile, false))
        .setConnectTimeout(1500)
        .setReuseAddress(true)
        .setReusePort(true);
    client = vertx.createHttpClient(options);
  }

  @AfterEach
  void cleanupClient() throws Exception {
    client.close();

    List<String> knownServers = Files.readAllLines(knownServersFile);
    assertEquals(2, knownServers.size(), "Host was verified via TOFU and not CA");
    assertEquals("#First line", knownServers.get(0));
    assertEquals("localhost:" + fooServer.actualPort() + " " + fooFingerprint, knownServers.get(1));
  }

  @AfterAll
  static void stopServers() {
    caValidServer.close();
    fooServer.close();
    barServer.close();
    System.clearProperty("javax.net.ssl.trustStore");
    System.clearProperty("javax.net.ssl.trustStorePassword");
  }

  @Test
  void shouldNotValidateUsingCertificate() {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .request(HttpMethod.POST, caValidServer.actualPort(), "localhost", "/sample")
        .onSuccess((req) -> req.send().onSuccess((response) -> statusCode.complete(response.statusCode())))
        .onFailure(statusCode::completeExceptionally);
    Throwable e = assertThrows(CompletionException.class, statusCode::join);
    e = e.getCause();
    while (!(e instanceof CertificateException)) {
      assertTrue(e instanceof SSLException);
      e = e.getCause();
    }
    assertTrue(e.getMessage().contains("has unknown fingerprint " + caValidFingerprint));
  }

  @Test
  void shouldValidateAllowlisted() {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .request(HttpMethod.POST, fooServer.actualPort(), "localhost", "/sample")
        .onSuccess((req) -> req.send().onSuccess((response) -> statusCode.complete(response.statusCode())))
        .onFailure(statusCode::completeExceptionally);
    assertEquals((Integer) 200, statusCode.join());
  }

  @Test
  void shouldRejectNonAllowlisted() {
    CompletableFuture<Integer> statusCode = new CompletableFuture<>();
    client
        .request(HttpMethod.POST, barServer.actualPort(), "localhost", "/sample")
        .onSuccess((req) -> req.send().onSuccess((response) -> statusCode.complete(response.statusCode())))
        .onFailure(statusCode::completeExceptionally);
    Throwable e = assertThrows(CompletionException.class, statusCode::join);
    e = e.getCause();
    while (!(e instanceof CertificateException)) {
      assertTrue(e instanceof SSLException, "Expected SSLException, but got " + e.getClass());
      e = e.getCause();
    }
    assertTrue(e.getMessage().contains("has unknown fingerprint " + barFingerprint));
  }
}
