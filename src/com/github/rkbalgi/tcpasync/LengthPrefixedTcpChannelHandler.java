package com.github.rkbalgi.tcpasync;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class LengthPrefixedTcpChannelHandler extends SimpleChannelHandler {

    private final MLI_TYPE mliType;

    public LengthPrefixedTcpChannelHandler(MLI_TYPE mliType) {
        this.mliType = mliType;
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
        buf.markReaderIndex();
        if (buf.readableBytes() > 2) {

            int n = buf.readShort();
            if (mliType == MLI_TYPE.MLI_2I) {
                n -= 2;
            }
            if (buf.readableBytes() >= n) {
                ChannelBuffer outBuf = ChannelBuffers.buffer(n);
                buf.readBytes(outBuf);
                TcpAsync.receivedMsg(outBuf);
                return;
            }
        }
        buf.resetReaderIndex();

    }

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
    }
}
