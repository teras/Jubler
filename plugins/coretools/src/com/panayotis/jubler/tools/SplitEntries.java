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

import java.util.ArrayList;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;
import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
public class SplitEntries extends OneByOneTool {

    public SplitEntries() {
        super(true, new ToolMenu(__("Split entries"), "TSE", Location.CONTENTTOOL, KeyEvent.VK_MINUS, InputEvent.CTRL_MASK));
    }

    @Override
    protected String getToolTitle() {
        return __("Split Entries");
    }

    @Override
    protected void affect(SubEntry sub) {
        StringTokenizer tk = new StringTokenizer(sub.getText(), "\n");
        ArrayList<String> tokens = new ArrayList<String>();
        double delta = (sub.getFinishTime().toSeconds() - sub.getStartTime().toSeconds()) / sub.getText().length();
        double from, upto;
        Subtitles newsubs = new Subtitles();

        while (tk.hasMoreTokens())
            tokens.add(tk.nextToken());
        from = sub.getStartTime().toSeconds();
        for (String subtext : tokens) {
            upto = from + delta * subtext.length();
            newsubs.add(new SubEntry(from, upto, subtext));
            from = upto + 0.001;
        }
        sub.setStartTime(newsubs.elementAt(0).getStartTime());
        sub.setFinishTime(newsubs.elementAt(0).getFinishTime());
        sub.setText(newsubs.elementAt(0).getText());
        newsubs.remove(0);
        subtitles.insertSubs(sub, newsubs);
    }
}
