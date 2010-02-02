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
package com.panayotis.jubler.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author teras
 */
public class JublerClient {

    private static JublerRMI stub;

    private JublerClient() {
    }

    public static boolean isRunning() {
        try {
            Registry registry = LocateRegistry.getRegistry(JublerServer.JUBLERPORT);
            stub = (JublerRMI) registry.lookup("Jubler");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void setFileList(String file) {
        try {
            stub.addFile(file);
        } catch (RemoteException ex) {
        }
    }
}

