/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.subs.SubEntry;

import com.panayotis.jubler.subs.style.SubStyle;
import com.panayotis.jubler.tools.ToolMenu.Location;

/**
 *
 * @author teras
 */
public class Styler extends OneByOneTool {

    private SubStyle style;

    public Styler() {
        super(true, new ToolMenu(_("By selection"), "ESS", Location.STYLE, 0, 0));
    }

    @Override
    public void updateData(JubFrame jub) {
        super.updateData(jub);
        StylerGUI vis = (StylerGUI) getVisuals();

        int selvalue = vis.StyleSel.getSelectedIndex();
        vis.StyleSel.removeAllItems();
        for (SubStyle sstyle : subs.getStyleList())
            vis.StyleSel.addItem(sstyle);
        if (selvalue < 0)
            selvalue = 0;
        if (selvalue < subs.getStyleList().size())
            vis.StyleSel.setSelectedIndex(selvalue);
    }

    @Override
    protected String getToolTitle() {
        return _("Set region style");
    }

    @Override
    protected void storeSelections() {
        style = (SubStyle) ((StylerGUI) getVisuals()).StyleSel.getSelectedItem();
    }

    @Override
    protected void affect(SubEntry sub) {
        sub.setStyle(style);
    }

    @Override
    protected ToolGUI constructToolVisuals() {
        return new StylerGUI();
    }
}
