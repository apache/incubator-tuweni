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
# Units

# Getting started

Apache Tuweni provides support for manipulating unsigned integers and base Ethereum currencies.

To get started, install the `units` library.

With Maven:

```xml
<dependency>
  <groupId>org.apache.tuweni</groupId>
  <artifactId>units</artifactId>
  <version>2.3.1</version> <!-- replace with latest release -->
</dependency>
```

Or using Gradle:

```groovy
implementation("org.apache.tuweni:units:2.3.1") // replace with latest release
```

The [units library](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/index.html) revolves mainly around the [`Uint256`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/index.html), [`Uint384`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int384/index.html)  and [`Uint64`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int64/index.html)  interfaces.

This tutorial goes over the main uses of `Uint256` - you can apply the same behaviors over to `Uint384`, `Uint64` or `Uint32`.

It will also touch on `Wei` and `Gas`.

# Java API 

NOTE: We are going to use the [`UInt256` class](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/index.html) for all examples, but the same behaviors are possible with the [`UInt384`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int384/index.html), [`UInt64`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int64/index.html), [`UInt32`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int32/index.html) classes.

We refer to those classes as `UInt`.

# Creating Uints

## `valueOf`

You can initialize a `UInt` with the [static method `valueOf`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/value-of.html), with an integer, a long, or a BigInteger object. This only accepts positive values.

```java
// from an int
UInt256 value = UInt256.valueOf(42);
// from a long
UInt256 value = UInt256.valueOf(42L);
// from a BigInteger
UInt256 value = UInt256.valueOf(BigInteger.ONE);
```

## `fromBytes`

You can initialize a `UInt` from a `Bytes` object, using the [`fromBytes` method](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/from-bytes.html).

```java
UInt256 value = UInt256.fromBytes(Bytes.wrap(new byte[] {0x01, 0x02, 0x03}));
```

## `fromHexString`

You can initialize a `UInt` from a string representing a hexadecimal encoding of bytes, with an optional prefix of `0x`.

```java
UInt256 value = UInt256.fromHexString("0xdeadbeef");
```

# UInt operations

`Uints` are immutable, so all operations create a new object as the result of the operation.

## Arithmetic operations

### Adding and subtracting

* [`add(other)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/add.html)
* [`substract(other)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/subtract.html)

Since these are bound integers, they will overflow and underflow if the operation returns a value outside the boundaries - for a `Uint256`, 0 to 2^256.

For this reason, the API also contains `exact` methods that throw exceptions if the operations overflows or underflows.

* [`addExact(other)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/add-exact.html)
* [`subtractExact(other)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/subtract-exact.html)

### Multiplying and dividing

* [`multiply(other)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/multiply.html)
* [`divide(other)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/divide.html)

Additionally, the [method `divideCeil(other)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/divide-ceil.html) divides integers but returns the ceiling of the rounding.

```java
UInt256 result = UInt256.valueOf(12L).divide(UInt256.valueOf(5L)); // returns 2
UInt256 resultCeiling = UInt256.valueOf(12L).divideCeil(UInt256.valueOf(5L)); // returns 3
```

### Modulus

You can use the [method `mod(divisor)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/mod.html) to get the modulus of the value by the divisor.

The [method `mod0`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/mod0.html) is more forgiving - if you divide by zero, it will return zero instead of throwing an exception.

### Power

The [method `pow(exponent)`](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256-value/pow.html) returns a value that is `value^exponent mod 2^256.
# Boolean operations
You can use the following methods to perform boolean operations:
* [`not()`: bit-wise NOT of this value.](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/not.html)
* [`and(other)`: bit-wise AND of this value and the supplied value.](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/and.html)
* [`or(other)`: bit-wise OR of this value and the supplied value.](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/or.html)
* [`xor(other)`: bit-wise XOR of this value and the supplied value.](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/xor.html)
# Shifting bytes
You can shift [right](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/shift-right.html) and [left](https://tuweni.apache.org/docs/org.apache.tuweni.units.bigints/-u-int256/shift-left.html) the bits of the underlying bytes object by a given distance.
This is equivalent to the `<<<` or `>>>` operators in Java.

# Your own UInt class

You can create your own domain class based off the `Uints`. A good example is the [`Wei class`](https://tuweni.apache.org/docs/org.apache.tuweni.units.ethereum/-wei/index.html).

You will need to provide the super constructor the constructor of your class.

```java
public final class Wei extends BaseUInt256Value<Wei> { 
  private Wei(UInt256 bytes) {
    super(bytes, Wei::new);
  }
}
```