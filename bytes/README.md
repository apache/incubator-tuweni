<!---
Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at
 *
http://www.apache.org/licenses/LICENSE-2.0
 *
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
 --->
# Bytes

| Status         |           |
|----------------|-----------|
| Stability      | [stable]  |
| Component Type | [library] |

# Getting started

Apache Tuweni provides support for manipulating bytes.

To get started, install the `bytes` library.

With Maven:

```xml
<dependency>
  <groupId>org.apache.tuweni</groupId>
  <artifactId>bytes</artifactId>
  <version>2.3.1</version> <!-- replace with latest release -->
</dependency>
```

Or using Gradle:

```groovy
implementation("org.apache.tuweni:bytes:2.3.1") // replace with latest release
```

The [bytes library](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/index.html) revolves mainly around the [`Bytes`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/index.html) interface.

This tutorial goes over the main uses of `Bytes`, from creating them to manipulating them.

# Creating Bytes

## From a bytes array:

You can create `Bytes` objects by wrapping a native byte array:

```java
Bytes bytes = Bytes.wrap(new byte[] {1, 2, 3, 4});
```

Note the underlying array is not copied - any change to the byte array will change the Bytes object's behavior.

You can also wrap with an offset and a size to select a portion of the array:

```java
// wrapping with an offset of 2 and a size of one byte
Bytes bytes = Bytes.wrap(new byte[] {1, 2, 3, 4}, 2, 1);
```

## From a hex string:

You can create `Bytes` objects from a hexadecimal-encoded string with the [`fromHexString`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/from-hex-string.html) method:

```java
Bytes bytes = Bytes.fromHexString("0xdeadbeef");
```

The `"0x"` prefix is optional.

However, this requires an even-sized string. For example, this succeeds:

```java
Bytes bytes = Bytes.fromHexString("01FF2A");
```

This fails:

```java
Bytes bytes = Bytes.fromHexString("1FF2A");
```

You can circumvent this with the [`fromHexStringLenient` method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/from-hex-string-lenient.html):

```java
Bytes bytes = Bytes.fromHexStringLenient("1FF2A");
```

## From a base64-encoded string:

You can create `Bytes` objects from a base64-encoded string with the [`fromBase64String`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/from-base64-string.html) method:

```java
Bytes value = Bytes.fromBase64String("deadbeefISDAbest");
```

# From primitive types

We also have convenience methods to create `Bytes` objects from primitive types.

* [Bytes.of()](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/of.html)

[Bytes.of()](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/of.html) takes a variadic argument of bytes:

```java
Bytes value = Bytes.of(0x00, 0x01, 0xff, 0x2a);
```

* [Bytes.ofUnsignedInt()](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/of-unsigned-int.html)
* [Bytes.ofUnsignedLong()](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/of-unsigned-long.html)
* [Bytes.ofUnsignedShort()](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/of-unsigned-short.html)

```java
Bytes value = Bytes.ofUnsignedInt(42);
```

## More wrapping
### Use [`Bytes.wrapByteBuf(buffer)`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/wrap-byte-buf.html) to wrap a Netty `ByteBuf` object as a `Bytes` object.

```java
ByteBuf buffer = Unpooled.buffer(42);
Bytes.wrapByteBuf(buffer);
```

You can apply an offset and size parameter:

```java
Bytes value = Bytes.wrapByteBuf(buffer, 1, 1);
```

### Use [`Bytes.wrapByteBuffer(buffer)`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/wrap-byte-buffer.html) to wrap a `ByteBuffer` object as a `Bytes` object.

```java
Bytes.wrapByteBuffer(buffer);
```

You can apply an offset and size parameter:

```java
Bytes value = Bytes.wrapByteBuffer(buffer, 1, 1);
```

### Use [`Bytes.wrapBuffer(buffer)`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/wrap-byte-buffer.html) to wrap a Vert.x `Buffer` object as a `Bytes` object.

```java
Bytes.wrapBuffer(buffer);
```

You can apply an offset and size parameter:

```java
Bytes value = Bytes.wrapBuffer(buffer, 1, 1);
```

## Random

You can create random bytes objects of a given length with the [Bytes.random() method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/random.html):

```java
// create a Bytes object of 20 bytes:
Bytes.random(20);
```

Create a Bytes object with our own Random implementation:

```java
Random random = new SecureRandom();
...
Bytes.random(20, random);
```

# Manipulating bytes

## Concatenating and wrapping

You can [concatenate](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/concatenate.html) or [wrap](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/wrap.html) bytes objects.

When concatenating, the underlying objects are copied into a new bytes object.

When wrapping, you are creating a *view* made of the underlying bytes objects. If their values change, the wrapped view changes as well.

Of course, wrapping is preferrable to avoid copying bytes in memory.

## Copying and slicing

In the same spirit as above, you can [copy](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/copy.html) or [slice](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/slice.html) bytes objects. When you slice a bytes object, you create a view of the original bytes object, and the slice will change if the underlying bytes object changes. If you copy instead, you create a new copy of the bytes.

```java
// slice from the second byte:
bytes.slice(2);
// slice from the second byte to the fifth byte:
bytes.slice(2, 5);
```

## Shifting bytes

You can shift [right](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/shift-right.html) and [left](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/shift-left.html) the bits of a bytes object by a given distance.

This is equivalent to the `<<<` or `>>>` operators in Java.

## xor, or, and

