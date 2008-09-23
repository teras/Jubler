/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater.updatelist;

/**
 *
 * @author teras
 */
public class Alias {
    private String name;
    private String tag;
    private String os;
    private String arch;

    public Alias(String name, String tag, String os, String arch) {
        this.name = name;
        this.tag = tag.toLowerCase();
        this.os = os.toLowerCase();
        this.arch = arch.toLowerCase();
    }

    boolean isTag(String tag) {
        return tag.toLowerCase().equals(this.tag);
    }

    boolean isSystem(String os, String arch) {
        return (os.toLowerCase().startsWith(this.os) && arch.toLowerCase().startsWith(this.arch));
    }
    
    public String toString() {
        return name;
    }
}
