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

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.subs.SubEntry;

import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.tools.ToolMenu.Location;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
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
}
