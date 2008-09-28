/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.utils;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author teras
 */
public class FileUtils {

    public static File fileIsValid(String file, String type) throws IOException {
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
        return f;
    }
}
