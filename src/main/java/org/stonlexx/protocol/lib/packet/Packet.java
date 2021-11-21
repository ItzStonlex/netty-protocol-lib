package org.stonlexx.protocol.lib.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Пакет
 * <br>
 * <p>
 * У пакета есть requestId, он нужен для того
 * <br>чтобы определить, чей это ответ/запрос
 * <br>и вернуть с тем-же ID обратно
 * <br>Так же можно сделать throw new SomePacket();
 * <br>если в это время обрабатывается пакет и у него
 * <br>есть Request ID, то SomePacket будет
 * <br>установлен Request ID этого пакета
 * </p>
 */
public abstract class Packet extends RuntimeException {
    @Setter
    @Getter
    private int requestId = -1;

    public abstract void process(@NonNull PacketProcessor processor) throws Exception;

    public boolean hasRequestId() {
        return requestId != -1;
    }

    public void readPacket(@NonNull ByteBuf buf) throws Exception {
        read(buf);

        if (buf.readableBytes() > 0) {
            requestId = buf.readInt();
        }
    }

    public abstract void read(@NonNull ByteBuf buf) throws Exception;

    public void writePacket(@NonNull ByteBuf buf) throws Exception {
        write(buf);

        if (hasRequestId()) {
            buf.writeInt(requestId);
        }
    }

    public abstract void write(@NonNull ByteBuf buf) throws Exception;
}