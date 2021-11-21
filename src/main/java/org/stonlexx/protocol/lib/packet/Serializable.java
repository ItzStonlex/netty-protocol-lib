package org.stonlexx.protocol.lib.packet;

import io.netty.buffer.ByteBuf;

public interface Serializable<T extends Serializable<T>> {

    T serialize(ByteBuf out);

    T deserialize(ByteBuf in);

}
