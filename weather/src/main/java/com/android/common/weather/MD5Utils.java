package com.android.common.weather;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {
    private static final String HEX = "0123456789abcdef";

    public MD5Utils() {
    }

    public String getMD5(String data) {
        try {
            MessageDigest digester = MessageDigest.getInstance("MD5");
            digester.update(data.getBytes(), 0, data.getBytes().length);
            byte[] digest = digester.digest();
            return this.toHex(digest);
        } catch (NoSuchAlgorithmException var4) {
            var4.printStackTrace();
            return "";
        }
    }

    public String toHex(byte[] buf) {
        if (buf == null) {
            return "";
        } else {
            StringBuffer result = new StringBuffer(2 * buf.length);

            for (byte b : buf) {
                this.appendHex(result, b);
            }

            return result.toString();
        }
    }

    private void appendHex(StringBuffer sb, byte b) {
        sb.append("0123456789abcdef".charAt(b >> 4 & 15)).append("0123456789abcdef".charAt(b & 15));
    }
}
