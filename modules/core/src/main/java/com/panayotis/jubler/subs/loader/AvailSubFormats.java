/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AvailSubFormats implements PluginContext {

    private final ArrayList<SubFormat> Formats;
    private PlainText pl_txt = new PlainText();
    private SubFormatOrderComparator sfmt_order_comp = new SubFormatOrderComparator();
    int current;

    /**
     * Creates a new instance of SubFormats
     */
    public AvailSubFormats() {
        current = 0;
        Formats = new ArrayList<>();
        PluginManager.getManager().callPluginListeners(this);
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
     * Find a format handler that handles the given extension. When multiple formats
     * support the same extension, the first matching format is returned, giving
     * priority to extension-based detection over pattern matching.
     *
     * @param ext The subtitle file's extension given.
     * @return the first file handler for the given extension, null if not found.
     */
    public SubFormat findFromExtension(String ext) {
        if (ext == null)
            return null;

        for (SubFormat found_format : Formats) {
            String found_extension = found_format.getExtension();
            if (found_extension.equalsIgnoreCase(ext))
                return found_format;
        }
        return null;
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