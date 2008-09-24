/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.list;

/**
 *
 * @author teras
 */
public class FileRm extends FileElement {

    public FileRm(String name, String dest, int id) {
        super(name, dest, id);
    }

    public String toString() {
        return "-" + getHash();
    }
}
