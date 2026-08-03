/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.gui;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;

import javax.swing.JPanel;

/**
 * Thin base for the load/save file-dialog accessories. The encoding picker that used to live here
 * (a large generated charset menu) now lives in the reusable {@link JEncodingChooser} used by the
 * encoding bar; subclasses only carry what remains (the save panel keeps an output-FPS chooser, the
 * load panel is empty).
 */
public abstract class JFileOptions extends JPanel {

    /** Load the options onto the visual components. */
    public abstract void updateVisuals(Subtitles subs, MediaFile mfile);

    /** Apply the chosen options onto the given SubFile (and persist any defaults). */
    protected void applyOptions(SubFile sfile) {
        SubFile.saveDefaultOptions();
    }
}
