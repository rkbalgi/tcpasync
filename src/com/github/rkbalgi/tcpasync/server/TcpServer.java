package com.github.rkbalgi.tcpasync.server;

import com.github.rkbalgi.tcpasync.MLI_TYPE;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by 132968 on 08-05-2015.
 */
public class TcpServer {

    private ServerBootstrap bootstrap = new ServerBootstrap();

    public TcpServer(MLI_TYPE mliType, String host, int port, TcpServerMessageHandler handler) {

        Executor exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        bootstrap.setFactory(new NioServerSocketChannelFactory(exec, exec));
        bootstrap.getPipeline().addLast("server-handler", new TcpServerChannelHandler(mliType, handler));
        bootstrap.bind(new InetSocketAddress(host, port));
    }

    public static void main(String[] args){
        new TcpServer(MLI_TYPE.MLI_2I,"localhost",7879,new TcpEchoHandler());
    }


}
