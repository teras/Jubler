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

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.LoaderThread;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author teras
 */
public class JublerServer implements JublerRMI {

    public static final int JUBLERPORT = 12258;

    public static void startServer() {
        try {
            JublerServer obj = new JublerServer();
            JublerRMI stub = (JublerRMI) UnicastRemoteObject.exportObject(obj, 0);
            try {
                LocateRegistry.createRegistry(JUBLERPORT);
            } catch (Exception e) {
            }
            LocateRegistry.getRegistry(JUBLERPORT).bind("Jubler", stub);
        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    public static void stopServer() {
        try {
            LocateRegistry.getRegistry(JUBLERPORT).unbind("Jubler");
        } catch (RemoteException ex) {
        } catch (NotBoundException ex) {
        }
    }

    public void addFile(String URL) throws RemoteException {
        LoaderThread.getLoader().addSubtitle(URL);
    }
}
