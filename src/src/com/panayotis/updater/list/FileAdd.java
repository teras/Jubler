/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater.list;

/**
 * 
 * @author teras
 */
public class FileAdd extends FileElement {
   
    /** This is actually a URL */
    private String source;

    FileAdd(String name, String source, String dest, UpdaterAppElements elements) {
        super(name, dest, elements);
        this.source = source;
    }
    
    public String toString() {
        return "+"+source+SEP+name+">"+getDestination();
    }

}
