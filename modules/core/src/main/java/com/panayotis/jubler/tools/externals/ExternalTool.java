/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import static com.panayotis.jubler.i18n.I18N.__;

public class ExternalTool {
    private static final SubFormat DEFAULT;

    static {
        ArrayList<SubFormat> formats = Availabilities.formats.getFormats();
        SubFormat found = formats.get(0);
        for (SubFormat format : formats)
            if (format.getExtension().equalsIgnoreCase("srt")) {
                found = format;
                break;
            }
        DEFAULT = found;
    }

    private String name;
    private String path;
    private String command;
    private SubFormat format;

    public ExternalTool() {
        this("Tool Name", "tool", "%x --input %i --output %o", (SubFormat) null);
    }

    public ExternalTool(String name, String path, String command, String className) {
        this(name, path, command, SubFormat.initFromClassname(className));
    }

    public ExternalTool(String name, String path, String command, SubFormat format) {
        this.name = name;
        this.path = path;
        this.command = command;
        this.format = format == null ? DEFAULT : format;
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

    public SubFormat getFormat() {
        return format;
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

    public void setFormat(SubFormat format) {
        this.format = format;
    }

    public void exec(final JubFrame jubler) {
        try {
            // Create input/output files
            File tempDir = File.createTempFile("jubler_", "_exec").getAbsoluteFile();
            tempDir.delete();
            tempDir.mkdirs();

            SubFile inputSubfile = new SubFile(new File(tempDir, "input." + format.getExtension()), true);
            inputSubfile.setFormat(format);
            SubFile outputSubfile = command.contains("%o") ? new SubFile(new File(tempDir, "output." + format.getExtension()), true) : inputSubfile;
            outputSubfile.setFormat(format);

            Subtitles cloned_subs = new Subtitles(jubler.getSubtitles());
            String result = FileCommunicator.save(cloned_subs, inputSubfile, null);
            if (result != null) {
                JOptionPane.showMessageDialog(jubler, result, __("Error found"), JOptionPane.ERROR_MESSAGE);
                return;
            }

            String[] commandLine = getCommandLine(tempDir, inputSubfile.getSaveFile().getAbsolutePath(), outputSubfile.getSaveFile().getAbsolutePath());
            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.directory(tempDir);
            Process process = builder.start();

            JExternalConsole console = new JExternalConsole(jubler, name, process::destroy);
            console.addOutLine(__("Executing command:"));
            console.addOutLine(String.join(" ", commandLine));
            console.addOutLine("----------------------------------");

            new ProcThread(process, false, console).start();
            new ProcThread(process, true, console).start();
            new ExitThread(process, console).start();

            console.setVisible(true);
            if (process.exitValue() == 0) {
                jubler.loadProcessedFile(outputSubfile, name);
                FileCommunicator.deleteRecursive(tempDir);
            }
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
            parts[i] = parts[i].replace("%x", path).replace("%i", input).replace("%o", output);
        System.out.println(Arrays.toString(parts));
        return parts;
    }
}


class ExitThread extends Thread {
    private final Process proc;
    private final JExternalConsole console;

    ExitThread(Process proc, JExternalConsole console) {
        this.proc = proc;
        this.console = console;
    }

    @Override
    public void run() {
        try {
            proc.waitFor();
        } catch (InterruptedException ignored) {
        }
        console.setResult(proc.exitValue());
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