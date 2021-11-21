package org.stonlexx.protocol.lib.packet;

import org.stonlexx.protocol.lib.util.MetafactoryUtil;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class PacketMapper {

    private final TIntObjectMap<Supplier<? extends Packet>> idFactoryMap = new TIntObjectHashMap<>();
    private final TObjectIntMap<Class<? extends Packet>> classIdMap = new TObjectIntHashMap<>(10, 0.5F, -1);

    public <T extends Packet> void registerPacket(int id, Class<T> cls, Supplier<T> factory) {
        idFactoryMap.put(id, factory);
        classIdMap.put(cls, id);
    }

    public <T extends Packet> void registerPacket(int id, Class<T> cls) {
        try {
            registerPacket(id, cls, MetafactoryUtil.objectConstructor(cls));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T extends Packet> T newPacket(int id) {
        Supplier<T> supplier = (Supplier<T>) idFactoryMap.get(id);

        if (supplier == null) {
            return null;
        }

        return supplier.get();
    }

    public int getPacketId(Class<? extends Packet> cls) {
        return classIdMap.get(cls);
    }
}
