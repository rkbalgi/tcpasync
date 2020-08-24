package com.github.rkbalgi.tcptester.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

/**
 * Created by Raghavendra Balgi on 08-05-2015.
 */
public interface TcpServerMessageHandler {
    void handleMessage(Channel channel, ByteBuf data);
}
