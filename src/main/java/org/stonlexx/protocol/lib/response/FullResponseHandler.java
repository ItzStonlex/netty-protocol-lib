package org.stonlexx.protocol.lib.response;

import org.stonlexx.protocol.lib.packet.Packet;

public interface FullResponseHandler<ResponsePacket extends Packet> {

    void handleResponse(ResponsePacket packet, Throwable throwable);

}
