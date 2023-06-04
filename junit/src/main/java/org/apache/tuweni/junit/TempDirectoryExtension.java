// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import static java.nio.file.Files.createTempDirectory;
import static org.apache.tuweni.io.file.Files.deleteRecursively;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * A junit5 extension, that provides a temporary directory for tests.
 *
 * <p>The temporary directory is created for the test suite and injected into any tests with
 * parameters annotated by {@link TempDirectory}.
 */
public final class TempDirectoryExtension implements ParameterResolver, AfterAllCallback {

  private Path tempDirectory;

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().isAnnotationPresent(TempDirectory.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (tempDirectory == null) {
      try {
        tempDirectory =
            createTempDirectory(extensionContext.getRequiredTestClass().getSimpleName());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return tempDirectory;
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (tempDirectory != null) {
      deleteRecursively(tempDirectory);
    }
  }
}
