// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.kv

import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import redis.clients.jedis.JedisPool
import java.io.IOException
import java.net.InetAddress
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
  }

  private val conn: JedisPool

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
    "redis://${address.hostAddress}:$port",
    keySerializer,
    valueSerializer,
    keyDeserializer,
    valueDeserializer
  )

  init {
    conn = JedisPool(uri)
  }

  override suspend fun containsKey(key: K) = conn.resource.use {
    it.get(keySerializer(key).toArrayUnsafe()) != null
  }

  override suspend fun get(key: K): V? {
    val keyBytes = keySerializer(key).toArrayUnsafe()
    return conn.resource.use {
      val value = it[keyBytes]
      value?.let { valueDeserializer(Bytes.wrap(it)) }
    }
  }

  override suspend fun put(key: K, value: V) {
    conn.resource.use {
      it[keySerializer(key).toArrayUnsafe()] = valueSerializer(value).toArrayUnsafe()
    }
  }

  override suspend fun remove(key: K) {
    conn.resource.use {
      it.del(keySerializer(key).toArrayUnsafe())
    }
  }

  override suspend fun keys(): Iterable<K> {
    return conn.resource.use {
      it.keys(ByteArray(0)).map {
        keyDeserializer(Bytes.wrap(it))
      }
    }
  }

  override suspend fun clear() {
    conn.resource.use {
      it.flushDB()
    }
  }

  /**
   * Close the store
   */
  override fun close() {
    conn.close()
  }
}
