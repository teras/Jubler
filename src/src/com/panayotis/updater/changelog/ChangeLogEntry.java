/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater.changelog;

/**
 *
 * @author teras
 */
public class ChangeLogEntry {

    private String version;
    private String description;
    
    public ChangeLogEntry (String version) {
        this.version = version;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDescription() {
        return description;
    }
}
