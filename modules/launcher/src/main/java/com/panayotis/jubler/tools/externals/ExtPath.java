/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.externals;

public class ExtPath {

    private String path;
    private int recursive;
    public static final int FILE_ONLY = 0;
    public static final int BUNDLE_ONLY = 50;

    /**
     * Creates a new instance of ExtPath
     */
    public ExtPath(String path, int rec) {
        this.path = path;
        recursive = rec;
    }

    public String toString() {
        return path + ":" + recursive;
    }

    public boolean searchForFile() {
        return (recursive == FILE_ONLY);
    }

    public String getPath() {
        return path;
    }

    public int getRecStatus() {
        return recursive;
    }
}
