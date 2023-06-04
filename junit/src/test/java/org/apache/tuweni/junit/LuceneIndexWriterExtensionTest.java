// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LuceneIndexWriterExtension.class)
class LuceneIndexWriterExtensionTest {

  @Test
  void shouldHaveAccessToLuceneIndexWriter(@LuceneIndexWriter IndexWriter indexWriter) throws Exception {
    assertTrue(indexWriter.isOpen());
    indexWriter.addDocument(new Document());
    indexWriter.commit();
  }

  @Test
  void shouldHaveAccessToLuceneIndex(@LuceneIndex Directory index) throws Exception {
    assertNotNull(index);
  }
}
