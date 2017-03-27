/*
 *  JFileFilter.java 
 * 
 *  Created on: 22-Oct-2009 at 20:42:12
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * This file is part of Jubler.
 * 
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 * 
 * 
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Contributor(s):
 * 
 */

package com.panayotis.jubler.subs.loader.gui;

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

