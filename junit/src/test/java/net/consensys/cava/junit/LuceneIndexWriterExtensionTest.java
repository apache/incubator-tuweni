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
package net.consensys.cava.junit;

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
