<div align="center">

# Netty Protocol Library
[![MIT License](https://img.shields.io/github/license/pl3xgaming/Purpur?&logo=github)](License)
</div>

------------------------------------------
### Обратная связь
* **[Discord Server](https://discord.gg/GmT9pUy8af)**
* **[ВКонтакте](https://vk.com/itzstonlex)**
------------------------------------------

## Connection

`Client:`
```java
public final class Client extends AbstractClientChannel {
    
    public Client() {
        super("127.0.0.1", 1010, 2);
    
        // Registration of packets.
        PacketProtocol.HANDSHAKE.TO_SERVER.registerPacket(0x00, SPacket.class);
        PacketProtocol.HANDSHAKE.TO_CLIENT.registerPacket(0x00, SPacket.class);
    }
    
    @Override
    protected AbstractRemoteServerChannel newServerChannel(SocketChannel channel) {
        return new ServerChannelHandler(this, channel);
    }
    
    // That class required for channel & packets handle.
    private static class ServerChannelHandler extends AbstractRemoteServerChannel {
    
        public ServerChannelHandler(AbstractClientChannel client, SocketChannel channel) {
            super(client, channel);
        }
        
    }
}
```

`Server:`
```java
public final class Server extends AbstractServerChannel {

    public Server() {
        super("127.0.0.1", 1010, 2);
        
        // Registration of packets.
        PacketProtocol.HANDSHAKE.TO_SERVER.registerPacket(0x00, SPacket.class);
        PacketProtocol.HANDSHAKE.TO_CLIENT.registerPacket(0x00, SPacket.class);
    }

    @Override
    protected AbstractRemoteClientChannel newClientChannel(SocketChannel channel) {
        return new ClientChannelHandler(channel);
    }

    // That class required for channel & packets handle.
    private static class ClientChannelHandler extends AbstractRemoteClientChannel {

        public ClientChannelHandler(SocketChannel channel) {
            super(channel);
        }
    }

}
```
---
## Bind & Connect channels.
`Client:`

```java

// Enable metrics (No required).
PerformanceMetrics.startMetrics();

// Create client channel connection.
Client client = new Client();

client.connectAsynchronous((exception) -> client.reconnect(), () -> {
    System.out.println("SUCCESS: Channel was success connected to " + client.socketAddress);
});
```

`Server:`

```java

// Enable metrics (No required).
PerformanceMetrics.startMetrics();

// Create server channel connection.
Server server = new Server();

server.bindAsynchronous(() -> {
    System.out.println("SUCCESS: Channel was success bind on " + server.getChannel().localAddress())
});
```
---
## Create Packet

```java
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SPacket extends Packet {

    private Player player;

    @Override
    public void process(@NonNull PacketProcessor processor) {
        ((AbstractRemoteChannel) processor).upgradeConnection(PacketProtocol.PLAY);

        // Test debugging (It's worked).
        if (player != null) {
            System.out.println("Name: " + player.getName());
            System.out.println("Status: " + player.getStatus());

        } else {

            System.out.println("ERROR: Player is'nt deserialized!");
        }
    }


    @Override
    public void read(@NonNull ByteBuf buf) {
        String name = PacketUtils.readString(buf, 16);
        int status = buf.readInt();

        player = new Player(name, status);
    }

    @Override
    public void write(@NonNull ByteBuf buf) {
        PacketUtils.writeString(buf, player.getName());
        buf.writeInt(player.getStatus());
    }

}
```

For send packets example:

* Send simple packets: `AbstractChannel#sendPacket()`
* Send await packets: `AbstractChannel#awaitPacket()`

---
## Objects Serialization

```java
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
```

For use example:
```java
@Override
public void read(@NonNull ByteBuf buf) {
    player = PacketUtils.readSerialization(buf, Player::new);
}

@Override
public void write(@NonNull ByteBuf buf) {
    PacketUtils.writeSerialization(buf, player);
}
```
