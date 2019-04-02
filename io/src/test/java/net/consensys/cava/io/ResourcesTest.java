/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

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

  @Test
  @SuppressWarnings("MustBeClosedChecker")
  void shouldIterateResourcesOnFileSystemAndInJars() throws Exception {
    List<URL> all = Resources.find("net/consensys/cava/io/file/resourceresolver/**").collect(Collectors.toList());
    assertEquals(12, all.size(), () -> describeExpectation(12, all));

    List<URL> txtFiles = Resources.find("net/**/test*.txt").collect(Collectors.toList());
    assertEquals(6, txtFiles.size(), () -> describeExpectation(6, txtFiles));

    List<URL> txtFilesFromRoot = Resources.find("/**/test?.txt").collect(Collectors.toList());
    assertEquals(5, txtFilesFromRoot.size(), () -> describeExpectation(5, txtFilesFromRoot));

    List<URL> txtFilesFromRoot2 = Resources.find("//**/test*.txt").collect(Collectors.toList());
    assertEquals(6, txtFilesFromRoot2.size(), () -> describeExpectation(6, txtFilesFromRoot2));

    List<URL> txtFilesFromRoot3 = Resources.find("///**/test*.txt").collect(Collectors.toList());
    assertEquals(6, txtFilesFromRoot3.size(), () -> describeExpectation(6, txtFilesFromRoot3));

    List<URL> txtFilesInDir = Resources.find("**/anotherdir/*.txt").collect(Collectors.toList());
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
