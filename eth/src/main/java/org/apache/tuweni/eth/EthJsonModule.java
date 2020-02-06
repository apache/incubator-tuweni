package org.apache.tuweni.eth;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.ethereum.Gas;

import java.io.IOException;
import java.time.Instant;

public class EthJsonModule extends SimpleModule {

    static class HashSerializer extends StdSerializer<Hash> {

        HashSerializer() {
            super(Hash.class);
        }

        @Override
        public void serialize(Hash value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toHexString());
        }
    }

    static class AddressSerializer extends StdSerializer<Address> {

        AddressSerializer() {
            super(Address.class);
        }

        @Override
        public void serialize(Address value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toHexString());
        }
    }

    static class BytesSerializer extends StdSerializer<Bytes> {

        BytesSerializer() {
            super(Bytes.class);
        }

        @Override
        public void serialize(Bytes value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toHexString());
        }
    }

    static class GasSerializer extends StdSerializer<Gas> {

        GasSerializer() {
            super(Gas.class);
        }

        @Override
        public void serialize(Gas value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toBytes().toHexString());
        }
    }

    static class UInt256Serializer extends StdSerializer<UInt256> {

        UInt256Serializer() {
            super(UInt256.class);
        }

        @Override
        public void serialize(UInt256 value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toHexString());
        }
    }

    static class InstantSerializer extends StdSerializer<Instant> {

        InstantSerializer() {
            super(Instant.class);
        }

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeNumber(value.toEpochMilli());
        }
    }

    public EthJsonModule() {
        addSerializer(Hash.class, new HashSerializer());
        addSerializer(Address.class, new AddressSerializer());
        addSerializer(Bytes.class, new BytesSerializer());
        addSerializer(Gas.class, new GasSerializer());
        addSerializer(UInt256.class, new UInt256Serializer());
        addSerializer(Instant.class, new InstantSerializer());
    }
}
