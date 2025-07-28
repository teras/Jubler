/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.replace.JReplaceList;
import com.panayotis.jubler.tools.replace.ReplaceModel;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;

import static com.panayotis.jubler.i18n.I18N.__;

public class JRegExpReplace extends RealTimeTool {

    private final ArrayList<Pattern> patterns;
    private final ArrayList<String> texts;
    private final JReplaceList rlist;

    public JRegExpReplace() {
        super(false, null);
        patterns = new ArrayList<>();
        texts = new ArrayList<>();
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
