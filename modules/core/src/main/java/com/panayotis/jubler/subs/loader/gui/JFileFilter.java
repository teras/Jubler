/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.gui;

import com.panayotis.jubler.subs.loader.SubFormat;
import java.io.File;

/**
 *
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class JFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    final String desc;
    final String ext;
    private SubFormat formatHandler = null;

    public JFileFilter() {
        desc = "All files";
        ext = "*";
    }

    public JFileFilter(String ext, String desc, SubFormat formatHandler) {
        this.desc = desc;
        this.ext = ext;
        this.formatHandler = formatHandler;
    }

    public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        if (ext.equals("*"))
            return true;
        return pathname.getName().toLowerCase().endsWith(ext.toLowerCase());
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

