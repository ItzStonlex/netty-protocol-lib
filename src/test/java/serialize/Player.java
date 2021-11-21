package serialize;

import org.stonlexx.protocol.lib.util.PacketUtils;
import org.stonlexx.protocol.lib.packet.Serializable;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player implements Serializable<Player> {

    private String name;
    private int status;

    @Override
    public Player serialize(ByteBuf out) {
        PacketUtils.writeString(out, name);
        out.writeInt(status);

        return this;
    }

    @Override
    public Player deserialize(ByteBuf in) {
        name = PacketUtils.readString(in, 16);
        status = in.readInt();

        return this;
    }
}
