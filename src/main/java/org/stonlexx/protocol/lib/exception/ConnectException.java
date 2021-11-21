package org.stonlexx.protocol.lib.exception;

import org.stonlexx.protocol.lib.channel.AbstractChannel;

public class ConnectException extends Exception {

    public ConnectException(AbstractChannel channel, Throwable cause) {
        super("Unable to connect to server [" + channel.getAddress() + ":" + channel.getPort() + "]", cause);
    }

}
