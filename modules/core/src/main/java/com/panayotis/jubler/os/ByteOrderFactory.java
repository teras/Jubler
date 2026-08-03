/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.os;

public class ByteOrderFactory {

    private final static ByteOrder[] orders;

    static {
        orders = new ByteOrder[3];
        orders[0] = new ByteOrder("EFBBBF", "UTF-8");
        orders[1] = new ByteOrder("FEFF", "UTF-16");
        orders[2] = new ByteOrder("FFFE", "UTF-16");
    }

    private static class ByteOrder {

        private byte[] tag;
        private String encoding;
        private static int MaxSize = 0;

        private ByteOrder(String stringtag, String enc) {
            int length = stringtag.length() / 2;
            tag = new byte[length];
            if (MaxSize < length)
                MaxSize = length;
            for (int i = 0; i < length; i++)
                tag[i] = (byte) Integer.decode("0x" + stringtag.substring(i * 2, i * 2 + 2)).intValue();
            encoding = enc;
        }

        private boolean match(byte[] test) {
            for (int i = 0; i < tag.length; i++)
                if (tag[i] != test[i])
                    return false;
            return true;
        }
    }

    /** Detect a Unicode BOM at the start of already-read bytes; returns null when none matches. */
    public static String getEncoding(byte[] buffer) {
        if (buffer == null || buffer.length < ByteOrder.MaxSize)
            return null;
        for (ByteOrder order : orders)
            if (order.match(buffer))
                return order.encoding;
        return null;
    }
}
