package com.github.rkbalgi.tcpasync.server;

import com.github.rkbalgi.iso8583.Hex;
import com.github.rkbalgi.tcpasync.MLI_TYPE;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;

/**
 * Created by 132968 on 08-05-2015.
 */
public class TcpServerChannelHandler extends SimpleChannelHandler {
    private static final Logger log = Logger.getLogger(TcpServerChannelHandler.class);
    private final MLI_TYPE mliType;
    private final TcpServerMessageHandler handler;


    public TcpServerChannelHandler(MLI_TYPE mliType, TcpServerMessageHandler handler) {
        this.mliType = mliType;
        this.handler = handler;

    }


    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent cse) {

        if (log.isDebugEnabled()) {
            log.debug(String.format("channel connected from [%s] ", cse.getChannel().getRemoteAddress()));
        }


    }

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {

        log.error("error on channel", e.getCause());
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();

        log.debug("reader?:msgreceived???");

        buf.markReaderIndex();
        if (buf.readableBytes() > 2) {
            int n = buf.readShort();
            if (mliType == MLI_TYPE.MLI_2I) {
                n = n - 2;
            }
            if (buf.readableBytes() >= n) {
                byte[] data = new byte[n];
                buf.readBytes(data);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("message received from [%s] = %s", e.getChannel().getRemoteAddress(), Hex
                            .toString(data)));
                }
                handler.handleMessage(e.getChannel(), data);
            } else {
                buf.resetReaderIndex();
            }
        } else {
            buf.resetReaderIndex();
        }

    }

}
