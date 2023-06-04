// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * A junit5 extension, that provides a Vert.X instance for tests.
 *
 * The Vert.X instance created for the test suite and injected into any tests with parameters annotated by
 * {@link VertxInstance}.
 */
public class VertxExtension implements ParameterResolver, AfterAllCallback {

  private Vertx vertx = null;

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().isAnnotationPresent(VertxInstance.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (vertx == null) {
      System.setProperty("vertx.disableFileCPResolving", "true");
      vertx = Vertx.vertx();
    }
    return vertx;
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (vertx != null) {
      vertx.close();
    }
  }
}
