/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater.updatelist;

/**
 *
 * @author teras
 */
public class UpdateFile implements FileElement {
    private String source;
    private String dest;

    UpdateFile(String source, String dest) {
        this.source = source;
        this.dest = dest;
    }
    
    public String toString() {
        return "("+source+"|"+dest+")";
    }
}
