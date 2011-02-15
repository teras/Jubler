/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import java.util.List;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class JoinEntries extends TimeBaseTool {

    public JoinEntries() {
        super(true, new ToolMenu(_("Join entries"), "TJE", Location.CONTENTTOOL, KeyEvent.VK_EQUALS, InputEvent.CTRL_MASK));
    }

    @Override
    protected String getToolTitle() {
        return _("Join Entries");
    }

    @Override
    protected boolean affect(List<SubEntry> list) {
        if (list.isEmpty())
            return true;

        SubEntry first = list.get(0);
        first.setFinishTime(list.get(list.size() - 1).getFinishTime());
        StringBuilder text = new StringBuilder(first.getText());
        for (int i = 1; i < list.size(); i++) {
            SubEntry cur = list.get(i);
            text.append('\n').append(cur.getText());
            subtitles.remove(cur);
        }
        first.setText(text.toString());
        return true;
    }
}
