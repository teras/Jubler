/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.filters;

import java.io.File;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Accepts any media file Jubler can preview — video or audio. Used by the media
 * selector so the user can anchor the preview on either a video or an audio-only
 * file (e.g. a plain .mp3/.wav) without first having to pick a video.
 */
public class AnyMediaFileFilter extends MediaFileFilter {

    private final String[] exts;

    public AnyMediaFileFilter() {
        String[] v = new VideoFileFilter().getExtensions();
        String[] a = new AudioFileFilter().getExtensions();
        exts = new String[v.length + a.length];
        System.arraycopy(v, 0, exts, 0, v.length);
        System.arraycopy(a, 0, exts, v.length, a.length);
    }

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
        return __("All Media files");
    }
}
