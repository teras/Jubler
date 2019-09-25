package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;

import javax.swing.*;
import java.io.*;

import static com.panayotis.jubler.i18n.I18N.__;

public class ExternalTool {
    private String name;
    private String path;
    private String command;
    private boolean inplace;

    public ExternalTool() {
        this("Tool Name", "tool", "%x --input %i --output %o", false);
    }

    public ExternalTool(String name, String path, String command, boolean inplace) {
        this.name = name;
        this.path = path;
        this.command = command;
        this.inplace = inplace;
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setInplace(boolean inplace) {
        this.inplace = inplace;
    }

    public boolean isInplace() {
        return inplace;
    }

    public void exec(final JubFrame jubler) {
        try {

            // Create input/output files
            File tempDir = File.createTempFile("jubler_", "_exec").getAbsoluteFile();
            String input = new File(tempDir, "input" + (inplace ? "output" : "") + ".srt").getAbsolutePath();
            final String output = inplace ? input : new File(tempDir, "output.srt").getAbsolutePath();
            SubFile new_sub_file = new SubFile(new File(input));
            Subtitles cloned_subs = new Subtitles(jubler.getSubtitles());
            FileCommunicator.save(cloned_subs, new_sub_file, null);

            final JExternalConsole console = new JExternalConsole(jubler, new Runnable() {
                @Override
                public void run() {
                    SubFile file = new SubFile(new File(output));
                    FileCommunicator.load(file);
                    Subtitles subtitles = new Subtitles(file);
                    jubler.setSubs(subtitles);
                }
            });
            console.setTitle(__("Launching application:") + " " + name);

            ProcessBuilder builder = new ProcessBuilder(getCommandLine(tempDir, input, output));
            builder.directory(tempDir);
            final Process process = builder.start();
            new Thread() {
                @Override
                public void run() {
                    try {
                        process.waitFor();
                    } catch (InterruptedException ignored) {
                    }
                    console.setResult(process.exitValue(), jubler, output);
                }
            }.start();
            new ProcThread(process, false, console).start();
            new ProcThread(process, true, console).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(jubler, e.getClass().getName() + ":\n" + e.getMessage(), __("Error found"), JOptionPane.ERROR_MESSAGE);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private String[] getCommandLine(File tempDir, String input, String output) {
        tempDir.delete();
        tempDir.mkdir();
        String[] parts = command.trim().split("\\s+");
        for (int i = 0; i < parts.length; i++)
            switch (parts[i]) {
                case "%x":
                    parts[i] = path;
                    break;
                case "%i":
                    parts[i] = input;
                    break;
                case "%o":
                    parts[i] = output;
                    break;
            }
        return parts;
    }
}

class ProcThread extends Thread {

    private final Process proc;
    private final boolean asError;
    private final JExternalConsole console;

    ProcThread(Process proc, boolean asError, JExternalConsole console) {
        this.proc = proc;
        this.asError = asError;
        this.console = console;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(asError ? proc.getErrorStream() : proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (asError)
                    console.addErrLine(line);
                else
                    console.addOutLine(line);
                if (isInterrupted())
                    break;
            }
        } catch (Exception ignored) {
        }
    }
}