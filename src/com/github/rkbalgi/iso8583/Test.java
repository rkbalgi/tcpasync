package com.github.rkbalgi.iso8583;

import com.github.rkbalgi.iso8583.utils.IsoUtils;
import org.apache.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class Test {

    private static final Logger log = Logger.getLogger(Test.class);
    private static final byte[] BASE_GCAG_MSG = Hex.fromString
            ("F1F1F0F0703425C108C18000F1F5F3F7F6F0F8F4F9F5F9F2F2F4F0F1F5F0F0F4F0F0F0F0F0F0F0F0F0F0F0F6F3F0F1F3F2F8F5F6F4F1F0F0F3F2F2F1F5F2F8F2F1F1F7F1F2F8F4F0F0F0F0F0F0F1F6F0F0F0F0F0F1F0F0F1F9F0F0F5F3F1F1F0F1F0F0F0F0F0F0F0F0F0F9F8F4F4F1F1F1F0F0F1F1F1F9F1F0F0F0F9F3F6F0F84040404040F0F0F4F0F3F0F1F8F4F0");


    public static void main(String[] args) {

        Iso8583.init();
        IsoUtils.nextStan();

        for (int i = 0; i < 10; i++) {
            long t1 = System.nanoTime();
            Iso8583Message msg = newGcagIsoMsg();
            long t2 = System.nanoTime();
            if (log.isDebugEnabled()) {
                log.debug(msg.info());
            }
            System.out.println(TimeUnit.MILLISECONDS.convert((t2 - t1), TimeUnit.NANOSECONDS) + " millis.");
        }


    }

    private static Iso8583Message newGcagIsoMsg() {
        final Iso8583 spec = Iso8583.getSpec("GCAG_ISO");
        Iso8583Message msg = new Iso8583Message(spec, BASE_GCAG_MSG);
        byte[] stan = IsoUtils.nextStan().ToEbcdic();
        msg.set(11, stan);
        return (msg);


    }
}

