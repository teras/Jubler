/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater.updatelist;

/**
 *
 * @author teras
 */
public class Arch {
    private String tag;
    private String name;
    private String os;
    private String arch;

    public Arch(String tag, String name, String os, String arch) {
        this.tag = tag.toLowerCase();
        this.name = name;
        this.os = os.toLowerCase();
        this.arch = arch.toLowerCase();
    }

    boolean isTag(String tag) {
        return this.tag.toLowerCase().equals(tag);
    }

    boolean isCurrent() {
        String c_os = System.getProperty("os.name");
        String c_arch = System.getProperty("os.arch");
        return (c_os.toLowerCase().startsWith(os) && c_arch.toLowerCase().startsWith(arch));
    }
    
}
