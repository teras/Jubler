/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater.updatelist;

/**
 *
 * @author teras
 */
public class FileAdd extends FileElement {
    
    private String source;

    FileAdd(String name, String source, String dest, int id) {
        super(name, dest, id);
        this.source = source;
    }
    
    public String toString() {
        return "+"+source+SEP+name+">"+dest;
    }

}
