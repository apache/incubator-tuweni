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
package org.apache.tuweni.net.coroutines;

import static java.nio.channels.SelectionKey.OP_READ;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SelectorTest {

  private static ExecutorService executor;

  @BeforeAll
  static void setup() {
    executor = Executors.newCachedThreadPool();
  }

  @Test
  void selectorRemovesKeysOnChannelCloseWhenSelecting() throws Exception {
    Pipe pipe = Pipe.open();

    Selector selector = Selector.open();
    SelectableChannel source = pipe.source();
    source.configureBlocking(false);

    SelectionKey key = source.register(selector, OP_READ);
    assertTrue(selector.keys().contains(key));

    source.close();
    assertTrue(selector.keys().contains(key));

    selector.selectNow();
    assertFalse(selector.keys().contains(key));
  }

  @Test
  void selectorRemovesKeysOnChannelCloseWhileSelecting() throws Exception {
    Pipe pipe = Pipe.open();

    Selector selector = Selector.open();
    SelectableChannel source = pipe.source();
    source.configureBlocking(false);

    SelectionKey key = source.register(selector, OP_READ);
    assertTrue(selector.keys().contains(key));

    CountDownLatch latch = new CountDownLatch(1);
    Future<?> job = executor.submit(() -> {
      latch.countDown();
      try {
        selector.select();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });

    latch.await();
    Thread.sleep(100);

    source.close();
    selector.wakeup();
    job.get();
    assertFalse(selector.keys().contains(key));
  }

  @Test
  void selectorRemovesKeysOnCancelWhenSelecting() throws Exception {
    Pipe pipe = Pipe.open();

    Selector selector = Selector.open();
    SelectableChannel source = pipe.source();
    source.configureBlocking(false);

    SelectionKey key = source.register(selector, OP_READ);
    assertTrue(selector.keys().contains(key));

    key.cancel();
    assertTrue(selector.keys().contains(key));
    assertSame(key, source.keyFor(selector));

    selector.selectNow();
    assertFalse(selector.keys().contains(key));
    assertNull(source.keyFor(selector));
  }

  @Test
  void selectorRemovesKeysOnCancelWhileSelecting() throws Exception {
    Pipe pipe = Pipe.open();

    Selector selector = Selector.open();
    SelectableChannel source = pipe.source();
    source.configureBlocking(false);

    SelectionKey key = source.register(selector, OP_READ);
    assertTrue(selector.keys().contains(key));

    CountDownLatch latch = new CountDownLatch(1);
    Future<?> job = executor.submit(() -> {
      latch.countDown();
      try {
        selector.select();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });

    latch.await();
    Thread.sleep(100);

    key.cancel();
    assertTrue(selector.keys().contains(key));
    assertSame(key, source.keyFor(selector));

    selector.wakeup();
    job.get();
    assertFalse(selector.keys().contains(key));
    assertNull(source.keyFor(selector));
  }

  @Test
  void cancelledKeyRemovedFromChannel() throws Exception {
    Pipe pipe = Pipe.open();

    Selector selector = Selector.open();
    SelectableChannel source = pipe.source();
    source.configureBlocking(false);

    for (int i = 0; i < 1000; ++i) {
      assertNull(source.keyFor(selector));

      SelectionKey key = source.register(selector, OP_READ);

      selector.selectedKeys().clear();
      selector.selectNow();

      key.cancel();
      selector.wakeup();

      selector.selectedKeys().clear();
      selector.selectNow();
    }
  }
}
