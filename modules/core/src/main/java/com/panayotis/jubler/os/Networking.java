/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.os;

import java.io.IOException;
import java.net.ServerSocket;

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
