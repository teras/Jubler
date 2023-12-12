/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.filters;

import java.io.File;

import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.media.preview.decoders.AudioPreview;

public class AudioFileFilter extends MediaFileFilter {

    private static final String exts[];
    private String cachesource = null;

    static {
        exts = new String[5];
        exts[0] = AudioPreview.getExtension();
        exts[1] = ".wav";
        exts[2] = ".mp3";
        exts[3] = ".ogg";
        exts[4] = ".ac3";
    }

    public String[] getExtensions() {
        return exts;
    }

    public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        String fname = pathname.getName().toLowerCase();
        if (cachesource != null) {
            String name = AudioPreview.getNameFromCache(pathname);
            if (name != null && name.equals(cachesource))
                return true;
            return false;
        }

        for (int i = 0; i < exts.length; i++)
            if (fname.endsWith(exts[i]))
                return true;
        return false;
    }

    public String getDescription() {
        return __("All Audio files");
    }

    public void setCheckForValidCache(File cachesource) {
        if (cachesource == null) {
            this.cachesource = null;
            return;
        }
        this.cachesource = cachesource.getName(); // trick to get the filename from the full path
    }
}
