/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater.list;

import static com.panayotis.jubler.i18n.I18N._;

import java.io.File;

/**
 *
 * @author teras
 */
public class UpdaterAppElements {

    private String AppHome;
    private String AppName;
    private String iconpath;
    private String HTML;
    private int newrelease = -1;
    private int lastrelease = -1;   // Last known release, read from XML
    private String newversion = "0.0.0";
    private String lastversion = "0.0.0.0"; // Last known version, read from XML
    private int currelease;
    private String curversion;

    public UpdaterAppElements(String rel, String ver, String home) {
        currelease = -1;
        try {
            currelease = Integer.parseInt(rel);
        } catch (NumberFormatException ex) {
        }

        if (ver == null)
            ver = "0.0.0";
        curversion = ver;
        
        if (home==null) {
            throw new NullPointerException(_("Application path can not be null."));
        }
        File f = new File(home);
        if (!f.isDirectory()) {
            throw new IllegalArgumentException(_("Unable to find Application path {0}.", home));
        }
        AppHome = home;
    }

    public int getCurRelease() {
        return currelease;
    }

    public String getCurVersion() {
        return curversion;
    }

    public String getAppName() {
        return AppName;
    }

    public int getLastRelease() {
        return lastrelease;
    }

    public String getLastVersion() {
        return lastversion;
    }

    void setAppName(String AppName) {
        if (AppName == null || AppName.equals(""))
            AppName = "Unknown";
        this.AppName = AppName;
    }

    public String getAppHome() {
        return AppHome;
    }

    public String getHTML() {
        return HTML;
    }

    void setHTML(String HTML) {
        this.HTML = HTML;
    }

    public String getIconpath() {
        return iconpath;
    }

    void setIconpath(String iconpath) {
        if (iconpath==null)
            iconpath = "";
        this.iconpath = iconpath;
    }

    void updateVersion(int lastrelease, String lastversion) {
        this.lastrelease = lastrelease;
        this.lastversion = lastversion;
        if (lastrelease > newrelease) {
            newrelease = lastrelease;
            newversion = lastversion;
        }
        if (newversion==null)
            newversion = "0.0.0";
    }

    public String getNewVersion() {
        return newversion;
    }
}
