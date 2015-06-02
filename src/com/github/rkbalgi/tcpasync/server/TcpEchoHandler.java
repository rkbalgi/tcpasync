package com.github.rkbalgi.tcpasync.server;

import com.github.rkbalgi.iso8583.Hex;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

/**
 * Created by 132968 on 08-05-2015.
 */
public class TcpEchoHandler implements TcpServerMessageHandler {

    private static final Logger log= Logger.getLogger(TcpEchoHandler.class);

    @Override
    public void handleMessage(Channel channel, byte[] data) {
        ChannelBuffer buf = ChannelBuffers.buffer(2 + data.length);
        buf.writeShort(data.length + 2);//2I
        buf.writeBytes(data);
        if(log.isDebugEnabled()){
            log.debug("Echo Response ="+ Hex.toString(buf.array()));
        }
        channel.write(buf);
    }
}
