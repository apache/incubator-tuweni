// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.tls;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.tuweni.io.file.Files.deleteRecursively;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class FileBackedFingerprintRepositoryTest {

  private SecureRandom secureRandom = new SecureRandom();

  private Bytes generateFingerprint() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Bytes.wrap(bytes);
  }

  @Test
  void testRelativePath() throws IOException {
    try {
      new FileBackedFingerprintRepository(Paths.get("tmp", "foo"));
    } finally {
      deleteRecursively(Paths.get("tmp"));
    }
  }

  @Test
  void testCaseSensitiveIdentifier(@TempDirectory Path tempFolder) throws IOException {
    Path repoFile = tempFolder.resolve("repo");
    String identifier1 = "foo";
    String identifier2 = "Foo";

    Bytes fingerprint1 = generateFingerprint();
    Bytes fingerprint2 = generateFingerprint();

    String content =
        String.format("%s %s%n%s %s", identifier1, fingerprint1, identifier2, fingerprint2);
    Files.writeString(repoFile, content);

    FileBackedFingerprintRepository repo = new FileBackedFingerprintRepository(repoFile);
    assertTrue(repo.contains(identifier1, fingerprint1));
    assertTrue(repo.contains(identifier2, fingerprint2));
  }

  @Test
  FileBackedFingerprintRepository testAddingNewFingerprint(@TempDirectory Path tempFolder)
      throws IOException {
    FileBackedFingerprintRepository repo =
        new FileBackedFingerprintRepository(tempFolder.resolve("repo"));
    Bytes fingerprint = generateFingerprint();
    repo.addFingerprint("foo", fingerprint);
    assertTrue(repo.contains("foo", fingerprint));
    assertEquals(
        "foo " + fingerprint.toHexString().substring(2).toLowerCase(Locale.ENGLISH),
        Files.readAllLines(tempFolder.resolve("repo")).get(0));
    return repo;
  }

  @Test
  void testUpdateFingerprint(@TempDirectory Path tempFolder) throws IOException {
    FileBackedFingerprintRepository repo = testAddingNewFingerprint(tempFolder);
    Bytes fingerprint = generateFingerprint();
    repo.addFingerprint("foo", fingerprint);
    assertTrue(repo.contains("foo", fingerprint));
    assertEquals(
        "foo " + fingerprint.toHexString().substring(2).toLowerCase(Locale.ENGLISH),
        Files.readAllLines(tempFolder.resolve("repo")).get(0));
  }

  @Test
  void testInvalidFingerprintAddedToFile(@TempDirectory Path tempFolder) throws IOException {
    FileBackedFingerprintRepository repo =
        new FileBackedFingerprintRepository(tempFolder.resolve("repo-bad2"));
    Bytes fingerprint = generateFingerprint();
    Files.write(
        tempFolder.resolve("repo-bad2"),
        ("bar " + fingerprint.slice(8).toHexString().substring(2) + "GGG").getBytes(UTF_8));
    assertThrows(TLSEnvironmentException.class, () -> repo.addFingerprint("foo", fingerprint));
  }
}
