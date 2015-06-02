package com.github.rkbalgi.tcpasync;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LengthPrefixedTcpMessage {

    public static final String TIMED_OUT = "TIMED_OUT";
    public static final String OK = "OK";
    //private final MLI_TYPE mliType;
    private byte[] requestData, responseData;
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private String responseCode;


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

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public byte[] getResponseData() {
        return responseData;
    }

    public void setResponseData(byte[] responseData) {
        this.responseData = responseData;
    }

    void waitForResponse() {
        try {
            lock.lock();
            condition.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    void timedOut() {
        setResponseCode(LengthPrefixedTcpMessage.TIMED_OUT);
        try {
            getLock().lock();
            getCondition().signalAll();
        } finally {
            getLock().unlock();
        }
    }

    void receivedResponse(byte[] responseData) {
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
