/*
 * SubFile.java
 *
 * Created on February 4, 2007, 4:55 PM
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
 */

package com.panayotis.jubler.subs;

import java.io.File;

/**
 *
 * @author teras
 */
public class SubFile {
    
    private File current_file;
    private File last_opened_file;
    
    /** Creates a new instance of SubFile */
    public SubFile() {
    }
    
    public SubFile (SubFile old) {
        current_file = old.current_file;
        last_opened_file = old.last_opened_file;
    }

    public File getCurrentFile() { return current_file; }
    public void setCurrentFile(File f) { current_file = f; }
    
    public File getLastOpenedFile() { return last_opened_file; }
    public void setLastOpenedFile(File f) { last_opened_file = f; }
}
