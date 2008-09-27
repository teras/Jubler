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

    private String AppHomeDir;
    private int release = -1;
    private String version = "0.0.0";

    public String getApplicationHome() {
        return AppHomeDir;
    }

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

    public ApplicationInfo(String AppHomeDir) {
        if (AppHomeDir == null) {
            throw new NullPointerException(_("Application path can not be null."));
        }
        File f = new File(AppHomeDir);
        if (!f.isDirectory()) {
            throw new IllegalArgumentException(_("Unable to find Application path {0}.", AppHomeDir));
        }
        this.AppHomeDir = AppHomeDir;
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
