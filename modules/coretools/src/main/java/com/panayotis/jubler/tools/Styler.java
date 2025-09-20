/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.cmdline.CommandLine;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.tools.ToolMenu.Location;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

public class Styler extends OneByOneTool {

    private SubStyle style;

    public Styler() {
        super(true, new ToolMenu(__("By selection"), "ESS", Location.STYLE, 0, 0));
    }

    @Override
    public void updateData(JubFrame jub) {
        super.updateData(jub);
        StylerGUI vis = (StylerGUI) getToolVisuals();

        int selvalue = vis.StyleSel.getSelectedIndex();
        vis.StyleSel.removeAllItems();
        for (SubStyle sstyle : subtitles.getStyleList())
            vis.StyleSel.addItem(sstyle);
        if (selvalue < 0)
            selvalue = 0;
        if (selvalue < subtitles.getStyleList().size())
            vis.StyleSel.setSelectedIndex(selvalue);
    }

    @Override
    protected String getToolTitle() {
        return __("Set region style");
    }

    @Override
    protected void storeSelections() {
        style = (SubStyle) ((StylerGUI) getToolVisuals()).StyleSel.getSelectedItem();
    }

    @Override
    protected void affect(SubEntry sub) {
        sub.setStyle(style);
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new StylerGUI();
    }

    @Override
    public String getCommandOptionName() {
        return "style";
    }

    @Override
    public String getCommandLineHelp() {
        return "Apply a specific style to selected subtitle entries (style:style_name)";
    }

    @Override
    protected Collection<String> gatherSelfTags() {
        return Collections.singleton("style");
    }

    @Override
    protected String applyToolSpecificArguments(Map<String, String> args) {
        style = parseStyleParam(args, CommandLine.getSubtitles(null), "style");
        if (style == null)
            return "Unable to find style named " + args.get("style");
        return null;
    }
}
