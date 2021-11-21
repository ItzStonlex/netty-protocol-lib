package org.stonlexx.protocol.lib.response;

import org.stonlexx.protocol.lib.packet.Packet;

public interface ResponseHandler<Response extends Packet> extends FullResponseHandler<Response> {

    @Override
    default void handleResponse(Response packet, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
            return;
        }

        handleResponse(packet);
    }

    void handleResponse(Response packet);
}
