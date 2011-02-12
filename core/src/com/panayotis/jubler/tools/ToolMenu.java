/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

/**
 *
 * @author teras
 */
public class ToolMenu {

    public final String text;
    public final String parent;
    public final String key;
    public final String tag;

    public ToolMenu(String text, String parent, String key, String tag) {
        this.text = text;
        this.parent = parent;
        this.key = key;
        this.tag = tag;
    }
}
