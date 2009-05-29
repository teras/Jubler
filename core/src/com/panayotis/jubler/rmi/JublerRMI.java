/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author teras
 */
public interface JublerRMI extends Remote {

    void addFile(String URL) throws RemoteException;
}
