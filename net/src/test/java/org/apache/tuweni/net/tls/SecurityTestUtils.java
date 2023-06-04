// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.tls;

import static org.apache.tuweni.net.tls.TLS.readPemFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.http.HttpServer;
import io.vertx.core.net.SelfSignedCertificate;

class SecurityTestUtils {
  private SecurityTestUtils() {}

  static final String DUMMY_FINGERPRINT = "1111111111111111111111111111111111111111111111111111111111111111";

  static void configureJDKTrustStore(Path workDir, SelfSignedCertificate clientCert) throws Exception {
    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, null);

    KeyFactory kf = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keysp = new PKCS8EncodedKeySpec(readPemFile(new File(clientCert.privateKeyPath()).toPath()));
    PrivateKey clientPrivateKey = kf.generatePrivate(keysp);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    Certificate certificate = cf
        .generateCertificate(
            new ByteArrayInputStream(Files.readAllBytes(new File(clientCert.certificatePath()).toPath())));
    ks.setCertificateEntry("clientCert", certificate);
    ks.setKeyEntry("client", clientPrivateKey, "changeit".toCharArray(), new Certificate[] {certificate});
    Path tempKeystore = Files.createTempFile(workDir, "keystore", ".jks");
    try (FileOutputStream output = new FileOutputStream(tempKeystore.toFile());) {
      ks.store(output, "changeit".toCharArray());
    }
    System.setProperty("javax.net.ssl.trustStore", tempKeystore.toString());
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
  }

  static void configureAndStartTestServer(HttpServer httpServer) {
    httpServer.requestHandler(request -> {
      request.response().setStatusCode(200).end("OK");
    });
    startServer(httpServer);
  }

  static void startServer(HttpServer server) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    server.listen(0, result -> {
      if (result.succeeded()) {
        future.complete(true);
      } else {
        future.completeExceptionally(result.cause());
      }
    });
    future.join();
  }
}
