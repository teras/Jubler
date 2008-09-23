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
public class Version extends ArrayList<Arch> {

    private String id;
    private String basedir;
    
    Version(String id, String basedir) {
        this.id = id;
        this.basedir = basedir;
    }
}
