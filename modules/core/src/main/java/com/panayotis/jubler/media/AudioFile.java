/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media;

import java.io.File;

public class AudioFile extends File {

    private boolean same_as_video = false;

    ;

    /** Creates a new instance of AudioFile */
    public AudioFile(File af, File vfile) {
        super(af.getPath());
        same_as_video = getPath().equals(vfile.getPath());
    }

    public AudioFile(String parent, String audiofname, VideoFile vfile) {
        super(parent, audiofname);
        same_as_video = getPath().equals(vfile.getPath());
    }

    public boolean isSameAsVideo() {
        return same_as_video;
    }
}
