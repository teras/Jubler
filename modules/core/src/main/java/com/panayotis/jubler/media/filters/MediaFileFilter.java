/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.filters;

public abstract class MediaFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    public abstract String[] getExtensions();
}
