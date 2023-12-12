/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player.terminals;

import com.panayotis.jubler.media.player.PlayerArguments;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

public class ServerTerminal extends AbstractTerminal {

    private Process proc;
    private Socket socket;
    private final static int TIMEOUT = 15;
    private PlayerArguments args;

    public void start(PlayerArguments args) throws ExtProgramException {
        proc = null;
        cmd = null;
        out = error = null;
        int counter = 0;
        try {
            proc = Runtime.getRuntime().exec(args.arguments);
            while (socket == null && counter <= TIMEOUT)
                try {
                    Thread.sleep(1000);
                    socket = new Socket((String) null, args.port);
                } catch (Exception ex) {
                    try {
                        socket.close();
                    } catch (Exception exin) {
                    }
                    socket = null;
                    counter++;
                }

            if (socket == null)
                throw new ExtProgramException(new TimeoutException("Timeout while trying to contact server."));

            cmd = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception ex) {
            throw new ExtProgramException(ex);
        }
        this.args = args;
    }

    public PlayerArguments getArguments() {
        return args;
    }

    public void terminate() {
        if (proc != null)
            proc.destroy();
        try {
            socket.close();
        } catch (IOException ex) {
        }
        socket = null;
    }
}
