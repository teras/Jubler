/*
 * RemoveDuplicationBase.java
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

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.undo.UndoEntry;
import static com.panayotis.jubler.i18n.I18N._;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import java.util.Vector;

/**
 * This base setout the basic routines to deal with duplications,
 * such as top line, bottom and top line, and time duplications.
 * The routine holds the list of duplication groups, which is worked out by
 * the implementation of abstract method {@link #isDuplidated}. <br><br>
 * 
 * The implementation of the abstract method 
 * {@link #resolveDuplication} takes the goup of duplicated
 * records and resolve the duplications. <br><br>
 * 
 * The local {@link #delList} holds the
 * list of records that are to be removed. This list is expected to hold
 * references of duplicated records and is used to remove from the existing
 * list at the end of the run.<br><br>
 * 
 * By default, all duplicated records are grouped into a single list of 
 * duplicated records, however, the number of records in the group may affect the
 * performance and increase the complexity for the implementation of the
 * {@link #resolveDuplication} routine, therefore the {@link #maxGroupCount}
 * is used to control the number of duplicated records to be on the duplicated 
 * group at any one time. 
 * 
 * This, however, may requires users to re-run the duplication
 * removal task more than once.
 * @author Hoang Duy Tran <hoangduytran@tiscali.co.uk>
 */
public abstract class RemoveDuplicationBase extends JMenuItem implements ActionListener {

    public static final int NO_GROUP_COUNT_LIMIT = -1;
    protected static String action_name = _("Remove Duplication");
    protected Jubler jublerParent = null;
    protected Vector<SubEntry> delList = new Vector<SubEntry>();
    protected DuplicationList dupGroupList = new DuplicationList();
    protected int maxGroupCount = NO_GROUP_COUNT_LIMIT;
    protected String task_name = _("Remove duplication.");
    public RemoveDuplicationBase() {
        setText(action_name);
        setName(action_name);
        addActionListener(this);
    }

    public RemoveDuplicationBase(Jubler jublerParent) {
        this();
        this.jublerParent = jublerParent;
    }

    private void removeDeletedRecords() {
        try {
            Subtitles subs = jublerParent.getSubtitles();
            for (int i = 0; i < this.delList.size(); i++) {
                SubEntry delEntry = delList.elementAt(i);
                subs.remove(delEntry);
            }//end for(int i=0; i < this.delList.size(); i++)
            subs.fireTableDataChanged();
        } catch (Exception ex) {
        }
    }//private void removeDeletedRecords()

    public abstract boolean isDuplidated(SubEntry sub1, SubEntry sub2);

    public abstract boolean resolveDuplication(DuplicationGroup dupGroup);

    private void groupingDuplication() {
        dupGroupList.clear();
        DuplicationGroup dupGroup = null;
        try {
            Subtitles subs = jublerParent.getSubtitles();
            for (int i = 0; i < subs.size(); i++) {
                SubEntry sub1 = subs.elementAt(i);
                SubEntry sub2 = subs.elementAt(i + 1);
                
                //making sure that all text are re-spacing with a single spaced
                //words, avoidng differences in the spaces causing identical
                //text lines to be confused as being different.
                sub1.reSpacingText();
                sub2.reSpacingText();
                
                if (isDuplidated(sub1, sub2)) {
                    boolean dup_group_is_not_initialised = (dupGroup == null);
                    boolean dup_group_size_exceed_limit =
                            (dupGroup != null) &&
                            (maxGroupCount != NO_GROUP_COUNT_LIMIT) &&
                            (dupGroup.size() == maxGroupCount);

                    boolean is_create_new_group =
                            dup_group_is_not_initialised ||
                            dup_group_size_exceed_limit;
                    if (is_create_new_group) {
                        dupGroup = new DuplicationGroup();
                        dupGroupList.add(dupGroup);
                    }//end if (is_create_new_group)

                    //Note: the list is a non-duplicated list.
                    dupGroup.add(sub1);
                    dupGroup.add(sub2);
                }else{
                    dupGroup = null;
                }//end if
            }//end for (int i = 0; i < subs.size();)
        } catch (Exception ex) {
        }
    }//end private void groupingDuplication()

    public void actionPerformed(java.awt.event.ActionEvent evt) {
        this.delList.clear();
        dupGroupList.clear();
        
        try {
            groupingDuplication();
            Subtitles subs = jublerParent.getSubtitles();
            jublerParent.getUndoList().addUndo(new UndoEntry(subs, task_name));
            int len = this.dupGroupList.size();
            for (int i = 0; i < len; i++) {
                DuplicationGroup dupGroup = dupGroupList.elementAt(i);
                resolveDuplication(dupGroup);
            }//end for (int i=0; i < subs.size(); i++)
            removeDeletedRecords();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            removeDeletedRecords();
        }//end try/catch/finally
    }//public void actionPerformed(java.awt.event.ActionEvent evt)

    /**
     * @return the jublerParent
     */
    public Jubler getJublerParent() {
        return jublerParent;
    }//public Jubler getJublerParent() 

    /**
     * @param jublerParent the jublerParent to set
     */
    public void setJublerParent(Jubler jublerParent) {
        this.jublerParent = jublerParent;
    }//public void setJublerParent(Jubler jublerParent)
}//end public class RemoveTopLineDuplication extends JMenuItem implements ActionListener
