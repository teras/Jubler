/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.terminals;

import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 *
 * @author teras
 */
public class ServerTerminal extends AbstractTerminal {

    private Process proc;
    private Socket socket;

    public void start(String[] command) throws ExtProgramException {
        proc = null;
        cmd = null;
        out = error = null;
        int counter = 0;
        try {
            proc = Runtime.getRuntime().exec(command);
            while (socket == null) {
                try {
                    Thread.sleep(1000);
                    socket = new Socket((String) null, 55600);
                } catch (Exception ex) {
                    try {
                        socket.close();
                    } catch (Exception exin) {
                    }
                    socket = null;
                    counter++;
                }
            }
            cmd = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception ex) {
            System.out.println("ti skata pali "+ex.getMessage());
            throw new ExtProgramException(ex);
        }
        System.out.println("all ok?");
    }

    public void terminate() {
        if (proc != null)
            proc.destroy();
    }
}
