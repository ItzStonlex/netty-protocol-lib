package org.stonlexx.protocol.lib.packet;

import lombok.NonNull;

@SuppressWarnings({"unused", "RedundantThrows"})
public interface PacketProcessor {

    /**
     * Process packet
     */
    default void process(@NonNull Packet packet) throws Exception {
        packet.process(this);
    }

    /**
     * Process connect
     */
    default void active() throws Exception {
        // for implementation
    }

    /**
     * Process disconnect
     */
    default void inactive() throws Exception {
        // for implementation
    }

    /**
     * Process error
     */
    default void process(@NonNull Throwable throwable) throws Exception {
        throwable.printStackTrace();

        // for implementation
    }
}
