package com.github.rkbalgi.tcpasync.server;

import org.jboss.netty.channel.Channel;

/**
 * Created by 132968 on 08-05-2015.
 */
public interface TcpServerMessageHandler {
    void handleMessage(Channel channel, byte[] data);
}
