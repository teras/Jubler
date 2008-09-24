/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.updatelist;

/**
 *
 * @author teras
 */
public abstract class FileElement {

    protected static final String SEP = System.getProperty("file.separator");
    protected String name;
    protected String dest;
    protected int id;

    public FileElement(String name, String dest, int id) {
        this.name = name;
        this.dest = dest;
        this.id = id;
    }

    public String getHash() {
        return dest + SEP + name;
    }
}
