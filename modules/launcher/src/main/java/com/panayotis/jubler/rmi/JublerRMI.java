/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface JublerRMI extends Remote {

    void addFile(String URL) throws RemoteException;
}
