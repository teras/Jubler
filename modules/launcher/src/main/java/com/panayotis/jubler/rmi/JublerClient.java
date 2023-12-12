/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.rmi;

import com.panayotis.jubler.os.DEBUG;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;

public class JublerClient {

    private static JublerRMI stub;

    private JublerClient() {
    }

    public static boolean isRunning() {
        try {
            RMISocketFactory.setSocketFactory(new RMISocketFactory() {
                public Socket createSocket(String host, int port)
                        throws IOException {
                    Socket socket = new Socket();
                    socket.setSoTimeout(2000);
                    socket.setSoLinger(false, 0);
                    socket.connect(new InetSocketAddress(host, port), 2000);
                    return socket;
                }

                public ServerSocket createServerSocket(int port)
                        throws IOException {
                    return new ServerSocket(port);
                }
            });
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
            DEBUG.debug(ex);
        }
    }
}
