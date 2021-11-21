package org.stonlexx.protocol.lib.packet;

public enum PacketDirection {

    TO_SERVER {
        @Override
        public PacketMapper getMapper(PacketProtocol protocol) {
            return protocol.TO_SERVER;
        }
    },
    TO_CLIENT {
        @Override
        public PacketMapper getMapper(PacketProtocol protocol) {
            return protocol.TO_CLIENT;
        }
    };

    public abstract PacketMapper getMapper(PacketProtocol protocol);
}
