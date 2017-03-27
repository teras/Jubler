/*
 * SubFormats.java
 *
 * Created on 22 Ιούνιος 2005, 3:40 πμ
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

package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.plugins.PluginManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *
 * @author teras and Hoang Duy Tran
 */
public class AvailSubFormats {

    private final ArrayList<SubFormat> Formats;
    private PlainText pl_txt = new PlainText();
    private SubFormatOrderComparator sfmt_order_comp = new SubFormatOrderComparator();
    int current;

    /**
     * Creates a new instance of SubFormats
     */
    public AvailSubFormats() {
        current = 0;
        Formats = new ArrayList<SubFormat>();
        PluginManager.manager.callPluginListeners(this);
        pl_txt.setClassLoader(ClassLoader.getSystemClassLoader());
        add(pl_txt);
    }

    public boolean hasMoreElements() {
        if (current < Formats.size())
            return true;
        return false;
    }

    public SubFormat nextElement() {
        return Formats.get(current++);
    }

    public int size() {
        return Formats.size();
    }

    public SubFormat findFromDescription(String name) {
        if (name == null)
            return null;
        for (int i = 0; i < Formats.size(); i++) {
            SubFormat fmt = Formats.get(i);
            String desc = fmt.getDescription();
            boolean is_found = desc.equals(name);
            if (is_found)
                return fmt;
        }
        return null;
    }

    public SubFormat findFromName(String ext) {
        if (ext == null)
            return null;
        for (int i = 0; i < Formats.size(); i++)
            if (Formats.get(i).getName().equals(ext))
                return Formats.get(i);
        return null;
    }

    /**
     * Find a single instance of handler that handle the given extension. Since
     * the same file extension can be used by multiple formats, the routine will
     * return null when more than one instances are found.
     *
     * @param ext The subtitle file's extension given.
     * @return unique file handler for the given extension, null if not found or
     * there are more than one handler which can handle the same file extension.
     */
    public SubFormat findFromExtension(String ext) {
        boolean is_found = false;
        String found_extension = null;
        SubFormat format = null;
        ArrayList<SubFormat> found_list = new ArrayList<SubFormat>();
        for (SubFormat found_format : Formats) {
            found_extension = found_format.getExtension();
            is_found = found_extension.equalsIgnoreCase(ext);
            if (is_found)
                found_list.add(found_format);//end if
        }//end for (SubFormat found_format : Formats)
        is_found = (found_list.size() == 1);
        if (is_found)
            format = found_list.get(0);//end if (is_found)
        return format;
    }//end public SubFormat findFromExtension(String ext) 

    public SubFormat get(int i) {
        return Formats.get(i);
    }

    /**
     * Add one instance of the format into the list and sort the instances into
     * the order number.
     *
     * @param format The instance of format to be added.
     */
    public void add(SubFormat format) {
        Formats.add(format);
        Collections.sort(Formats, sfmt_order_comp);
    }//end public void add(SubFormat format)

    /**
     * @return the Formats
     */
    public ArrayList<SubFormat> getFormats() {
        return Formats;
    }
}

class SubFormatOrderComparator implements Comparator<SubFormat> {

    public int compare(SubFormat o1, SubFormat o2) {
        int comp = -1;
        try {
            boolean is_same = (o1 == o2);
            if (is_same)
                comp = 0;
            else {
                int o1_order = o1.getFormatOrder();
                int o2_order = o2.getFormatOrder();
                comp = o1_order - o2_order;
            }//end if (is_same)/else
        } catch (Exception ex) {
        }
        return comp;
    }//end public int compare(SubFormat o1, SubFormat o2) 
}