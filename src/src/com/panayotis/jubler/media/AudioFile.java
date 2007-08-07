/*
 * AudioFile.java
 *
 * Created on August 5, 2007, 12:20 PM
 *
  * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

package com.panayotis.jubler.media;

import java.io.File;

/**
 *
 * @author teras
 */
public class AudioFile extends File {
    private boolean same_as_video = false;;
    
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
