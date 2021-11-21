package org.stonlexx.protocol.lib.util;

import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;
import org.stonlexx.protocol.lib.packet.Serializable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@UtilityClass
public class PacketUtils {

    public void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);

        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public InetAddress readAddress(ByteBuf buf) throws UnknownHostException {
        byte[] bytes = new byte[4];
        buf.readBytes(bytes);

        return InetAddress.getByAddress(bytes);
    }

    public void writeAddress(ByteBuf buf, InetAddress address) {
        byte[] bytes = address.getAddress();
        buf.writeBytes(bytes);
    }

    public <T extends Enum<T>> T readEnum(ByteBuf buf, Class<T> cls) {
        int enumOrdinal = buf.readUnsignedByte();
        T[] constants = cls.getEnumConstants();

        return enumOrdinal >= constants.length || enumOrdinal < 0
                ? null
                : constants[enumOrdinal];
    }

    public <T extends Enum<T>> T readSafeEnum(ByteBuf buf, Class<T> cls) {
        try {
            return Enum.valueOf(cls, readString(buf, 255));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void writeSafeEnum(ByteBuf buf, Enum<?> e) {
        writeString(buf, e.name());
    }

    public void writeSerialization(ByteBuf buf, Serializable<?> serializable) {
        serializable.serialize(buf);
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable<?>> T readSerialization(ByteBuf buf, Supplier<T> reader) {
        return (T) reader.get().deserialize(buf);
    }

    public <T, C extends Collection<T>> C readCollection(ByteBuf buf, IntFunction<C> colFactory, Supplier<T> reader) {
        int size = readVarInt(buf);

        C col = colFactory.apply(size);

        for (int i = 0; i < size; i++) {
            col.add(reader.get());
        }

        return col;
    }

    private <T extends Serializable<T>, C extends Collection<T>> C readCollection0(ByteBuf buf,
                                                                                   IntFunction<C> colFactory,
                                                                                   Supplier<T> factory) {
        return readCollection(buf, colFactory, () -> factory.get().deserialize(buf));
    }

    public <T extends Serializable<T>, C extends Collection<T>> C readCollection(ByteBuf buf, C col, Supplier<T> factory) {
        return readCollection0(buf, size -> col, factory);
    }

    public <T extends Serializable<T>> Set<T> readSet(ByteBuf buf, Supplier<T> factory) {
        return readCollection0(buf, HashSet::new, factory);
    }

    public <T extends Serializable<T>> List<T> readList(ByteBuf buf, Supplier<T> factory) {
        return readCollection0(buf, ArrayList::new, factory);
    }

    public <T> void writeCollection(ByteBuf buf, Collection<T> collection, Consumer<T> writer) {
        writeVarInt(buf, collection.size());

        for (T element : collection) {
            writer.accept(element);
        }
    }

    public <T extends Serializable<T>> void writeCollection(ByteBuf buf, Collection<T> collection) {
        writeCollection(buf, collection, element -> element.serialize(buf));
    }

    public void writeStringCollection(ByteBuf buf, Collection<String> collection) {
        writeCollection(buf, collection, s -> writeString(buf, s));
    }

    public int[] readIntArray(ByteBuf buf) {
        int[] array = new int[buf.readUnsignedByte()];

        for (int i = 0; i < array.length; i++) {
            array[i] = buf.readInt();
        }

        return array;
    }

    public void writeIntArray(ByteBuf buf, int[] array) {
        buf.writeByte(array.length);

        for (int i : array) {
            buf.writeInt(i);
        }
    }

    public void writeEnum(ByteBuf buf, Enum<?> e) {
        buf.writeByte(e.ordinal());
    }

    @Deprecated
    public String readString(ByteBuf buf) {
        return readString(buf, Short.MAX_VALUE);
    }

    public String readString(ByteBuf buf, int max) {
        int size = readVarInt(buf);

        if (size < 0) {
            throw new IllegalStateException("Received wrong string size");
        }

        if (size > max) {
            throw new IllegalArgumentException(size + " > " + max);
        }

        if (size > buf.readableBytes()) {
            throw new IllegalArgumentException(size + " > " + buf.readableBytes());
        }

        byte[] bytes = new byte[size];
        buf.readBytes(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public int getVarIntSize(int input) {
        return (input & 0xFFFFFF80) == 0
                ? 1 : (input & 0xFFFFC000) == 0
                ? 2 : (input & 0xFFE00000) == 0
                ? 3 : (input & 0xF0000000) == 0
                ? 4 : 5;
    }

    public int readVarInt(ByteBuf buf) {
        int result = 0;
        int numRead = 0;

        byte read;

        do {
            read = buf.readByte();
            result |= (read & 127) << numRead++ * 7;

            if (numRead > 5) {
                throw new RuntimeException("VarInt is too big");
            }
        } while ((read & 128) == 128);

        return result;
    }

    public void writeVarInt(ByteBuf buf, int value) {
        while ((value & -128) != 0) {
            buf.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        buf.writeByte(value);
    }

}
