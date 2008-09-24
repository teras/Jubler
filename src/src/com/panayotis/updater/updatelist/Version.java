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
public class Version extends ArrayList<Arch> implements Comparable {

    private int id;
    private String basedir;
    
    Version(String id, String basedir) {
        this.id = Integer.parseInt(id);
        this.basedir = basedir;
    }

    /** 
     * Forward sorting
     */
    public int compareTo(Object o) {
        return id - ((Version)o).id;
    }

    private Arch getArch(String tag) {
        for(Arch a:this) {
            if(a.isTag(tag))
                return a;
        }
        return null;
    }
    
    void mergeWith(Version other) {
        Arch thisarch;
        for(Arch a:other) {
            thisarch = getArch(a.getTag());
        }
    }
}
