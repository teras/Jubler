/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

