package com.github.rkbalgi.tcpasync;

/**
 * Created by 132968 on 27-04-2015.
 */
public interface KeyExtractor {

    /**
     * The end users should implement these methods, to extract the key for this particular
     * message. In ISO8583 for example, the STAN has to be extracted and returned.
     */

    public String getRequestKey(LengthPrefixedTcpMessage requestMsg);
    public String getResponseKey(LengthPrefixedTcpMessage responseMsg);
}
