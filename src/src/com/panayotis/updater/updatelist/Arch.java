/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater.updatelist;

import java.util.ArrayList;

/**
 *
 * @author teras
 */
public class Arch extends ArrayList<FileElement> {
    private Alias alias;
    
    public Arch(Alias alias) {
        this.alias = alias;
    }

    String getTag() {
        return alias.getTag();
    }

    boolean isTag(String tag) {
        return alias.isTag(tag);
    }
 }
