/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.list;

import com.panayotis.updater.ApplicationInfo;

/**
 *
 * @author teras
 */
public class FileRm extends FileElement {

    public FileRm(String name, String dest, UpdaterAppElements elements, ApplicationInfo info) {
        super(name, dest, elements, info);
    }

    public String toString() {
        return "-" + getHash();
    }
}
