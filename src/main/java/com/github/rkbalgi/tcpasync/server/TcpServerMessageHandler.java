package com.github.rkbalgi.tcpasync.server;

import io.netty.channel.Channel;

/**
 * Created by Raghavendra Balgi on 08-05-2015.
 */
public interface TcpServerMessageHandler {
    void handleMessage(Channel channel, byte[] data);
}
