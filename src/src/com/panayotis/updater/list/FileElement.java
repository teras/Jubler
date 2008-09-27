/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.list;

/**
 *
 * @author teras
 */
public abstract class FileElement {

    protected static final String SEP = System.getProperty("file.separator");
    protected String name;
    private String dest;
    protected int id;

    public FileElement(String name, String dest, UpdaterAppElements elements) {
        if (name==null)
            name = "";
        this.name = name;
        id = elements.getLastRelease();
        
        if (dest==null)
            dest = "";
        dest.replaceAll("$\\{APPHOME\\}", elements.getAppHome());
        this.dest = dest;
    }

    public String getHash() {
        return dest + SEP + name;
    }
    
    public String getDestination() {
        return dest;
    }
}
