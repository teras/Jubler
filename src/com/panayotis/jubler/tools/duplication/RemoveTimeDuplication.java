/*
 * RemoveTimeDuplication.java
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
import java.util.Vector;

/**
 * This action detects and removes the duplications that are found on the
 * timing elements of subtitle text. An example of duplication is shown in
 * the following live-subtitled programme:
 * <pre>
 * 2
 * 00:04:41,020 --> 00:04:41,200
 * love
 *
 * 3
 * 00:04:41,020 --> 00:04:42,210
 * this
 *
 * 4
 * 00:04:41,020 --> 00:04:42,240
 * show.
 *
 * 5
 * 00:04:41,020 --> 00:04:44,000
 * 140
 *
 * 6
 * 00:04:41,020 --> 00:04:44,040
 * people,
 *
 * 7
 * 00:04:41,020 --> 00:04:44,090
 * I love this show. 140 people.
 * ladies
 * </pre>
 *
 * The number of lines on the duplication group is not limited, however, the
 * second line of text, if contains only a single word, will be assembled as
 * a single line of words which are single-space separated. The result of the
 * above example will be as below:
 *
 * <pre>
 * 2
 * 00:04:41,020 --> 00:04:44,090
 * love this show. 140 people,
 * I love this show. 140 people.
 * ladies
 * </pre>
 * 
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public class RemoveTimeDuplication extends RemoveDuplicationBase {

    public RemoveTimeDuplication(){
        super();
        task_name = _("Remove time duplication.");
    }

    public RemoveTimeDuplication(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    /**
     * Checks to see if the start-time and finish-time both subtitle events,
     * sub1 and sub2, are the same, or too close, or not.
     * @param sub1 The first subtitle event to be compared.
     * @param sub2 The second subtitle event to be compared.
     * @return True if the start-time or finish-time of the two subtitle events
     * are the same, or too close to another. False otherwise.
     */
    public boolean isDuplidated(SubEntry sub1, SubEntry sub2){
        boolean is_dup = sub1.isSameStartTime(sub2) || sub1.isSameEndTime(sub2);
        return is_dup;
    }//end public boolean isDuplidated(SubEntry sub1, SubEntry sub2)

    /**
     * Remove the duplication in the group of records by using the first record
     * in the group as the only record to hold the non-duplicated text. The
     * text on subsequent records are collected and appended to the first
     * record. If the text contains only a single word, the word is appended
     * with a single space character separation. If the text contains more
     * than one word, it is treated as a text line, and hence would be
     * appended as a new text line with a new-line character. If more than
     * one line of text is found, the whole block is appended to the first
     * record. The end-time of the first-record will be the finish-time of the
     * duplicated record, thus the at the end of the task, the first record
     * would hold the finish time of the last duplicated-record. All duplicated
     * records are inserted into the delList to be removed at the end of the
     * task.
     * @param dupGroup The duplication group which have all first text line
     * duplicated.
     * @return true if the task is completed without error, false otherwise.
     */
    public boolean resolveDuplication(DuplicationGroup dupGroup) {
        try {
            int len = dupGroup.size();
            SubEntry target = dupGroup.elementAt(0);
            for (int i=1; i < len; i++){
                SubEntry dupEntry = dupGroup.elementAt(i);
                Vector<String> text_list = dupEntry.getTextList();
                target.appendText(text_list);
                target.setFinishTime(dupEntry.getFinishTime());
                delList.add(dupEntry);
            }//end for (int i=1; i < dupGroup.size(); i++)
            return true;
        } catch (Exception ex) {
            return false;
        }
    }//end public boolean resolveDuplication(SubEntry sub1, SubEntry sub2)

}//end public class RemoveTopLineDuplication extends JMenuItem implements ActionListener
