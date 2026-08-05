/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.gui;

import com.panayotis.appenh.AFileChooser;
import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.loader.SubFormat;

import javax.swing.JOptionPane;
import java.awt.Frame;
import java.io.File;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Subtitle open/save dialogs, backed by appenh {@link AFileChooser} (native/portal-ready, with a
 * Swing fallback today). The subtitle format is a document property chosen on the encoding bar, so
 * the save dialog only offers the document's current format and needs no accessory panel; encoding
 * and FPS are likewise document properties set on that bar.
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
                // Suggest the name already carrying the document format's extension.
                .file(subs.getSubFile().getStrippedFile().getName() + "." + current.getExtension());
        // The format is chosen up-front on the encoding bar, so the dialog offers only that one filter.
        // Nothing here has to map a filter back to a format, which also sidesteps the Flatpak portal's
        // inability to tie the chosen filter to a file extension (xdg-desktop-portal#496).
        fc.filter(current.getExtension(), current.getName());

        while (true) {
            File chosen = fc.save();
            if (chosen == null)
                return null;
            SubFile sfile = new SubFile(subs.getSubFile());   // carries the document's format, encoding + FPS
            sfile.setFile(chosen);
            if (SystemDependent.isFlatpak()) {
                // The portal's native save dialog already grants exactly this path and has run its own
                // overwrite prompt, so accept it verbatim and write straight away.
                rememberDir(sfile.getSaveFile());
                return sfile;
            }
            sfile.updateFileByType();   // normalise the extension to the document's format
            File out = sfile.getSaveFile();
            if (!out.exists() || confirmOverwrite(parent, out)) {
                rememberDir(out);
                return sfile;
            }
            // overwrite declined → reopen the save dialog
        }
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
