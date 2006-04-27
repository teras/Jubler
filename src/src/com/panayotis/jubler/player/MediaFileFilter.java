/*
 * MediaFileFilter.java
 *
 * Created on 25 Οκτώβριος 2005, 5:12 μμ
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.panayotis.jubler.player;

/**
 *
 * @author teras
 */
public abstract class MediaFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {
    
    public abstract String[] getExtensions();
}
