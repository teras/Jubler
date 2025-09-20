/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.replace.JReplaceList;
import com.panayotis.jubler.tools.replace.ReplaceModel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;

import static com.panayotis.jubler.i18n.I18N.__;

public class JRegExpReplace extends RealTimeTool {

    private final List<Pattern> patterns;
    private final List<String> replacements;
    private final JReplaceList rlist;

    public JRegExpReplace() {
        super(false, null);
        patterns = new ArrayList<>();
        replacements = new ArrayList<>();
        rlist = new JReplaceList();
    }

    @Override
    protected void affect(SubEntry sub) {
        String res = sub.getText();
        for (int i = 0; i < patterns.size(); i++) {
            Matcher m = patterns.get(i).matcher(res);
            res = m.replaceAll(replacements.get(i));
        }
        sub.setText(res);
    }

    protected String getToolTitle() {
        return __("Regular Expression replace");
    }

    @Override
    protected void storeSelections() {
        ReplaceModel model = rlist.getModel();
        patterns.clear();
        replacements.clear();
        for (int i = 0; i < model.size(); i++)
            if (model.elementAt(i).usable) {
                patterns.add(Pattern.compile(model.elementAt(i).getPattern()));
                replacements.add(model.elementAt(i).getReplacement());
            }
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new JRegExpReplaceGUI(this);
    }

    JReplaceList getRlist() {
        return rlist;
    }

    @Override
    public String getCommandOptionName() {
        return "regex";
    }

    @Override
    public String getCommandLineHelp() {
        return "Perform regular expression-based find and replace operations on subtitle text (format: regex:pattern:replacement:flags)";
    }

    @Override
    protected Collection<String> gatherSelfTags() {
        return Arrays.asList("pattern", "replace", "esc");
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        String pattern = args.get("pattern");
        String replacement = args.get("replace");
        String esc = args.get("esc");
        if (pattern == null)
            return "Missing pattern";
        if (replacement == null)
            return "Missing replacement";
        patterns.clear();
        patterns.add(Pattern.compile(pattern));
        replacements.clear();
        replacements.add(replacement);
        return null;
    }
}
