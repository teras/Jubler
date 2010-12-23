/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.ffmpeg;

import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.SystemFileFinder;
import java.io.File;

/**
 *
 * @author teras
 */
public class FFMPegSystemDependent extends SystemDependent {

    public static String findFFMpegExec() {
        File found;
        if (IS_WINDOWS)
            found = SystemFileFinder.findFile("lib/ffmpeg.exe");
        else
            found = SystemFileFinder.findFile("lib/ffmpeg");
        return (found == null ? null : found.getAbsolutePath());
    }
}
