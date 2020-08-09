package com.github.rkbalgi.tcpasync;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A TcpMessage represents a TCP message (raw []byte) that is exchanged between a TCP client and a
 * server. The TcpMessage is usually prepended with a message length indicator (or MLI) to demarcate
 * a message from a subsequent one on a TCP stream.
 */
public class TcpMessage {

  public static final int INVALID = -1;
  public static final int OK = 0;
  public static final int TIMED_OUT = 1;

  private byte[] requestData, responseData;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Condition condition = lock.writeLock().newCondition();
  private int responseCode = INVALID;


  public TcpMessage(byte[] requestData) {
    this.requestData = requestData;
  }

  public TcpMessage(byte[] responseData, boolean b) {
    this.responseData = responseData;
  }

  public byte[] getRequestData() {
    return requestData;
  }

  public void setRequestData(byte[] requestData) {
    this.requestData = requestData;
  }

  public ReentrantReadWriteLock getLock() {
    return lock;
  }

  public Condition getCondition() {
    return condition;
  }

  public int getResponseCode() {

    try {
      lock.readLock().lock();
      return responseCode;
    } finally {
      lock.readLock().unlock();
    }
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public byte[] getResponseData() {

    try {
      lock.readLock().lock();
      return responseData;
    } finally {
      lock.readLock().unlock();
    }

  }

  public void setResponseData(byte[] responseData) {
    this.responseData = responseData;
  }

  public void waitForResponse() {
    try {
      lock.writeLock().lock();
      condition.await(1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void timedOut() {
    setResponseCode(TcpMessage.TIMED_OUT);
    try {
      getLock().writeLock().lock();
      getCondition().signalAll();
    } finally {
      getLock().writeLock().unlock();
    }
  }

  public void receivedResponse(byte[] responseData) {

    try {
      getLock().writeLock().lock();
      setResponseData(responseData);
      setResponseCode(TcpMessage.OK);
      getCondition().signalAll();
    } finally {
      getLock().writeLock().unlock();
    }
  }
}
