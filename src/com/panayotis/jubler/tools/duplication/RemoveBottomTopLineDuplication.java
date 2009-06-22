/*
 * RemoveBottomTopLineDuplication.java
 *
 * Created on 20-May-2009, 19:26:57
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.tools.duplication;

import static com.panayotis.jubler.i18n.I18N._;
import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.SubEntry;

/**
 * This action detects and removes the duplications that are found on the
 * bottom line of the first subtitle event's text and on the top-line
 * of a second subttle event's. An example of duplication is shown in
 * the following live-subtitled programme:
 * <pre>
 * 7
 * I love this show. 140 people.
 * ladies and gentlemen. All with the
 *
 * 8
 * ladies and gentlemen. All with the
 * same aim.
 * </pre>
 *
 * The number of lines on the duplication group limited to 2.
 * The result of the above example will be as below, after the run is
 * compledted:
 * <pre>
 * 7
 * I love this show. 140 people.
 * ladies and gentlemen. All with the
 *
 * 8
 * same aim.
 * </pre>

 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class RemoveBottomTopLineDuplication extends RemoveDuplicationBase {

    public RemoveBottomTopLineDuplication(){
        super();
        maxGroupCount = 2;
        task_name = _("Remove bottom and top-line duplication.");
    }

    public RemoveBottomTopLineDuplication(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    /**
     * Checks to see if the bottom line of the first subtitle event and
     * the top line of the second subtitle events are the same or not.
     * @param sub1 The first subtitle event to be compared.
     * @param sub2 The second subtitle event to be compared.
     * @return true if the bottom lines of sub1 and the top-line of sub2
     * are the same, case sensitive, false otherwise.
     */
    public boolean isDuplidated(SubEntry sub1, SubEntry sub2) {
        return sub1.isThisBottomTextLineDuplicatedToOtherTopLine(sub2);
    }//end public boolean isDuplidated(SubEntry sub1, SubEntry sub2)

    /**
     * Remove the top-line of the second-sub in the group, and remove it
     * only if its subtitle text is empty, otherwise keep the subtitle
     * record in the current subtitle list.
     * @param dupGroup The duplication group which have all first text line
     * duplicated.
     * @return true if the task is completed without error, false otherwise.
     */
    public boolean resolveDuplication(DuplicationGroup dupGroup) {
        try {
            SubEntry sub1 = dupGroup.elementAt(0);
            SubEntry sub2 = dupGroup.elementAt(1);

            sub2.removeTextLine(0);
            int text_line_count = sub2.getTextLineCount();
            boolean is_delete_sub2 = (text_line_count < 1);
            if (is_delete_sub2) {
                sub1.setFinishTime(sub2.getFinishTime());
                delList.add(sub2);
            }//end if (is_delete_sub2)
            return is_delete_sub2;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean resolveDuplication(SubEntry sub1, SubEntry sub2)
}//end public class RemoveTopLineDuplication extends JMenuItem implements ActionListener
