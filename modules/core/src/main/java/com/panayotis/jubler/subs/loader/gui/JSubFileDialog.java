/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.gui;

import com.panayotis.appenh.AFileChooser;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Frame;
import java.io.File;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.subs.SubFile.basic_format;

/**
 * Subtitle open/save dialogs, backed by appenh {@link AFileChooser} (native/portal-ready, with a
 * Swing fallback today). On save the chosen subtitle format comes from the selected filter; encoding
 * and FPS are document properties (set via the encoding bar / Information tab), so the dialog needs
 * no accessory panel.
 */
public class JSubFileDialog {

    private static File lastDirectory = initializeDefaultDirectory();

    private static File initializeDefaultDirectory() {
        File dir = new File(FileCommunicator.getDefaultDirPath());
        return dir.isDirectory() && dir.canRead() ? dir : new File(System.getProperty("user.home"));
    }

    public SubFile getLoadFile(Frame parent, MediaFile mfile) {
        AFileChooser fc = new AFileChooser()
                .parent(parent)
                .title(__("Load Subtitles"))
                .loadButtonTitle(__("Load Subtitles"))
                .directory(lastDirectory)
                .mode(AFileChooser.FileSelectionMode.FilesOnly);
        for (SubFormat f : Availabilities.formats.getFormats())
            fc.filter(f.getExtension(), f.getName());
        File file = fc.loadSingle();
        if (file == null)
            return null;
        rememberDir(file);
        return new SubFile(file, SubFile.EXTENSION_GIVEN);   // format is auto-detected on load
    }

    public SubFile getSaveFile(Frame parent, Subtitles subs, MediaFile mfile) {
        SubFormat current = subs.getSubFile().getFormat();
        AFileChooser fc = new AFileChooser()
                .parent(parent)
                .title(__("Save Subtitles"))
                .saveButtonTitle(__("Save Subtitles"))
                .directory(lastDirectory)
                .file(subs.getSubFile().getStrippedFile().getName());
        fc.filter(current.getExtension(), current.getName());   // the document's format first
        for (SubFormat f : Availabilities.formats.getFormats())
            if (!f.getName().equals(current.getName()))
                fc.filter(f.getExtension(), f.getName());

        while (true) {
            File chosen = fc.save();
            if (chosen == null)
                return null;
            SubFile sfile = new SubFile(subs.getSubFile());   // carries the document's encoding + FPS
            sfile.setFile(chosen);
            sfile.setFormat(formatFor(fc.selectedFilter(), current));
            sfile.updateFileByType();   // normalise the extension to the chosen format
            File out = sfile.getSaveFile();
            if (!out.exists() || confirmOverwrite(parent, out)) {
                rememberDir(out);
                return sfile;
            }
            // overwrite declined → reopen the save dialog
        }
    }

    /* Map the filter the user selected back to a subtitle format (filter description == format name). */
    private static SubFormat formatFor(FileNameExtensionFilter selected, SubFormat fallback) {
        if (selected != null)
            for (SubFormat f : Availabilities.formats.getFormats())
                if (f.getName().equals(selected.getDescription()))
                    return f.newInstance();
        return (fallback == null ? basic_format : fallback).newInstance();
    }

    private static boolean confirmOverwrite(Frame parent, File file) {
        return JOptionPane.showConfirmDialog(parent,
                __("File already exists. Do you want to overwrite it?"),
                __("Confirm Overwrite"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private static void rememberDir(File file) {
        File dir = file.isDirectory() ? file : file.getParentFile();
        if (dir != null) {
            lastDirectory = dir;
            FileCommunicator.setDefaultDir(dir);
        }
    }
}
