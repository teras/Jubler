/*
 *  DVDMaestroFileFilter.java 
 * 
 *  Created on: Jun 22, 2009 at 2:32:23 PM
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

package com.panayotis.jubler.subs.loader;

import java.io.File;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SimpleFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {
    private String extension = null;
    private String description = null;
    
    public SimpleFileFilter(){}
    public SimpleFileFilter(String extension){
        try{
            this.extension = extension.toLowerCase();
        }catch(Exception ex){}
    }

    public SimpleFileFilter(String extension, String description){
        this(extension);
        this.description = description;
    }
    
    public boolean accept(File pathname) {
        if (pathname.isDirectory()) {
            return true;
        }
        String fname = pathname.getName().toLowerCase();
        return fname.endsWith(extension);
    }


    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}