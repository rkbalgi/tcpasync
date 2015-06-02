package com.github.rkbalgi.iso8583.utils;

import java.io.UnsupportedEncodingException;

/**
 * Created by 132968 on 29-04-2015.
 */
public class Stan {
    private final String val;

    public Stan(String val) {
        this.val = val;
    }

    public byte[] ToEbcdic() {
        try {
            return val.getBytes("cp037");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
