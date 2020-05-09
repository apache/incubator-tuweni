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
package org.apache.tuweni.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.junit.TempDirectory;
import org.apache.tuweni.junit.TempDirectoryExtension;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class ResourcesTest {

  @Test
  void shouldSplitGlob() {
    assertEquals(Arrays.asList("foo", "*.bar"), Arrays.asList(Resources.globRoot("foo/*.bar")));
    assertEquals(Arrays.asList("foo", "bar.?"), Arrays.asList(Resources.globRoot("foo/bar.?")));
    assertEquals(Arrays.asList("foo/baz", "*.bar"), Arrays.asList(Resources.globRoot("foo/baz/*.bar")));
    assertEquals(Arrays.asList("foo/baz", "bar.?"), Arrays.asList(Resources.globRoot("foo/baz/bar.?")));
    assertEquals(Collections.singletonList("foo/*.bar"), Arrays.asList(Resources.globRoot("foo/\\*.bar")));
    assertEquals(Arrays.asList("foo/*.bar", "*.baz"), Arrays.asList(Resources.globRoot("foo/\\*.bar/*.baz")));
    assertEquals(Arrays.asList("", "*.bar"), Arrays.asList(Resources.globRoot("*.bar")));
    assertEquals(Arrays.asList("", "**/*.bar"), Arrays.asList(Resources.globRoot("**/*.bar")));
  }

  private void copy(Path source, Path dest) {
    try {
      if (!dest.toString().equals("")) {
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  @SuppressWarnings({"MustBeClosedChecker", "StreamResourceLeak"})
  void shouldIterateResourcesOnFileSystemAndInJars(@TempDirectory Path folder) throws Exception {
    Files.createDirectories(folder.resolve("org/apache/tuweni/io/file/resourceresolver"));
    Files.createDirectory(folder.resolve("org/apache/tuweni/io/file/resourceresolver/subdir"));
    Files.createFile(folder.resolve("org/apache/tuweni/io/file/resourceresolver/test.txt"));
    Files.createFile(folder.resolve("org/apache/tuweni/io/file/resourceresolver/test1.txt"));
    Files.createFile(folder.resolve("org/apache/tuweni/io/file/resourceresolver/test2.txt"));
    Files.createFile(folder.resolve("org/apache/tuweni/io/file/resourceresolver/subdir/test3.yaml"));

    Files.createDirectory(folder.resolve("org/apache/tuweni/io/file/resourceresolver/anotherdir"));
    Files.createFile(folder.resolve("org/apache/tuweni/io/file/resourceresolver/anotherdir/test6.yaml"));
    Files.createFile(folder.resolve("org/apache/tuweni/io/file/resourceresolver/anotherdir/test5.txt"));

    URI jarFile = URI.create("jar:" + folder.resolve("resourceresolvertest.jar").toUri());

    try (FileSystem zipfs = FileSystems.newFileSystem(jarFile, Collections.singletonMap("create", "true"));) {
      Files.walk(folder).forEach(source -> copy(source, zipfs.getPath(folder.relativize(source).toString())));
    }
    Files
        .walk(folder.resolve("org/apache/tuweni/io/file/resourceresolver/anotherdir"))
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);

    URLClassLoader classLoader = new URLClassLoader(
        new URL[] {folder.toUri().toURL(), folder.resolve("resourceresolvertest.jar").toUri().toURL()});
    List<URL> all =
        Resources.find(classLoader, "/org/apache/tuweni/io/file/resourceresolver/**").collect(Collectors.toList());

    assertEquals(14, all.size(), () -> describeExpectation(14, all));

    List<URL> txtFiles = Resources.find(classLoader, "org/**/test*.txt").collect(Collectors.toList());
    assertEquals(7, txtFiles.size(), () -> describeExpectation(7, txtFiles));

    List<URL> txtFilesFromRoot = Resources.find(classLoader, "/**/test?.txt").collect(Collectors.toList());
    assertEquals(5, txtFilesFromRoot.size(), () -> describeExpectation(5, txtFilesFromRoot));

    List<URL> txtFilesFromRoot2 = Resources.find(classLoader, "//**/test*.txt").collect(Collectors.toList());
    assertEquals(7, txtFilesFromRoot2.size(), () -> describeExpectation(7, txtFilesFromRoot2));

    List<URL> txtFilesFromRoot3 = Resources.find(classLoader, "///**/test*.txt").collect(Collectors.toList());
    assertEquals(7, txtFilesFromRoot3.size(), () -> describeExpectation(7, txtFilesFromRoot3));

    List<URL> txtFilesInDir = Resources.find(classLoader, "**/anotherdir/*.txt").collect(Collectors.toList());
    assertEquals(1, txtFilesInDir.size(), () -> describeExpectation(1, txtFilesInDir));
  }

  @Nonnull
  private String describeExpectation(int count, List<URL> urls) {
    return "Should have contained "
        + count
        + " items, but got "
        + urls.size()
        + ": \n  "
        + urls.stream().map(URL::toString).collect(Collectors.joining("\n  "));
  }
}