You can apply boolean operations to Bytes objects.

* [xor](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/xor.html)
* [or](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/or.html)
* [and](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/and.html)

Those methods take as argument the value to compare this value with, and return a new Bytes object that is the result of the boolean operation.

If the argument and the value are different lengths, then the shorter will be zero-padded to the left.

```java
Bytes value = Bytes.fromHexString("0x01000001").xor(Bytes.fromHexString("0x01000000"));
assertEquals(Bytes.fromHexString("0x00000001"), value);
```

## not

The [`not` method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/not.html) returns a bit-wise NOT of the bytes.

```java
Bytes value = Bytes.fromHexString("0x01000001").not();
assertEquals(Bytes.fromHexString("0xfefffffe"), value);
```

## commonPrefix

The [`commonPrefix` method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/common-prefix.html) returns the common bytes both the value and the argument start with.

# Extracting values
You can extract values from a bytes object into native Java objects such as ints and longs, bytes, byte arrays and so on.

Note all the methods here take an optional `ByteOrder` argument, defaulting to big endian by default.

## toInt() and toLong()

The [method `toInt()`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-int.html) and the [method `toLong()`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-long.html) respectively translate the bytes values into an int or a long, requiring respectively the value to be at most 4 or 8 bytes long.

## get(i)

The [`get(i)` method](/docs/org.apache.tuweni.bytes/-bytes/get.html) provides the byte at index `i`.

## getInt(i) and getLong(i)

The [method `getInt()`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/get-int.html) and the [method `getLong()`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/get-long.html) respectively return the next 4 or 8 bytes into an int or a long.

## toArray() and toArrayUnsafe()

The [method `toArray`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-array.html) copies the bytes of the object into a new bytes array.

The [method `toArrayUnsafe`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-array-unsafe.html) makes available the underlying byte array of the object - modifying it changes the Bytes object. Note this is more performant as it doesn't allocate new memory.

## To BigIntegers

The [method `toUnsignedBigInteger`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-unsigned-big-integer.html) creates a new unsigned BigInteger object with the contents of the Bytes object.
You can also use the method [`toBigInteger`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-big-integer.html) to represent Bytes as a signed integer, using the two's-complement representation.

## Transforming Bytes into strings

There is a sleuth of options to turn bytes into strings, and they all have different use cases.

* The [method `toHexString`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-hex-string.html) provides the value represented as hexadecimal, starting with "0x".
* The [method `toUnprefixedHexString`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-unprefixed-hex-string.html) provides the value represented as hexadecimal, no "0x" prefix though.
* The [method `toShortHexString`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-short-hex-string.html) provides the value represented as a minimal hexadecimal string (without any leading zero).
* The [method `toQuantityHexString`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-quantity-hex-string.html) provides the value represented as a minimal hexadecimal string (without any leading zero, except if it's valued zero or empty, in which case it returns 0x0).
* The [method `toEllipsisHexString`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/to-ellipsis-hex-string.html) provides the first 3 bytes and last 3 bytes represented as hexadecimal strings, joined with an ellipsis (`...`).

By default, `toString()` calls `toHexString()`.

# Mutable Bytes

By default, bytes objects are not mutable. You can use [`MutableBytes` objects](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-mutable-bytes/index.html) instead.

## Creating MutableBytes objects

The methods described in the [tutorial "Creating Bytes"](https://tuweni.apache.org/tutorials/creating-bytes) all work in the same way for `MutableBytes`.

You can call the [method `mutableCopy()`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-mutable-bytes/mutable-copy.html) on any Bytes object to get a copy of the Bytes object as mutable.

Finally, you can create fresh objects with the [`create()` method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-mutable-bytes/create.html).

## Fill, Clear

Fill a MutableBytes with the same byte the [fill method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-mutable-bytes/fill.html):

```java
MutableBytes bytes = MutableBytes.create(2);
bytes.fill((byte) 34);
assertEquals(Bytes.fromHexString("0x2222"), bytes);
```

You can clear the contents with the [`clear` method](/docs/org.apache.tuweni.bytes/-mutable-bytes/clear.html):

```java
MutableBytes bytes = MutableBytes.fromHexString("0xdeadbeef");
bytes.clear();
```

## Setting values

You can set values with different arguments:

* The [`set(int i, byte b)` method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-mutable-bytes/set.html) sets the value of a byte at index `i`.
* The [`setInt(int i, int value)` method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-mutable-bytes/set-int.html) sets the value of the next four bytes at index `i`.
* The [`setLong(int i, long value)` method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-mutable-bytes/set-long.html) sets the value of the next eight bytes at index `i`.
* The [`set(int offset, Bytes bytes)` method](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-mutable-bytes/set.html) sets the value of the next bytes at index `i`.

# Your own Bytes class

You can create your very own implementation of `Bytes` by extending the [`AbstractBytes` class](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-abstract-bytes/index.html).

You will need to implement the following functions:
* [`get(i)`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/get.html)
* [`size()`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/size.html)
* [`slice(offset, size)`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/slice.html)
* [`copy(offset, size)`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/copy.html)
* [`mutableCopy(offset, size)`](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-bytes/mutable-copy.html)

You can choose to simplify greatly by extending the [`DelegatingBytes` class](https://tuweni.apache.org/docs/org.apache.tuweni.bytes/-delegating-bytes/index.html) instead.

[stable]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#stable
[library]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#library