/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.gui;

import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.loader.SubFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class JFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    private static List<String> extensions = null;

    private static Collection<String> getExtensions() {
        if (extensions == null) {
            extensions = new ArrayList<>();
            Availabilities.formats.getFormats().forEach(it
                    -> extensions.add(it.getExtension().toLowerCase()));
        }
        return extensions;
    }

    final String desc;
    final String ext;
    private SubFormat formatHandler = null;

    public JFileFilter() {
        desc = __("All subtitle files");
        ext = "*";
    }

    public JFileFilter(SubFormat format) {
        this.desc = format.getDescription();
        this.ext = format.getExtension().toLowerCase();
        this.formatHandler = format;
    }

    public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        String filename = pathname.getName().toLowerCase();
        if (ext.equals("*")) {
            int pos = filename.lastIndexOf(".");
            return pos >= 1 && getExtensions().contains(filename.substring(pos + 1));
        } else
            return filename.endsWith(ext);
    }

    public String getDescription() {
        return desc;
    }

    /**
     * @return the formatHandler
     */
    public SubFormat getFormatHandler() {
        return formatHandler;
    }

    /**
     * @param formatHandler the formatHandler to set
     */
    public void setFormatHandler(SubFormat formatHandler) {
        this.formatHandler = formatHandler;
    }
}//end public class SimpleFileFilter

