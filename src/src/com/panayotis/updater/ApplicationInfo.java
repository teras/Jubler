/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater;

import static com.panayotis.jubler.i18n.I18N._;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author teras
 */
public class ApplicationInfo {

    protected static final String SEP = System.getProperty("file.separator");
    private String AppHome;
    private String AppUpdaterFile;
    private String AppConfigFile = null;
    private int release = -1;
    private String version = "0.0.0";
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
        AppUpdaterFile = AppHome + SEP + "updater.xml";
    }

    public boolean isDistributionBased() {
        return distributionBased;
    }

    public String getAppConfigFile() {
        if (AppConfigFile == null)
            throw new NullPointerException("Application configuration file unknown! Please set it first.");
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

    private void fileIsValid(String file, String type) throws IOException {
        if (file == null) {
            throw new NullPointerException(type + " file could not be null.");
        }
        File f = new File(file);
        File p = f.getParentFile();
        if (!(p.exists()))
            p.mkdirs();
        if (!(p.exists()) && p.isDirectory() && p.canWrite())
            throw new IOException(type + "parent file is not writable.");
        if (f.exists() && (!f.canWrite()))
            throw new IOException(type + " file is not writable.");
    }

    public void setAppConfigFile(String AppConfigFile) throws IOException {
        fileIsValid(AppConfigFile, "Application configuration");
        this.AppConfigFile = AppConfigFile;
    }

    public void setAppUpdaterFile(String AppUpdaterFile) throws IOException {
        fileIsValid(AppUpdaterFile, "Application updater");
        this.AppUpdaterFile = AppUpdaterFile;
    }

    public int getRelease() {
        return release;
    }

    public String getVersion() {
        return version;
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
