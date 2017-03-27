/*
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
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
