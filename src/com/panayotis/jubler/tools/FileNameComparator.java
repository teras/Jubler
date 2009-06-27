/*
 *  FileComparator.java 
 * 
 *  Created on: 27-Jun-2009 at 15:09:32
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
package com.panayotis.jubler.tools;

import java.io.File;
import java.util.Comparator;

/**
 * This class compare names of two files, and thus the result of 
 * string.compareTo is produced.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class FileNameComparator implements Comparator<File> {

    public int compare(File f1, File f2) {
        try {
            String f1_name = f1.getName();
            String f2_name = f2.getName();
            int compare_result = f1_name.compareTo(f2_name);
            return compare_result;
        } catch (Exception ex) {
            return -1;
        }
    }//end public int compare(File f1, File f2) 
}//end public class FileNameComparator extends Comparator

