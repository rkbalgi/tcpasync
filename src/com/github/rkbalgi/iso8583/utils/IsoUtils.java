package com.github.rkbalgi.iso8583.utils;

import java.util.Random;

/**
 * Created by 132968 on 29-04-2015.
 */
public class IsoUtils {

    private static final Random random = new Random(5);

    public static Stan nextStan() {
        return new Stan(Integer.toString(random.nextInt(999999)));
    }
}
