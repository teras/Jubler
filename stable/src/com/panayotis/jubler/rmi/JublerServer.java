/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.rmi;

import com.panayotis.jubler.Main;
import com.panayotis.jubler.os.DEBUG;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        Main.asyncAddSubtitle(URL);
    }
}
