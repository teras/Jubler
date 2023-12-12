/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.spell.checkers;

import com.panayotis.jubler.options.ASpellOptions;
import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;
import com.panayotis.jubler.tools.externals.AvailExternals;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellError;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;

import static com.panayotis.jubler.tools.spell.checkers.ASpell.ASpellSystemDependent.forceutf8;

public class ASpell extends SpellChecker implements PluginCollection, PluginItem<AvailExternals> {

    BufferedWriter send;
    BufferedReader get;
    ASpellOptions opts;
    Process proc;

    /**
     * Creates a new instance of ASpell
     */
    public ASpell() {
        opts = new ASpellOptions(family, getName());
    }

    public void start() throws ExtProgramException {
        try {
            ArrayList<String> cmd = new ArrayList<String>();
            cmd.add(opts.getExecFileName());
            if (forceutf8)
                cmd.add("--encoding=utf-8");

            ASpellOptions.ASpellDict lang = opts.getLanguage();
            if (lang != null) {
                if (lang.path != null)
                    cmd.add("--dict-dir=" + lang.path);
                cmd.add("-d");
                cmd.add(lang.lang);
            }
            cmd.add("pipe");

            String[] c = cmd.toArray(new String[1]);
            proc = Runtime.getRuntime().exec(c);
            DEBUG.debug(DEBUG.toString(c));

            if (forceutf8) {
                send = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream(), "UTF-8"));
                get = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
            } else {
                send = new BufferedWriter(new OutputStreamWriter(proc.getOutputStream()));
                get = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            }

            get.readLine();
            /* Read aspell information */
            send.write("!\n");  /* Enter terse mode */
        } catch (IOException e) {
            throw new ExtProgramException(e);
        }
    }

    public void stop() {
        if (proc != null)
            proc.destroy();
        proc = null;
    }

    public boolean insertWord(String word) {
        if (proc != null)
            try {
                send.write('*' + word + "\n#\n");
                send.flush();
                return true;
            } catch (IOException e) {
            }
        return false;
    }

    public boolean supportsInsert() {
        return true;
    }

    @SuppressWarnings("UseOfObsoleteCollectionType")
    public ArrayList<SpellError> checkSpelling(String text) {
        ArrayList<SpellError> ret = new ArrayList<SpellError>();
        String input;

        String orig;
        int pos;
        java.util.Vector<String> sug;

        try {
            send.write('^' + text.replace('\n', '|').replace('\r', '|') + '\n');
            send.flush();

            while (!(input = get.readLine()).equals("")) {
                StringTokenizer token = new StringTokenizer(input, " \t\r\n:,");
                String part = token.nextToken();
                if (part.equals("&")) {
                    sug = new java.util.Vector<String>();
                    orig = token.nextToken();
                    token.nextToken();
                    pos = Integer.parseInt(token.nextToken()) - 1;
                    while (token.hasMoreTokens())
                        sug.add(token.nextToken());
                    ret.add(new SpellError(pos, orig, sug));
                } else if (part.equals("#")) {
                    sug = new java.util.Vector<String>();
                    orig = token.nextToken();
                    pos = Integer.parseInt(token.nextToken()) - 1;
                    ret.add(new SpellError(pos, orig, sug));
                }
            }
            return ret;
        } catch (IOException e) {
        }
        return ret;
    }

    public JExtBasicOptions getOptionsPanel() {
        return opts;
    }

    public final String getName() {
        return "ASpell";
    }

    public void execPlugin(AvailExternals l) {
        if (l.getType().equals(family))
            l.add(this);
    }

    public Collection<PluginItem<?>> getPluginItems() {
        return Collections.singleton(this);
    }

    public String getCollectionName() {
        return "ASpell checker";
    }

    static class ASpellSystemDependent extends SystemDependent {

        /**
         * Force ASpell to use UTF-8 encoding - broken on Windows
         */
        static final boolean forceutf8 = !IS_WINDOWS;
    }
}
