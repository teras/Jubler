/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.tools.Tool;
import com.panayotis.jubler.tools.ToolMenu;
import com.panayotis.jubler.tools.ToolMenu.Location;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Tools-menu entry that opens the modeless subtitle-downloader window bound to the current document.
 * Registered through the standard Tools extension point, so it needs no core changes.
 */
public class SubDownloadTool extends Tool {

    public SubDownloadTool() {
        super(new ToolMenu(__("Download subtitles…"), "TDL", Location.FILETOOL, 0, 0));
    }

    @Override
    public void updateData(JubFrame current) {
        /* The window reads everything it needs straight from the frame when it opens. */
    }

    /* Only usable once a video is attached — the search is seeded from and matched to that video. */
    @Override
    public boolean isAvailable(JubFrame current) {
        return current != null && current.getMediaFile() != null && current.getMediaFile().getVideoFile() != null;
    }

    @Override
    public boolean execute(JubFrame current) {
        if (!isAvailable(current)) {
            JOptionPane.showMessageDialog(current,
                    __("Attach a video to this document first, then search for its subtitles."),
                    menu.text, JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        SubDownloadFrame.openFor(current);
        return true;
    }

    @Override
    protected JComponent constructVisuals() {
        return new JPanel();
    }

    @Override
    public String getCommandOptionName() {
        return null; // Hide from command line tools
    }

    @Override
    public String getCommandLineHelp() {
        return "";
    }

    @Override
    public Collection<String> gatherToolTags() {
        return Collections.emptyList();
    }

    @Override
    public String executeParams(Map<String, String> params, boolean debug) {
        return null;
    }
}
