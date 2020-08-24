package com.github.rkbalgi.tcptester;

import com.github.rkbalgi.tcptester.TcpMessage;

public interface MessageBuilder {

  /**
   * Builds and returns a new TcpMessage can can then be used to dispatch using the TcpClient
   *
   * @return TcpMessage {@link TcpMessage}
   */
  TcpMessage newMessage();

}
