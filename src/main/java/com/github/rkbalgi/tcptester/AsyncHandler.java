package com.github.rkbalgi.tcptester;

import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;

public interface AsyncHandler extends BiConsumer<TcpMessage, ByteBuf> {

  public void accept(TcpMessage tcpReqMessage, ByteBuf responseBuf);

}
