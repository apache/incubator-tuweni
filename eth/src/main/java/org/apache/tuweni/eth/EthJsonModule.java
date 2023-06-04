// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.ethereum.Gas;
import org.apache.tuweni.units.ethereum.Wei;

import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class EthJsonModule extends SimpleModule {

  static class HashSerializer extends StdSerializer<Hash> {

    HashSerializer() {
      super(Hash.class);
    }

    @Override
    public void serialize(Hash value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(value.toHexString());
    }
  }

  static class HashDeserializer extends StdDeserializer<Hash> {

    HashDeserializer() {
      super(Hash.class);
    }

    @Override
    public Hash deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return Hash.fromHexString(p.getValueAsString());
    }
  }

  static class AddressSerializer extends StdSerializer<Address> {

    AddressSerializer() {
      super(Address.class);
    }

    @Override
    public void serialize(Address value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(value.toHexString());
    }
  }

  static class AddressKeySerializer extends StdSerializer<Address> {

    protected AddressKeySerializer() {
      super(Address.class);
    }

    @Override
    public void serialize(Address value, JsonGenerator g, SerializerProvider provider)
        throws IOException {
      g.writeFieldName(value.toHexString());
    }
  }

  static class BytesSerializer extends StdSerializer<Bytes> {

    BytesSerializer() {
      super(Bytes.class);
    }

    @Override
    public void serialize(Bytes value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(value.toHexString());
    }
  }

  static class Bytes32Serializer extends StdSerializer<Bytes32> {

    Bytes32Serializer() {
      super(Bytes32.class);
    }

    @Override
    public void serialize(Bytes32 value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(value.toHexString());
    }
  }

  static class PublicKeySerializer extends StdSerializer<SECP256K1.PublicKey> {

    PublicKeySerializer() {
      super(SECP256K1.PublicKey.class);
    }

    @Override
    public void serialize(SECP256K1.PublicKey value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(value.toHexString());
    }
  }

  static class GasSerializer extends StdSerializer<Gas> {

    GasSerializer() {
      super(Gas.class);
    }

    @Override
    public void serialize(Gas value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(value.toBytes().toHexString());
    }
  }

  static class UInt256Serializer extends StdSerializer<UInt256> {

    UInt256Serializer() {
      super(UInt256.class);
    }

    @Override
    public void serialize(UInt256 value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeString(value.toHexString());
    }
  }

  static class InstantSerializer extends StdSerializer<Instant> {

    InstantSerializer() {
      super(Instant.class);
    }

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeNumber(value.toEpochMilli());
    }
  }

  static class AddressDeserializer extends StdDeserializer<Address> {

    AddressDeserializer() {
      super(Address.class);
    }

    @Override
    public Address deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (p.getValueAsString().length() == 0) {
        return null;
      }
      return Address.fromHexString(p.getValueAsString());
    }
  }

  static class GasDeserializer extends StdDeserializer<Gas> {

    GasDeserializer() {
      super(Gas.class);
    }

    @Override
    public Gas deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return Gas.valueOf(UInt256.fromHexString(p.getValueAsString()));
    }
  }

  static class WeiDeserializer extends StdDeserializer<Wei> {

    WeiDeserializer() {
      super(Wei.class);
    }

    @Override
    public Wei deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return Wei.valueOf(UInt256.fromHexString(p.getValueAsString()));
    }
  }

  static class BytesDeserializer extends StdDeserializer<Bytes> {

    BytesDeserializer() {
      super(Bytes.class);
    }

    @Override
    public Bytes deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      String value = p.getValueAsString();
      if (value.startsWith("0x:bigint ")) {
        value = value.substring(10);
      }
      return Bytes.fromHexStringLenient(value);
    }
  }

  static class Bytes32Deserializer extends StdDeserializer<Bytes32> {

    Bytes32Deserializer() {
      super(Bytes32.class);
    }

    @Override
    public Bytes32 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      String value = p.getValueAsString();
      if (value == null) {
        return null;
      }
      return Bytes32.fromHexString(value);
    }
  }

  static class UInt256Deserializer extends StdDeserializer<UInt256> {

    UInt256Deserializer() {
      super(UInt256.class);
    }

    @Override
    public UInt256 deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      return UInt256.fromHexString(p.getValueAsString());
    }
  }

  static class AddressKeyDeserializer extends KeyDeserializer {

    @Override
    public Address deserializeKey(String key, DeserializationContext ctxt) throws IOException {
      return Address.fromHexString(key);
    }
  }

  static class BytesKeyDeserializer extends KeyDeserializer {

    @Override
    public Bytes deserializeKey(String key, DeserializationContext ctxt) throws IOException {
      return Bytes.fromHexString(key);
    }
  }

  static class UInt256KeyDeserializer extends KeyDeserializer {

    @Override
    public UInt256 deserializeKey(String key, DeserializationContext ctxt) throws IOException {
      return UInt256.fromHexString(key);
    }
  }

  static class StringOrLongDeserializer extends StdDeserializer<StringOrLong> {

    public StringOrLongDeserializer() {
      super(StringOrLong.class);
    }

    @Override
    public StringOrLong deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException, JsonProcessingException {
      if (p.currentToken().isNumeric()) {
        return new StringOrLong(p.getLongValue());
      } else {
        return new StringOrLong(p.getValueAsString());
      }
    }
  }

  static class StringOrLongSerializer extends StdSerializer<StringOrLong> {

    public StringOrLongSerializer() {
      super(StringOrLong.class);
    }

    @Override
    public void serialize(StringOrLong value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      if (value.getValueAsString() == null) {
        gen.writeNumber(value.getValueAsLong());
      } else {
        gen.writeString(value.getValueAsString());
      }
    }
  }

  public EthJsonModule() {
    addSerializer(Hash.class, new HashSerializer());
    addDeserializer(Hash.class, new HashDeserializer());
    addSerializer(Address.class, new AddressSerializer());
    addKeySerializer(Address.class, new AddressKeySerializer());
    addSerializer(Bytes.class, new BytesSerializer());
    addSerializer(Bytes32.class, new Bytes32Serializer());
    addSerializer(Gas.class, new GasSerializer());
    addSerializer(UInt256.class, new UInt256Serializer());
    addSerializer(Instant.class, new InstantSerializer());
    addKeyDeserializer(Bytes.class, new BytesKeyDeserializer());
    addKeyDeserializer(Address.class, new AddressKeyDeserializer());
    addDeserializer(Address.class, new AddressDeserializer());
    addDeserializer(Gas.class, new GasDeserializer());
    addDeserializer(Wei.class, new WeiDeserializer());
    addDeserializer(UInt256.class, new UInt256Deserializer());
    addKeyDeserializer(UInt256.class, new UInt256KeyDeserializer());
    addDeserializer(Bytes.class, new BytesDeserializer());
    addDeserializer(Bytes32.class, new Bytes32Deserializer());
    addSerializer(SECP256K1.PublicKey.class, new PublicKeySerializer());
    addSerializer(StringOrLong.class, new StringOrLongSerializer());
    addDeserializer(StringOrLong.class, new StringOrLongDeserializer());
  }
}
