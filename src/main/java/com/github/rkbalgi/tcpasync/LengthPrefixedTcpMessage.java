package com.github.rkbalgi.tcpasync;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LengthPrefixedTcpMessage {

  public static final int INVALID = -1;
  public static final int OK = 0;
  public static final int TIMED_OUT = 1;

  private byte[] requestData, responseData;
  private ReentrantLock lock = new ReentrantLock();
  private Condition condition = lock.newCondition();
  private int responseCode = INVALID;


  public LengthPrefixedTcpMessage(byte[] requestData) {
    this.requestData = requestData;
  }

  public LengthPrefixedTcpMessage(byte[] responseData, boolean b) {
    this.responseData = responseData;
  }

  public byte[] getRequestData() {
    return requestData;
  }

  public void setRequestData(byte[] requestData) {
    this.requestData = requestData;
  }

  public ReentrantLock getLock() {
    return lock;
  }

  public Condition getCondition() {
    return condition;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public byte[] getResponseData() {
    return responseData;
  }

  public void setResponseData(byte[] responseData) {
    this.responseData = responseData;
  }

  public void waitForResponse() {
    try {
      lock.lock();
      condition.await(1000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      lock.unlock();
    }
  }

  public void timedOut() {
    setResponseCode(LengthPrefixedTcpMessage.TIMED_OUT);
    try {
      getLock().lock();
      getCondition().signalAll();
    } finally {
      getLock().unlock();
    }
  }

  public void receivedResponse(byte[] responseData) {
    this.responseData = responseData;
    setResponseCode(LengthPrefixedTcpMessage.OK);
    try {
      getLock().lock();
      getCondition().signalAll();
    } finally {
      getLock().unlock();
    }
  }
}
