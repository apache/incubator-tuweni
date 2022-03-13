/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.kv

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.RedisCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import org.apache.tuweni.bytes.Bytes
import org.checkerframework.checker.units.qual.K
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.CompletionStage
import java.util.function.Function
import kotlin.coroutines.CoroutineContext

/**
 * A key-value store backed by Redis.
 *
 * @param uri The uri to the Redis store.
 * @param keySerializer the serializer of key objects to bytes
 * @param valueSerializer the serializer of value objects to bytes
 * @param keyDeserializer the deserializer of keys from bytes
 * @param valueDeserializer the deserializer of values from bytes
 * @param coroutineContext the co-routine context in which this store executes
 * @constructor Open a Redis-backed key-value store.
 */
class RedisKeyValueStore<K, V>(
  uri: String,
  private val keySerializer: (K) -> Bytes,
  private val valueSerializer: (V) -> Bytes,
  private val keyDeserializer: (Bytes) -> K,
  private val valueDeserializer: (Bytes) -> V,
  override val coroutineContext: CoroutineContext = Dispatchers.IO
) : KeyValueStore<K, V> {

  companion object {

    /**
     * Open a Redis-backed key-value store using Bytes keys and values.
     *
     * @param uri The uri to the Redis store.
     * @return A key-value store dealing with bytes.
     * @throws IOException If an I/O error occurs.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun open(
      uri: String
    ) =
      RedisKeyValueStore<Bytes, Bytes>(
        uri,
        Function.identity<Bytes>()::apply,
        Function.identity<Bytes>()::apply,
        Function.identity<Bytes>()::apply,
        Function.identity<Bytes>()::apply
      )

    /**
     * Open a Redis-backed key-value store.
     *
     * @param uri The uri to the Redis store.
     * @param keySerializer the serializer of key objects to bytes
     * @param valueSerializer the serializer of value objects to bytes
     * @param keyDeserializer the deserializer of keys from bytes
     * @param valueDeserializer the deserializer of values from bytes
     * @return A key-value store.
     */
    @JvmStatic
    fun <K, V> open(
      uri: String,
      keySerializer: Function<K, Bytes>,
      valueSerializer: Function<V, Bytes>,
      keyDeserializer: Function<Bytes, K>,
      valueDeserializer: Function<Bytes, V>
    ) = RedisKeyValueStore(
      uri,
      keySerializer::apply,
      valueSerializer::apply,
      keyDeserializer::apply,
      valueDeserializer::apply
    )

    /**
     * Open a Redis-backed key-value store.
     *
     * @param port The port for the Redis store.
     * @param keySerializer the serializer of key objects to bytes
     * @param valueSerializer the serializer of value objects to bytes
     * @param keyDeserializer the deserializer of keys from bytes
     * @param valueDeserializer the deserializer of values from bytes
     * @return A key-value store.
     */
    @JvmStatic
    fun <K, V> open(
      port: Int,
      keySerializer: Function<K, Bytes>,
      valueSerializer: Function<V, Bytes>,
      keyDeserializer: Function<Bytes, K>,
      valueDeserializer: Function<Bytes, V>
    ) =
      RedisKeyValueStore(
        port = port,
        keySerializer = keySerializer::apply,
        valueSerializer = valueSerializer::apply,
        keyDeserializer = keyDeserializer::apply,
        valueDeserializer = valueDeserializer::apply
      )

    /**
     * Open a Redis-backed key-value store.
     *
     * @param address The address for the Redis store.
     * @param keySerializer the serializer of key objects to bytes
     * @param valueSerializer the serializer of value objects to bytes
     * @param keyDeserializer the deserializer of keys from bytes
     * @param valueDeserializer the deserializer of values from bytes
     * @return A key-value store.
     */
    @JvmStatic
    fun <K, V> open(
      address: InetAddress,
      keySerializer: Function<K, Bytes>,
      valueSerializer: Function<V, Bytes>,
      keyDeserializer: Function<Bytes, K>,
      valueDeserializer: Function<Bytes, V>
    ) =
      RedisKeyValueStore(
        6379,
        address,
        keySerializer::apply,
        valueSerializer::apply,
        keyDeserializer::apply,
        valueDeserializer::apply
      )

    /**
     * Open a Redis-backed key-value store.
     *
     * @param port The port for the Redis store.
     * @param address The address for the Redis store.
     * @param keySerializer the serializer of key objects to bytes
     * @param valueSerializer the serializer of value objects to bytes
     * @param keyDeserializer the deserializer of keys from bytes
     * @param valueDeserializer the deserializer of values from bytes
     * @return A key-value store.
     */
    @JvmStatic
    fun <K, V> open(
      port: Int,
      address: InetAddress,
      keySerializer: Function<K, Bytes>,
      valueSerializer: Function<V, Bytes>,
      keyDeserializer: Function<Bytes, K>,
      valueDeserializer: Function<Bytes, V>
    ) =
      RedisKeyValueStore(
        port,
        address,
        keySerializer::apply,
        valueSerializer::apply,
        keyDeserializer::apply,
        valueDeserializer::apply
      )

    /**
     * A [RedisCodec] for working with Bytes classes.
     *
     * @return A [RedisCodec] for working with Bytes classes.
     */
    @JvmStatic
    fun codec(): RedisCodec<Bytes, Bytes> = RedisBytesCodec()
  }

  private val conn: StatefulRedisConnection<Bytes, Bytes>
  private val asyncCommands: RedisAsyncCommands<Bytes, Bytes>

  /**
   * Open a Redis-backed key-value store.
   *
   * @param port The port for the Redis store.
   * @param address The address for the Redis store.
   * @param keySerializer the serializer of key objects to bytes
   * @param valueSerializer the serializer of value objects to bytes
   * @param keyDeserializer the deserializer of keys from bytes
   * @param valueDeserializer the deserializer of values from bytes
   */
  @JvmOverloads
  constructor(
    port: Int = 6379,
    address: InetAddress = InetAddress.getLoopbackAddress(),
    keySerializer: (K) -> Bytes,
    valueSerializer: (V) -> Bytes,
    keyDeserializer: (Bytes) -> K,
    valueDeserializer: (Bytes) -> V
  ) : this(
    RedisURI.create(address.hostAddress, port).toURI().toString(),
    keySerializer,
    valueSerializer,
    keyDeserializer,
    valueDeserializer
  )

  init {
    val redisClient = RedisClient.create(uri)
    conn = redisClient.connect(codec())
    asyncCommands = conn.async()
  }

  override suspend fun containsKey(key: K) = asyncCommands.get(keySerializer(key)) != null

  override suspend fun get(key: K): V? = asyncCommands.get(keySerializer(key)).thenApply {
    if (it == null) {
      null
    } else {
      valueDeserializer(it)
    }
  }.await()

  override suspend fun put(key: K, value: V) {
    val future: CompletionStage<String> = asyncCommands.set(keySerializer(key), valueSerializer(value))
    future.await()
  }

  override suspend fun remove(key: K) {
    val future: CompletionStage<Long> = asyncCommands.del(keySerializer(key))
    future.await()
  }

  override suspend fun keys(): Iterable<K> = asyncCommands.keys(Bytes.EMPTY).await().map(keyDeserializer)

  override suspend fun clear() {
    asyncCommands.flushdb().await()
  }

  /**
   * Close the store
   */
  override fun close() {
    conn.close()
  }
}
