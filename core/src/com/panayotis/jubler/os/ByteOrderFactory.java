/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.os;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * @author teras
 */
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

    public static String getEncoding(File f) {
        if (f.length() < ByteOrder.MaxSize)
            return null;
        byte[] buffer = new byte[ByteOrder.MaxSize];
        FileInputStream in = null;
        try {
            in = new FileInputStream(f);
            in.read(buffer);
            for (int i = 0; i < orders.length; i++) {
                if (orders[i].match(buffer)) {
                    in.close();
                    return orders[i].encoding;
                }
            }
        } catch (IOException ex) {
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }
}
