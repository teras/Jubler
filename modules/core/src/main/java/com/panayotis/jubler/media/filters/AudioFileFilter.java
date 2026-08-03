/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.filters;

import java.io.File;

import static com.panayotis.jubler.i18n.I18N.__;

public class AudioFileFilter extends MediaFileFilter {

    private static final String exts[] = {".wav", ".mp3", ".ogg", ".ac3", ".m4a", ".flac", ".aac"};

    public String[] getExtensions() {
        return exts;
    }

    public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        String fname = pathname.getName().toLowerCase();
        for (int i = 0; i < exts.length; i++)
            if (fname.endsWith(exts[i]))
                return true;
        return false;
    }

    public String getDescription() {
        return __("All Audio files");
    }
}
