/* (c) 2017 by CrossMobile.tech
 *
 * CrossMobile is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 * CrossMobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CrossMobile; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.panayotis.jubler.tools.externals;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class Commander {

    private static final int INVALID_EXIT_VALUE = Integer.MIN_VALUE;
    private static final String ENCODING = "UTF-8";
    private static final boolean AS_WINDOWS = System.getProperty("os.name", "unknown").toLowerCase().startsWith("windows");

    private final List<String> command = new ArrayList<String>();

    private final Map<String, String> envp = new HashMap<String, String>();
    private Consumer<String> outS, errS;
    private Consumer<Character> outC, errC;
    private BiConsumer<byte[], Integer> outB, errB;
    private Writer outW, errW;
    private Consumer<Integer> finish;
    private File currentDir;
    private Process proc;
    private BufferedWriter bufferin = null;
    private OutputProxy procout = null;
    private OutputProxy procerr = null;
    private int exit_value = INVALID_EXIT_VALUE;
    private boolean out_is_terminated = true;
    private boolean err_is_terminated = true;
    private Thread waitThread;
    private long pid;

    public Commander(List<String> command) {
        for (String candidate : command)
            if (candidate != null)
                add(candidate);
    }

    public Commander(String... command) {
        this(Arrays.asList(command));
    }

    private Commander add(String arg) {
        if (arg != null)
            command.add(arg);
        return this;
    }

    public Commander addArgument(String arg) {
        return add(arg);
    }

    public Commander setOutListener(Consumer<String> out) {
        this.outS = out;
        this.outC = null;
        this.outW = null;
        this.outB = null;
        return this;
    }

    public Commander setCharOutListener(Consumer<Character> out) {
        this.outC = out;
        this.outS = null;
        this.outW = null;
        this.outB = null;
        return this;
    }

    public Commander setOutWriter(Writer out) {
        this.outW = out;
        this.outS = null;
        this.outC = null;
        this.outB = null;
        return this;
    }

    public Commander setOutListener(BiConsumer<byte[], Integer> out) {
        this.outB = out;
        this.outW = null;
        this.outS = null;
        this.outC = null;
        return this;
    }

    public Commander setErrListener(Consumer<String> err) {
        this.errS = err;
        this.errC = null;
        this.errW = null;
        this.errB = null;
        return this;
    }

    public Commander setCharErrListener(Consumer<Character> err) {
        this.errC = err;
        this.errS = null;
        this.errW = null;
        this.errB = null;
        return this;
    }

    public Commander setErrWriter(Writer err) {
        this.errW = err;
        this.errS = null;
        this.errC = null;
        this.errB = null;
        return this;
    }

    public Commander setErrListener(BiConsumer<byte[], Integer> err) {
        this.errB = err;
        this.errS = null;
        this.errC = null;
        this.errW = null;
        return this;
    }

    public Commander setEndListener(Consumer<Integer> finish) {
        this.finish = finish;
        return this;
    }

    public Commander setCurrentDir(File cdir) {
        if (proc != null)
            throw new RuntimeException("Command already running");
        this.currentDir = cdir;
        return this;
    }

    public long getPid() {
        return pid;
    }

    public Commander appendEnvironmentalParameters(Map<String, String> params) {
        if (proc != null)
            throw new RuntimeException("Command already running");
        if (params != null)
            this.envp.putAll(params);
        return this;
    }

    public Commander appendEnvironmentalParameter(String name, String value) {
        if (proc != null)
            throw new RuntimeException("Command already running");
        if (name != null && value != null)
            this.envp.put(name, value);
        return this;
    }

    public synchronized Commander exec() {
        if (proc != null)
            throw new RuntimeException("Command already running");
        exit_value = INVALID_EXIT_VALUE;
        proc = null;
        waitThread = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(currentDir);
            pb.environment().putAll(envp);
            proc = pb.start();
            bufferin = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), ENCODING));
            procout = new OutputProxy(true);
            procerr = new OutputProxy(false);
            out_is_terminated = false;
            err_is_terminated = false;
            procout.worker.start();
            procerr.worker.start();
            findPID();
        } catch (IOException ex) {
            sendLine("Process can not start: " + ex.getMessage(), errS, errC);
            if (finish != null)
                finish.accept(INVALID_EXIT_VALUE);
        }
        return this;
    }

    @SuppressWarnings("UseSpecificCatch")
    private void findPID() {
        pid = -1;
        if (proc != null) {
            // First try Java 9
            try {
                pid = (Long) proc.getClass().getMethod("getPid", (Class[]) null).invoke(proc, (Object[]) null);
                return;
            } catch (Exception ex) {
            }

            String fieldName = null;
            String switchV = proc.getClass().getName();
            if (switchV.equals("java.lang.UNIXProcess")) {
                fieldName = "pid";
            } else if (switchV.equals("java.lang.Win32Process") || switchV.equals("java.lang.ProcessImpl")) {
                fieldName = "handle";

            }

            if (fieldName != null)
                try {
                    Field field = proc.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    long value = field.getLong(proc);
                    pid = value;
                } catch (Exception ex) {
                }
        }
    }

    public boolean isActive() {
        return proc != null;
    }

    public Commander sendLine(String line) {
        if (bufferin == null) {
            sendLine("Request to send data inappropriate: process not active", errS, errC);
            return this;
        }
        if (line == null)
            return this;
        try {
            bufferin.write(line);
            bufferin.newLine();
            bufferin.flush();
        } catch (IOException ex) {
        }
        return this;
    }

    public void terminateInput() {
        if (bufferin != null)
            try {
                bufferin.close();
            } catch (IOException ex) {
            }
        bufferin = null;
    }

    private synchronized void doWaitFor() {
        if (proc == null)
            return;
        try {
            proc.waitFor();
        } catch (InterruptedException ex) {
        }
        try {
            exit_value = proc.exitValue();
        } catch (Exception e) {
            exit_value = 0;
        }
        proc = null;
    }

    public Commander waitFor() {
        if (proc == null || waitThread != null)
            return this;

        /*
         * Use this thread trick, to suspend this thread until the streams have
         * finished executing. Thus, first is "finish" CallBack being informed
         * and then waitFor exits. A new thread is required since we want to
         * catch InterruptedExceptions only for the waitThread, not the current
         * thread.
         */
        waitThread = new Thread() {

            @SuppressWarnings("SleepWhileInLoop")
            @Override
            public void run() {
                doWaitFor();    // will also nullify `proc`
                while (!(out_is_terminated && err_is_terminated) && !waitThread.isInterrupted()) {
                    try {
                        synchronized (this) {
                            wait(1000);
                        }
                    } catch (InterruptedException ex) {
                    }
                }
            }
        };
        waitThread.start();
        try {
            waitThread.join();
        } catch (InterruptedException ex) {
        }
        waitThread = null;
        return this;
    }

    @SuppressWarnings("UseSpecificCatch")
    public Commander kill() {
        try {
            if (proc != null)
                proc.destroy();
        } catch (Exception e) {
        }
        return this;
    }

    public int exitValue() {
        if (exit_value == INVALID_EXIT_VALUE)
            throw new IllegalThreadStateException("Unable to retrieve exit value");
        return exit_value;
    }

    private synchronized void doInterrupt() {
        if (waitThread != null)
            waitThread.interrupt();
    }

    private synchronized void outputTerminated(OutputProxy proxy, boolean asOutput) {
        /*
         * Do not care if no call back is defined
         */
        if (finish == null) {
            doInterrupt();
            if (asOutput)
                out_is_terminated = true;
            else
                err_is_terminated = true;
            return;
        }
        /*
         * Do not care if it is already nulled
         */
        if (procout == null && procerr == null)
            return;

        if (proxy == procout)
            procout = null;
        if (proxy == procerr)
            procerr = null;
        if (procerr != null || procout != null)
            return;

        /*
         * Kill after both streams have ended
         */
        out_is_terminated = true;
        err_is_terminated = true;
        terminateInput();
        kill();
        doWaitFor();
        finish.accept(exit_value);
        doInterrupt();
    }

    @Override
    public String toString() {
        return commandToString();
    }

    private String commandToString() {
        StringBuilder out = new StringBuilder();
        for (String part : command)
            if (part != null)
                out.append(escapeIfRequired(part)).append(" ");
        return out.toString();
    }

    private String environmentToString() {
        StringBuilder out = new StringBuilder();
        if (envp != null && !envp.isEmpty())
            for (String name : envp.keySet())
                out.append(name).append("=").append(escapeIfRequired(envp.get(name))).append("; ");
        return out.toString();
    }

    private String pathToString() {
        if (currentDir != null)
            return "cd " + escapeIfRequired(currentDir.getAbsolutePath()) + "; ";
        return "";
    }

    private String escapeIfRequired(String input) {
        if (input.contains(" ") || input.contains("!"))
            return "'" + input + "'";
        else
            return input;
    }

    private class OutputProxy {

        private final BufferedReader inR;
        private final BufferedInputStream inS;
        private final Thread worker;
        private final Consumer<String> listenS;
        private final Consumer<Character> listenC;
        private final Writer listenW;
        private final BiConsumer<byte[], Integer> listenB;

        private OutputProxy(final boolean asOut) throws UnsupportedEncodingException {
            if (asOut) {
                if (outB == null) {
                    inR = new BufferedReader(new InputStreamReader(proc.getInputStream(), ENCODING));
                    inS = null;
                } else {
                    inS = new BufferedInputStream(proc.getInputStream());
                    inR = null;
                }
                listenS = outS;
                listenC = outC;
                listenW = outW;
                listenB = outB;
            } else {
                if (errB == null) {
                    inR = new BufferedReader(new InputStreamReader(proc.getErrorStream(), ENCODING));
                    inS = null;
                } else {
                    inS = new BufferedInputStream(proc.getErrorStream());
                    inR = null;
                }
                listenS = errS;
                listenC = errC;
                listenW = errW;
                listenB = errB;
            }
            worker = new Thread() {
                @Override
                public void run() {
                    String line;
                    int c;
                    try {
                        if (listenC != null)
                            while (inR != null && (c = inR.read()) >= 0 && (!isInterrupted()))
                                listenC.accept((char) c);
                        else if (listenW != null)
                            while (inR != null && (c = inR.read()) >= 0 && (!isInterrupted()))
                                listenW.write(c);
                        else if (listenB != null) {
                            byte[] buffer = new byte[0xf000];
                            int hm;
                            while (inS != null && (hm = inS.read(buffer)) >= 0)
                                listenB.accept(buffer, hm);
                        } else // without `else if', to make sure that the output is consumed
                            while (inR != null && (line = inR.readLine()) != null && (!isInterrupted()))
                                if (listenS != null)
                                    listenS.accept(line);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            if (inR != null)
                                inR.close();
                        } catch (IOException ex) {
                        }
                        try {
                            if (inS != null)
                                inS.close();
                        } catch (IOException ex) {
                        }
                    }
                    outputTerminated(OutputProxy.this, asOut);
                }
            };
        }
    }

    private static void sendLine(String what, Consumer<String> stringStream, Consumer<Character> charStream) {
        if (stringStream != null)
            stringStream.accept(what);
        else if (charStream != null)
            for (char c : what.toCharArray())
                charStream.accept(c);
    }

    public interface Consumer<T> {
        void accept(T value);
    }

    public interface BiConsumer<T, L> {
        void accept(T buffer, L length);
    }
}
