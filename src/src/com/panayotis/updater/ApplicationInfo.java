/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater;

import static com.panayotis.jubler.i18n.I18N._;

import java.io.File;

/**
 *
 * @author teras
 */
public class ApplicationInfo {

    private String AppHome;
    private String AppConfigFile;
    private String AppUpdaterFile;

    public String getAppConfigFile() {
        return AppConfigFile;
    }

    public String updatePath(String path) {
        if (path == null)
            path = "";
        path = path.replaceAll("\\$\\{APPHOME\\}", AppHome);
        path = path.replaceAll("\\$\\{APPCONFIG\\}", AppConfigFile);
        path = path.replaceAll("\\$\\{APPUPDATER\\}", AppUpdaterFile);
        return path;
    }

    public void setAppConfigFile(String AppConfigFile) {
        this.AppConfigFile = AppConfigFile;
    }

    public void setAppUpdaterFile(String AppUpdaterFile) {
        this.AppUpdaterFile = AppUpdaterFile;
    }
    private int release = -1;
    private String version = "0.0.0";

    public int getRelease() {
        return release;
    }

    public String getVersion() {
        return version;
    }
    /**
    true:  Some files can be ignored, if they are taken care by a distribution
    false: All files should be  updated
     */
    private boolean distributionBased = false;

    public ApplicationInfo(String AppHome) {
        if (AppHome == null) {
            throw new NullPointerException(_("Application path can not be null."));
        }
        File f = new File(AppHome);
        if (!f.isDirectory()) {
            throw new IllegalArgumentException(_("Unable to find Application path {0}.", AppHome));
        }
        this.AppHome = AppHome;
    }

    public boolean isDistributionBased() {
        return distributionBased;
    }

    public void setCurrentVersion(String rel, String ver) {
        try {
            release = Integer.parseInt(rel);
        } catch (NumberFormatException ex) {
        }
        if (ver != null)
            version = ver;
    }

    void setDistributionBased(boolean distributionBased) {
        this.distributionBased = distributionBased;
    }
}
