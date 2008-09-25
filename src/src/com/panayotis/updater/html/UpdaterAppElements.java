/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.html;

/**
 *
 * @author teras
 */
public class UpdaterAppElements {

    private String AppName;
    private String iconpath;
    private String HTML;
    private int newrelease = -1;
    private String newversion = "0.0.0";
    private int currelease;
    private String curversion;

    public UpdaterAppElements(String rel, String ver) {
        currelease = Integer.parseInt(rel);
        curversion = ver;
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

    public void setAppName(String AppName) {
        this.AppName = AppName;
    }

    public String getHTML() {
        return HTML;
    }

    public void setHTML(String HTML) {
        this.HTML = HTML;
    }

    public String getIconpath() {
        return iconpath;
    }

    public void setIconpath(String iconpath) {
        this.iconpath = iconpath;
    }

    public void updateVersion(int lastid, String lastrelease) {
        if (lastid > newrelease) {
            newrelease = lastid;
            newversion = lastrelease;
        }
    }

    public String getNewVersion() {
        return newversion;
    }
}
