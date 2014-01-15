/*
 * Copyright (C) 2014 teras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.replace.JReplaceList;
import com.panayotis.jubler.tools.replace.ReplaceModel;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 *
 * @author teras
 */
public class JRegExpReplace extends RealTimeTool {

    private final ArrayList<Pattern> patterns;
    private final ArrayList<String> texts;
    private final JReplaceList rlist;

    public JRegExpReplace() {
        super(false, null);
        patterns = new ArrayList<Pattern>();
        texts = new ArrayList<String>();
        rlist = new JReplaceList();
    }

    @Override
    protected void affect(SubEntry sub) {
        String res = sub.getText();
        for (int i = 0; i < patterns.size(); i++) {
            Matcher m = patterns.get(i).matcher(res);
            res = m.replaceAll(texts.get(i));
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
        texts.clear();
        for (int i = 0; i < model.size(); i++)
            if (model.elementAt(i).usable) {
                patterns.add(Pattern.compile(model.elementAt(i).fromS));
                texts.add(model.elementAt(i).toS);
            }
    }

    @Override
    protected JComponent constructToolVisuals() {
        return new JRegExpReplaceGUI(this);
    }

    JReplaceList getRlist() {
        return rlist;
    }

}
