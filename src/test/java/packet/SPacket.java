package packet;

import org.stonlexx.protocol.lib.channel.AbstractRemoteChannel;
import org.stonlexx.protocol.lib.packet.Packet;
import org.stonlexx.protocol.lib.packet.PacketProcessor;
import org.stonlexx.protocol.lib.packet.PacketProtocol;
import org.stonlexx.protocol.lib.util.PacketUtils;
import io.netty.buffer.ByteBuf;
import lombok.*;
import serialize.Player;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SPacket extends Packet {

    private Player player;

    @Override
    public void process(@NonNull PacketProcessor processor) {
        ((AbstractRemoteChannel) processor).upgradeConnection(PacketProtocol.PLAY);

        if (player != null) {
            System.out.println("Name: " + player.getName());
            System.out.println("Status: " + player.getStatus());

        } else {

            System.out.println("ERROR: Player is`nt deserialized!");
        }
    }


    @Override
    public void read(@NonNull ByteBuf buf) {
        player = PacketUtils.readSerialization(buf, Player::new);
    }

    @Override
    public void write(@NonNull ByteBuf buf) {
        PacketUtils.writeSerialization(buf, player);
    }

}
