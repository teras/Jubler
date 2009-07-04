/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.jubler.os;

import java.io.IOException;
import java.net.ServerSocket;

/**
 *
 * @author teras
 */
public class Networking {

        public static int getRandomPort() {
        ServerSocket soc;
        for (int i = 50000; i < 51000; i++) {
            soc = null;
            try {
                soc = new ServerSocket(i);
            } catch (Exception ex) {
            }
            if (soc != null) {
                try {
                    soc.close();
                } catch (IOException ex) {
                }
                return i;
            }
        }
        return -1;
    }
}
