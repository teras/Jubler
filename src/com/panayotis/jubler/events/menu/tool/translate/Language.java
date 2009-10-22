/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.events.menu.tool.translate;

/**
 *
 * @author teras
 */
public class Language {

    private String name;
    private String id;

    public Language(String id, String name) {
        this.name = name;
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    public String getID() {
        return id;
    }
}
