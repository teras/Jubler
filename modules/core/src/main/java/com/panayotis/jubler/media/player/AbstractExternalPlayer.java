/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.media.player;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.options.AbstractPlayerOptions;
import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.Networking;
import com.panayotis.jubler.os.SystemFileFinder;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.subs.SubFile;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.tools.externals.AvailExternals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public abstract class AbstractExternalPlayer extends ExternalVideoPlayer implements PluginItem<AvailExternals> {

    private AbstractPlayerOptions opts;
    private String subpath;
    private int x, y;

    public AbstractExternalPlayer(String family) {
        opts = new AbstractPlayerOptions(family, this);
    }

    public abstract String getDefaultArguments();

    public abstract String[] getTestParameters();

    public String[] getEnvironment() {
        return null;
    }

    public abstract String getTestSignature();

    /* Create Subtitle File for testing*/
    private void initSubFile(Subtitles subs, MediaFile mfile) {
        try {
            SubFile sfile = new SubFile(File.createTempFile("jubler_", ""));
            Subtitles cloned_subs = new Subtitles(subs);
            FileCommunicator.save(cloned_subs, sfile, mfile);
            subpath = sfile.getSaveFile().getPath();
            return;
        } catch (IOException ex) {
        }
    }

    public void cleanUp() {
        if (subpath == null)
            return;
        File f = new File(subpath);
        if (f.exists())
            f.delete();
    }

    public static void replaceValues(String[] args, String pattern, String value) {
        int pos;
        for (int i = 0; i < args.length; i++) {
            pos = args[i].indexOf(pattern);
            if (pos >= 0) {
                args[i] = args[i].substring(0, pos) + value + args[i].substring(pos + pattern.length());
                return;
            }
        }
    }

    public PlayerArguments getCommandArguments(MediaFile mfile, Subtitles sub, Time when) {

        /* Frist, find out if we need different audio stream */
        String options = opts.getArguments();
        int begin, end;
        begin = options.indexOf("%(");
        end = options.lastIndexOf("%)");
        if (begin >= 0 && end < options.length() && begin < end)
            if (mfile.getAudioFile().isSameAsVideo())
                options = options.substring(0, begin) + options.substring(end + 2, options.length());
            else
                options = options.substring(0, begin) + options.substring(begin + 2, end) + options.substring(end + 2, options.length());
        options = options.replaceAll("%\\(", "");
        options = options.replaceAll("%\\)", "");

        /* tokenize command line */
        StringTokenizer st = new StringTokenizer(options, " ");
        String[] cmds = new String[st.countTokens()];
        initSubFile(sub, mfile);
        String buf;
        int pos = 0;

        for (int i = 0; i < cmds.length; i++) {
            buf = st.nextToken();
            if (buf != null && (!buf.equals("")))
                cmds[pos++] = buf;
        }

        replaceValues(cmds, "%p", opts.getExecFileName());
        replaceValues(cmds, "%v", mfile.getVideoFile().getPath());
        replaceValues(cmds, "%a", mfile.getAudioFile().getPath());
        replaceValues(cmds, "%s", subpath);
        replaceValues(cmds, "%t", when.getRoundSeconds());
        replaceValues(cmds, "%x", Integer.toString(x));
        replaceValues(cmds, "%y", Integer.toString(y));
        replaceValues(cmds, "%j", SystemFileFinder.AppPath.getAbsolutePath());
        int port = Networking.getRandomPort();
        replaceValues(cmds, "%i", Integer.toString(port));

        PlayerArguments a = new PlayerArguments();
        a.arguments = cmds;
        a.port = port;
        a.subfile = subpath;
        a.videofile = mfile.getVideoFile().getPath();
        a.when = when;
        a.environment = getEnvironment();
        DEBUG.debug(a.toString());
        return a;
    }

    public JExtBasicOptions getOptionsPanel() {
        return opts;
    }

    public void setCentralLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getLocationX() {
        return x;
    }

    public int getLocationY() {
        return y;
    }
    
    public void execPlugin(AvailExternals l) {
        if (l.getType().equals(family))
            l.add(this);
    }

    public abstract ArrayList<String> getSearchNames();

    public boolean isValid() {
        String path = opts.getExecFileName();
        return path != null && !path.isEmpty() && new File(path).isFile();
    }
}
