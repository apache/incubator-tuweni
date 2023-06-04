// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class TempDirectoryExtensionTest {

  @Test
  void shouldHaveAccessToATemporaryDirectory(@TempDirectory Path tempDir) throws Exception {
    assertTrue(Files.exists(tempDir));
    assertTrue(Files.isDirectory(tempDir));
    Files.createFile(tempDir.resolve("file"));
  }
}
