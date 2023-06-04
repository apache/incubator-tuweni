// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NoLockFactory;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * A junit5 extension, that provides a memory-backed Lucene index writer for tests.
 *
 * The index writer is created for the test suite and injected into any tests with parameters annotated by
 * {@link LuceneIndexWriter}.
 */
public class LuceneIndexWriterExtension implements ParameterResolver, AfterAllCallback {

  private Directory index;
  private IndexWriter writer;

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().isAnnotationPresent(LuceneIndexWriter.class)
        || parameterContext.getParameter().isAnnotationPresent(LuceneIndex.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (writer == null) {
      try {
        index = new ByteBuffersDirectory(NoLockFactory.INSTANCE);

        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(index, config);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    if (parameterContext.isAnnotated(LuceneIndexWriter.class)) {
      return writer;
    } else {
      return index;
    }
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    if (writer != null) {
      writer.close();
    }
    if (index != null) {
      index.close();
    }
  }


}
