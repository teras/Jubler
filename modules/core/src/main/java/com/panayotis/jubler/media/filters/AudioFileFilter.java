/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.filters;

import java.io.File;

import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.media.preview.decoders.AudioPreviewData;

public class AudioFileFilter extends MediaFileFilter {

    private static final String exts[];
    private String cachesource = null;

    static {
        exts = new String[]{AudioPreviewData.getExtension(), ".wav", ".mp3", ".ogg", ".ac3", ".m4a", ".flac", ".aac"};
    }

    public String[] getExtensions() {
        return exts;
    }

    public boolean accept(File pathname) {
        if (pathname.isDirectory())
            return true;
        String fname = pathname.getName().toLowerCase();
        if (cachesource != null) {
            // The waveform cache is named "<media-basename>.jacache" (see
            // MediaFile.updateCacheFile). Show only the cache belonging to this
            // media, matched by that filename convention (the cache is a standard
            // WAV internally and no longer embeds the source name).
            int dot = cachesource.lastIndexOf('.');
            String base = (dot < 0 ? cachesource : cachesource.substring(0, dot)).toLowerCase();
            return fname.equals(base + AudioPreviewData.getExtension());
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
