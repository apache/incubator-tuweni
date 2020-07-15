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
package org.apache.tuweni.io.file;

import static org.apache.tuweni.io.file.Files.atomicReplace;
import static org.apache.tuweni.io.file.Files.copyResource;
import static org.apache.tuweni.io.file.Files.createFileIfMissing;
import static org.apache.tuweni.io.file.Files.deleteRecursively;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class FilesTest {

  @Test
  void deleteRecursivelyShouldDeleteEverything() throws Exception {
    Path directory = Files.createTempDirectory(this.getClass().getSimpleName());
    Path testData = directory.resolve("test_data");
    Files.createFile(testData);

    Path testDir = directory.resolve("test_dir");
    Files.createDirectory(testDir);
    Path testData2 = testDir.resolve("test_data");
    Files.createFile(testData2);

    assertTrue(Files.exists(directory));
    assertTrue(Files.exists(testData));
    assertTrue(Files.exists(testDir));
    assertTrue(Files.exists(testData2));

    deleteRecursively(directory);

    assertFalse(Files.exists(directory));
    assertFalse(Files.exists(testData));
    assertFalse(Files.exists(testDir));
    assertFalse(Files.exists(testData2));
  }

  @Test
  void canCopyResources(@TempDirectory Path tempDir) throws Exception {
    Files.createDirectories(tempDir.resolve("org/something"));
    Files.write(tempDir.resolve("org/something/test.txt"), "foo".getBytes(StandardCharsets.UTF_8));
    URLClassLoader classLoader = new URLClassLoader(new URL[] {new URL("file:" + tempDir.toString() + "/")});

    Path file = copyResource(classLoader, "org/something/test.txt", tempDir.resolve("test.txt"));
    assertTrue(Files.exists(file));
    assertEquals(3, Files.size(file));
  }

  @Test
  void canReplaceAtomic(@TempDirectory Path tempDir) throws Exception {
    Files.createDirectories(tempDir.resolve("org/something"));
    Files.write(tempDir.resolve("org/something/test.txt"), "foo".getBytes(StandardCharsets.UTF_8));

    atomicReplace(Paths.get("org/something/test.txt"), "hello world".getBytes(StandardCharsets.UTF_8));
    assertEquals(11, Files.size(Paths.get("org/something/test.txt")));
  }

  @Test
  void canReplaceAtomicWriter(@TempDirectory Path tempDir) throws Exception {
    Files.createDirectories(tempDir.resolve("org/something"));
    Files.write(tempDir.resolve("org/something/test.txt"), "foo".getBytes(StandardCharsets.UTF_8));

    atomicReplace(Paths.get("org/something/test.txt"), (writer) -> {
      writer.write("hello world");
    });
    assertEquals(11, Files.size(Paths.get("org/something/test.txt")));
  }

  @Test
  void testCreateFileIfMissing(@TempDirectory Path tempDir) throws Exception {
    Path p = tempDir.resolve("foo");
    assertFalse(p.toFile().exists());
    createFileIfMissing(p);
    assertTrue(p.toFile().exists());
    assertEquals(0, Files.size(p));
    Files.write(p, "foo".getBytes(StandardCharsets.UTF_8));
    assertEquals(3, Files.size(p));
    createFileIfMissing(p);
    assertEquals(3, Files.size(p));
  }
}
