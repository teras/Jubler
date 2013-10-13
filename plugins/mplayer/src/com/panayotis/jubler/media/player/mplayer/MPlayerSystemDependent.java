/*
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

package com.panayotis.jubler.media.player.mplayer;

import com.panayotis.jubler.options.Options;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.os.SystemFileFinder;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author teras
 */
public class MPlayerSystemDependent extends SystemDependent {

    static void updateParameters() {
        int version = 1;
        try {
            version = Integer.parseInt(Options.getOption("System.Preferences.Version", "1"));
        } catch (NumberFormatException ex) {
        }
        String params = Options.getOption("Player.MPlayer.Arguments", "");
        if (version < 2 && (!params.equals(""))) {
            Options.setOption("System.Preferences.Version", Integer.toString(Options.CURRENT_VERSION));
            Options.setOption("Player.MPlayer.Arguments", getDefaultMPlayerArgs());
            Options.backupPrefFile();
            DEBUG.debug("Updated configuration file. backup has been taken of the old configuration file .");
        }
    }

    public static String getDefaultMPlayerArgs() {
        String font = "";

        if (IS_LINUX)
            font = " -fontconfig";
        else if (IS_WINDOWS)
            font = " -font " + System.getenv("SystemRoot") + "\\fonts\\arial.ttf";
        else {
            File freesans = new File(SystemFileFinder.AppPath + "/lib/freesans.ttf");
            if (freesans.exists())
                font = " -font %j/lib/freesans.ttf";
        }

        return "%p -noautosub -noquiet -nofs -slave -idle -ontop -utf8 "
                + "-embeddedfonts -volstep 10 -sub %s -ss %t -geometry +%x+%y "
                + "%(-audiofile %a%) -ass" + font + " %v";
    }

    public static String[] getMPlayerEnvironment(MPlayer player) {
        if (!IS_MACOSX)
            return null;
        File resource = new File(player.getOptionsPanel().getExecFileName()).getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        File fontsconf = new File(resource, "fonts/fonts.conf");
        if (!fontsconf.exists())
            return null;

        Map<String, String> variables = System.getenv();
        ArrayList<String> env = new ArrayList<String>();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            if (name.equals("FONTCONFIG_PATH"))
                return null;
            env.add(name + "=" + value);
        }
        env.add("FONTCONFIG_PATH=/usr/X11/lib/X11/fontconfig/");
        return env.toArray(new String[]{});
    }

    static String getMPlayerName() {
        if (IS_MACOSX)
            return "MPlayer OS Extended";
        return "MPlayer";
    }
}
