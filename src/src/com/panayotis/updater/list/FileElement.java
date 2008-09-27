/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.list;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.updater.ApplicationInfo;

/**
 *
 * @author teras
 */
public abstract class FileElement {

    protected static final String SEP = System.getProperty("file.separator");
    protected String name = "";
    private String dest;
    protected int release;
    protected ApplicationInfo info;

    public FileElement(String name, String dest, UpdaterAppElements elements, ApplicationInfo appinfo) {
        if (name != null)
            this.name = name;
        release = elements.getLastRelease();
        this.dest = appinfo.updatePath(dest);

        info = appinfo;
        if (info == null) {
            throw new NullPointerException(_("Application info not provided."));
        }
    }

    public String getHash() {
        return dest + SEP + name;
    }

    public String getDestination() {
        return dest;
    }
}
