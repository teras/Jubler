/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.list;

import com.panayotis.updater.ApplicationInfo;

/**
 * 
 * @author teras
 */
public class FileAdd extends FileElement {

    /** This is actually a URL */
    private String source;

    FileAdd(String name, String source, String dest, UpdaterAppElements elements, ApplicationInfo info) {
        super(name, dest, elements, info);
        if (source == null)
            source = "";
        this.source = elements.getBaseURL() + source;
    }

    public String toString() {
        return "+" + source + SEP + name + ">" + getDestination();
    }
}
