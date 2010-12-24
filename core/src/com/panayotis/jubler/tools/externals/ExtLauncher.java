/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.media.player.terminals.Validator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author teras
 */
public class ExtLauncher {

    private Process proc;
    private Validator<String> outclosure = null;
    private Validator<String> errclosure = null;
    private boolean errorIsMainStream = false;
    private boolean readyToQuit;
    protected BufferedWriter cmdpipe;
    protected BufferedReader outpipe;
    protected BufferedReader errorpipe;
    protected Thread out_t, error_t;

    public void setErrclosure(Validator<String> errclosure) {
        this.errclosure = errclosure;
    }

    public void setOutclosure(Validator<String> outclosure) {
        this.outclosure = outclosure;
    }

    public void errorIsMainStream() {
        this.errorIsMainStream = true;
    }

    public void start(String[] args, String[] env) throws ExtProgramException {
        out_t = new OutputParser();
        error_t = new ErrorParser();
        readyToQuit = false;

        try {
            proc = Runtime.getRuntime().exec(args, env);
            cmdpipe = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
            outpipe = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            errorpipe = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        } catch (IOException ex) {
            throw new ExtProgramException(ex);
        }
        if (outpipe == null || cmdpipe == null)
            throw new ExtProgramException(new NullPointerException());

        out_t.start();
        error_t.start();
    }

    public void sendCommand(String data) {
        try {
            cmdpipe.write(data);
            cmdpipe.flush();
        } catch (IOException ex) {
        }
    }

    private void joinParts() {
        try {
            out_t.join(500);
        } catch (InterruptedException ex) {
        }
        try {
            cmdpipe.close();
        } catch (IOException ce) {
        }
        cmdpipe = null;
    }

    public void terminate() {
        joinParts();
        proc.destroy();
    }

    @SuppressWarnings("SleepWhileHoldingLock")
    public void waitForSignal() {
        while (!readyToQuit)
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
    }

    public int waitForTermination() {
        try {
            proc.waitFor();
        } catch (InterruptedException ex) {
        }
        joinParts();
        return proc.exitValue();
    }

    private class OutputParser extends Thread {

        @Override
        public void run() {
            if (outpipe == null)
                return;
            String info;
            try {
                while ((info = outpipe.readLine()) != null)
                    if (outclosure != null && outclosure.exec(info) && (!errorIsMainStream))
                        readyToQuit = true;
            } catch (Exception e) {
                if (errclosure != null)
                    errclosure.exec("E: " + e.toString());
            }
            if (!errorIsMainStream)
                readyToQuit = true;
            if (outclosure != null)
                outclosure.exec("QUIT");
        }
    }

    private class ErrorParser extends Thread {

        @Override
        public void run() {
            if (errorpipe == null)
                return;
            String info;
            try {
                while ((info = errorpipe.readLine()) != null)
                    if (errclosure != null && errclosure.exec(info) && errorIsMainStream)
                        readyToQuit = true;
            } catch (Exception e) {
                if (errclosure != null)
                    errclosure.exec("E: " + e.toString());
            }
            if (errorIsMainStream)
                readyToQuit |= true;
            if (errclosure != null)
                errclosure.exec("QUIT");
        }
    }
}
