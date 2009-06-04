/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 *
 * @author teras
 */
public abstract class CommandLineViewport extends ExternalViewport {

    protected Process proc;
    protected BufferedWriter cmdpipe;
    protected BufferedReader infopipe;
    protected BufferedReader errorpipe;
    protected Thread out_t,  error_t;

    protected abstract String[] getPostInitCommand();

    public CommandLineViewport(AbstractPlayer player) {
        super(player);
    }

    protected void sendData(String data) throws IOException {
        if (cmdpipe==null)
            throw new IOException("Can not receive commands, pipe non existent.");
        cmdpipe.write(data + "\n");
        cmdpipe.flush();
    }

    protected void terminate() {
        try {
            out_t.join(500);
        } catch (InterruptedException ex) {
        }
        
        if (proc != null)
            proc.destroy();
        proc = null;
        try {
            cmdpipe.close();
        } catch (IOException ce) {
        }
        cmdpipe = null;
    }

    public void start() throws ExtProgramException {
        player.cleanUp();   // Make sure player is in it's initial position (i.e. no subtitle files hanging around)

        String cmd[] = player.getCommandArguments(mfile, sub, when);
        position = 0;
        isPaused = false;

        try {
            proc = Runtime.getRuntime().exec(cmd);
            cmdpipe = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
            infopipe = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            errorpipe = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            if (infopipe == null || cmdpipe == null || proc == null)
                throw new ExtProgramException(new NullPointerException());

            out_t = new OutputParser();
            error_t = new ErrorParser();
            out_t.start();
            error_t.start();

            sendCommands(getPostInitCommand());     // Get information for current volume position
            return;
        } catch (Exception e) {
            player.cleanUp();
            throw new ExtProgramException(e);
        }
    }

    private class OutputParser extends Thread {

        public void run() {
            String info;
            try {
                int first, second;
                while ((info = infopipe.readLine()) != null) {
                    first = info.indexOf("V:");
                    if (first >= 0) {
                        first++;
                        while (info.charAt(++first) == ' ');
                        second = first;
                        while (info.charAt(++second) != ' ');
                        position = getDouble(info.substring(first, second).trim());
                    } else {
                        DEBUG.debug("[mplayer:out] " + info);
                        if (info.startsWith("ANS_volume"))
                            feedback.volumeUpdate(Float.parseFloat(info.substring(info.indexOf('=') + 1)) / 100f);
                    }
                }
            } catch (IOException e) {
            }
            if (quit_is_fatal)
                feedback.requestQuit();
            else
                quit_is_fatal = true;  // revert flag to it's original value
        }
    }

    private class ErrorParser extends Thread {

        public void run() {
            String info;
            try {
                while ((info = errorpipe.readLine()) != null)
                    DEBUG.debug("[Player] " + info);
            } catch (IOException e) {
            }
        }
    }
}
