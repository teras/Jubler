/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.rmi;

import com.panayotis.jubler.os.DEBUG;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class JublerServer implements JublerRMI {

    public static final int JUBLERPORT = 12258;
    private static ClassLoader cl;

    private Object loaderThread;
    private Method addSubtitle;

    public static void startServer(ClassLoader cl) {
        JublerServer.cl = cl;
        try {
            JublerServer obj = new JublerServer();
            JublerRMI stub = (JublerRMI) UnicastRemoteObject.exportObject(obj, 0);
            try {
                LocateRegistry.createRegistry(JUBLERPORT);
            } catch (Exception ignored) {
            }
            LocateRegistry.getRegistry(JUBLERPORT).bind("Jubler", stub);
        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    public static void stopServer() {
        try {
            LocateRegistry.getRegistry(JUBLERPORT).unbind("Jubler");
        } catch (Exception ignored) {
        }
    }

    public void addFile(String URL) throws RemoteException {
        try {
            if (addSubtitle == null) {
                Class<?> loaderThreadC = cl.loadClass("com.panayotis.jubler.os.LoaderThread");
                loaderThread = loaderThreadC.getMethod("getLoader").invoke(null);
                addSubtitle = loaderThreadC.getMethod("addSubtitle", String.class);
            }
            addSubtitle.invoke(loaderThread, URL);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
