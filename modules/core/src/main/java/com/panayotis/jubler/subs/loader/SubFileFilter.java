/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.plugins.Availabilities;
import java.io.File;

import static com.panayotis.jubler.i18n.I18N.__;

public class SubFileFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {

    public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        String fname = pathname.getName().toLowerCase();
        for (int i = 0; i < Availabilities.formats.size(); i++)
            if (fname.endsWith(Availabilities.formats.get(i).getExtension()))
                return true;
        return false;
    }

    public String getDescription() {
        return __("Subtitle files");
    }
}
