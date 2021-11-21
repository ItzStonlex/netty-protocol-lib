package org.stonlexx.protocol.lib.exception;

import org.stonlexx.protocol.lib.channel.AbstractChannel;

public class BindException extends Exception {

    public BindException(AbstractChannel channel, Throwable cause) {
        super("Unable to bind server [" + channel.getAddress() + ":" + channel.getPort() + "]", cause);
    }

}
