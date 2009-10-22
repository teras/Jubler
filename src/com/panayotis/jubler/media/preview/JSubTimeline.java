/*
 * JSubTimeline.java
 *
 * Created on 21 Σεπτέμβριος 2005, 9:44 πμ
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

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.events.menu.edit.undo.UndoEntry;
import java.awt.event.MouseWheelEvent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import javax.swing.JSlider;

/**
 *
 * @author teras
 */
public class JSubTimeline extends JPanel {
    
    public static final Color BackColor = Color.BLACK;
    public static final Color SubColor = new Color(30, 240, 50);
    public static final Color SelectColor = new Color(20, 140, 255);
    public static final Color OverlapColor = new Color(230, 50, 20);
    
    private static final int NONE = 0;
    private static final int SELECT = 1;
    private static final int MOVE = 2;
    private static final int RESIZE = 4;
    
    private static final int RIGHT = 8;
    private static final int LEFT = 16;
    private static final int RESIZELEFT = RESIZE|LEFT;
    private static final int RESIZERIGHT = RESIZE|RIGHT;
    
    /* DEfine which of the two groups were used */
    private static final int SELECTED_GROUP = 32;
    private static final int VISIBLE_GROUP = 64;
    
    private static final int NOGROUP_MASK = 127 ^ (SELECTED_GROUP|VISIBLE_GROUP);
    
    /* List of actions to be able to do */
    public static final int SELECT_ACTION = SELECT;
    public static final int MOVE_ACTION = SELECT | MOVE;
    public static final int RESIZE_ACTION = SELECT | RESIZE ;
    public static final int AUTO_ACTION = SELECT | MOVE | RESIZE;
    
    
    private int current_action = AUTO_ACTION;
    
    
    private Jubler parent;
    
    private ArrayList<SubInfo> vislist;
    private ArrayList<SubInfo> sellist;
    private ArrayList<SubInfo> overlaps;
    
    /* If this flag is true, then ignore new selections (visual feedback cutter) */
    private boolean ignore_new_selection_list = false;
    
    /* Here we store the start/end/videoduration values of the window*/
    private ViewWindow view;
    
    /* This is to determine if a resize or a move event takes place */
    private int selection_mode;
    
    /* The original X position: used to determine the move offset and the stretch centerpoint */
    private int X_position;
    
    /* Whether, during mouse up, we should keep current selection */
    private boolean keep_selection_list_on_mouseup = true;
    
    /* The actual selected sub. If we'll clear the selection after mouse up, this would be the new selected sub.
     * We also use this pointer to check that a movement has been performed ( if this object is null */
    private SubInfo last_selected_subinfo;
    /* We need to know also the state of this subtitle - if it was selected before or not, in order
     * to reverse its state if no mouse dragging has been performed */
    private boolean last_subinfo_was_selected = false;
    
    /* This variable stored the central point for a resize effect */
    private double central_point;
    
    private JWavePreview wave;
    private JSubPreview preview;
    
    
    /** Creates a new instance of JSubTimeline */
    public JSubTimeline(Jubler parent, ViewWindow view, JSubPreview preview) {
        this.parent = parent;
        vislist = new ArrayList<SubInfo>();
        sellist = new ArrayList<SubInfo>();
        overlaps = new ArrayList<SubInfo>();
        this.view = view;
        this.preview = preview;
        
        addMouseMotionListener( new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                mouseStillDragging(e);
            }
            public void mouseMoved(MouseEvent e)  {
                mouseUpdateCursor(e);
            }
        });
        addMouseListener( new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mouseStartsDragging(e);
            }
            public void mouseReleased(MouseEvent e) {
                mouseStopsDragging(e);
            }
        });
        addMouseWheelListener(new MouseWheelListener() {
            public void mouseWheelMoved(MouseWheelEvent e) {
                mouseWheelUpdates(e);
            }
        });
    }
    
    public void setAction(int action) { current_action = action; }
    public void setWavePreview(JWavePreview wave) { this.wave = wave; }
    public Jubler getJubler() { return parent; }
    
    ArrayList<SubInfo> getSelectedList() {
        return sellist;
    }
    
    public void setCursor(Cursor c) {
        super.setCursor(c);
        wave.setCursor(c);
    }
    
    public void mouseUpdateCursor(MouseEvent e) {
        X_position = e.getX();
        findAction();
    }

    void mouseWheelUpdates(MouseWheelEvent e) {
        JSlider slider = parent.getSubPreview().ZoomS;
        slider.setValue(slider.getValue() + e.getWheelRotation());
    }
    
    
    private void updateCursor(int cursortype) {
        switch (cursortype&NOGROUP_MASK) {
            case NONE:
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                break;
            case RESIZELEFT:
                setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                break;
            case RESIZERIGHT:
                setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                break;
            case SELECT:
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                break;
        }
    }
    
    private int findAction() {
        int saction, vaction;
        
        saction = checkAListForAction(sellist);  // First check the selected list
        if (saction==NONE) {                    // No action found
            vaction = checkAListForAction(vislist);        // Check selected list *if* there is action there
            updateCursor(vaction) ;             // Set cursor to vaction
            return (vaction!=NONE) ? vaction|VISIBLE_GROUP : NONE;
        }
        if ( (saction&RESIZE)!=0 ) {            // Resize is very strong - if resize was found then here we are
            updateCursor(saction);
            return saction|SELECTED_GROUP;
        }
        
        SubInfo infback = last_selected_subinfo;// Store this variable in case visual list is not THAT important
        vaction = checkAListForAction(vislist);  // Check visual list what action it is there
        if ( (vaction&RESIZE)!=0 ) {            // Take into account the visual list ONLY if the selected action is Resize (which is very strong)
            updateCursor(vaction);
            return vaction|VISIBLE_GROUP;
        }
        last_selected_subinfo = infback;        // Ignore visual action - use selected action
        updateCursor(saction);
        return saction|SELECTED_GROUP;
    }
    
    /** Check if a subinfo is inside this list AND it's position is relative to the mouse
     *  cursor (so we need to take action like moving or resizing */
    private int checkAListForAction(ArrayList<SubInfo> sourcelist) {
        SubInfo inf;
        int i;
        
        /* Check if a resizing should be done */
        if ( (current_action&RESIZE) != 0 ) {
            for (i = 0 ; i < sourcelist.size() ; i++) {
                inf = sourcelist.get(i);
                /* First check if we're on the left position of a subinfo */
                if (checkForEdge(inf.start)) {
                    last_selected_subinfo = inf;
                    return RESIZELEFT;
                }
                /* Then check if we're on the right position of a subinfo */
                if (checkForEdge(inf.end)) {
                    last_selected_subinfo = inf;
                    return RESIZERIGHT;
                }
            }
        }
        /* Check if we are allowed to select here */
        if ( (current_action&SELECT) != 0) {
            for (i = 0 ; i < sourcelist.size() ; i++) {
                inf = sourcelist.get(i);
                double pos = ((double)X_position)/getWidth();
                /* At the end check if we'return in the center of a subinfo */
                if (pos >= inf.start && pos <= inf.end) {
                    last_selected_subinfo = inf;
                    return SELECT;
                }
            }
        }
        /* No - we're nowhere near. Return nothing */
        last_selected_subinfo = null;
        return NONE;
    }
    
    /* Just a plain routine to check if the clicked position is near the edge of a subinfo */
    private boolean checkForEdge(double edge) {
        int pixeledge = (int)(edge * getWidth());
        return ( X_position >= (pixeledge-1) && X_position <= (pixeledge+1));
    }
    
    
    public void mouseStartsDragging(MouseEvent e) {
        if (e.getButton()!= MouseEvent.BUTTON1) return;
        if (current_action==NONE) return;
        
        X_position = e.getX();
        
        /* The control button is used to determine if the selection will be inversed or not */
        keep_selection_list_on_mouseup = ( (e.getModifiersEx()&InputEvent.CTRL_DOWN_MASK) != 0 );
        
        /* First check that we have clicked on an already selected subinfo */
        selection_mode = findAction();
        /* If nothing was found, exit */
        if (selection_mode==NONE) return;
        
        last_subinfo_was_selected = (selection_mode & SELECTED_GROUP) > 0;  // Check if last selected sub is already selected
        
        if (!last_subinfo_was_selected) {   // Found a click on a non selected subtitle
            ignore_new_selection_list = true;
            if (!keep_selection_list_on_mouseup) sellist.clear();
            sellist.add(last_selected_subinfo);
            windowHasChanged(null);
            ignore_new_selection_list = false;
        }
        
        selection_mode=selection_mode & NOGROUP_MASK;
        /* If resizing is performed, find the central point */
        if (selection_mode==RESIZERIGHT) {
            central_point = Double.MAX_VALUE;
            for (SubInfo inf : sellist) {
                if (central_point > inf.start) central_point = inf.start;
            }
        } else {
            central_point = Double.MIN_VALUE;
            for (SubInfo inf : sellist) {
                if (central_point < inf.end) central_point = inf.end;
            }
        }
        
        calcOverlaps();
        repaint();
    }
    
    public void mouseStillDragging(MouseEvent e) {
        if (selection_mode==NONE) return;
        
        /* Here we deal this problem: when the user clicks on a subtitle we don't know yet if he
         * only wants to select it or to move/resize it also. For this reason we always think that
         * at the beginning he only wants to select it (selection_mode=SELECT). *IF* we come here,
         * it means that he wants to move/resize it.
         * So, if the selction is "select", we have to change it into something "useful" (either exit,
         * if move is not supported, or MOVE if move is supported) */
        if ( selection_mode==SELECT ) {
            /* If the current action mode is select or resize (while the selection mode is select), then no dragging should be performed */
            if (current_action==SELECT_ACTION || current_action==RESIZE_ACTION) return;
            selection_mode = MOVE;
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        
        /* Forget original selection object since a movement has been performed */
        last_selected_subinfo = null;
        
        if (selection_mode == MOVE) {
            double dt = ((double)(e.getX() - X_position))/getWidth();
            for (SubInfo inf : sellist) {
                inf.start += dt;
                inf.end += dt;
            }
            X_position = e.getX();
        } else { /* Selection mode resize */
            double factor = (e.getX() - central_point*getWidth()) / ((double)X_position-central_point*getWidth());
            double dstart = central_point*(1d-factor);
            for (SubInfo inf : sellist) {
                inf.start = inf.start*factor + dstart;
                inf.end = inf.end*factor + dstart;
                if (inf.start>inf.end) {
                    double buffer = inf.start;
                    inf.start = inf.end;
                    inf.end = buffer;
                }
            }
            X_position = e.getX();
        }
        calcOverlaps();
        repaint();
        wave.repaint();
        preview.updateSelectedTime();
    }
    
    public void mouseStopsDragging(MouseEvent e) {
        if (selection_mode==NONE) return;
        
        /** Use this method to clear the selection list - if necessary.
         *  These actions are performed ONLY when a selection has been performed and NOT
         *  when a dragging has been done (eg. resize/move).
         *  We can check this if last_selected_subinfo is null */
        if (last_selected_subinfo!=null) {
            if (keep_selection_list_on_mouseup) {
                if (last_subinfo_was_selected) sellist.remove(last_selected_subinfo);
            } else {
                sellist.clear();
                sellist.add(last_selected_subinfo);
            }
        }
        
        /* Find the actual selected subtitles for the main subtitle window */
        int sel[] = new int[sellist.size()];
        for (int i = 0 ; i < sel.length ; i++)  sel[i] = sellist.get(i).pos;
        
        /* Try to reposition the selected subtitles */
        if (last_selected_subinfo==null) {  // A move has been performed, the subtitles have been changed
            
            /* Find subtitle offset (if someone is below zero, add an offset so that everything owuld be positive) */
            double offset_time = Double.MAX_VALUE;
            double current;
            /* Find minimum value first */
            for (int i = 0 ; i < sellist.size() ; i++) {
                current = sellist.get(i).start;
                if ( current < offset_time) offset_time = current;
            }
            /* Convert gui positioning into absolute time */
            offset_time = view.getStart() + view.getDuration() * offset_time;
            /* If the time is negative, offset is added */
            if (offset_time<0) offset_time = -offset_time;
            /* or else, keep it in place */
            else offset_time = 0;
            
            SubEntry entry;
            SubInfo info;
            /* Create an undo entry */
            parent.getUndoList().addUndo(new UndoEntry(parent.getSubtitles(), "Subtitle time changes"));
            for (int i = 0 ; i < sellist.size() ; i++) {
                info = sellist.get(i);  /* get the information for this selected subtitle */
                
                entry = parent.getSubtitles().elementAt(info.pos);  /* get the actual subtitle */
                /* Set new times */
                entry.setStartTime(new Time(view.getStart() + view.getDuration() * info.start + offset_time));
                entry.setFinishTime(new Time(view.getStart() + view.getDuration() * info.end + offset_time));
            }
        }
        
        selection_mode=NONE;
        /* The visual changes will come as a callback from the Jubler event dispach of the chaneg subtitles */
        //ignore_new_selection_list = true;
        parent.fn.setSelectedSub(sel, true);
        //ignore_new_selection_list = false;
    }
    
    
    
    public Dimension getMinimumSize() {
        return new Dimension(50, 40);
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(400, 40);
    }
    
    
    /** Use this functiopn when a change has been performed in the selected subtitles.
     *  No changes to the displayed subtitles will be performed */
    private void selectionHasChanged(int[] subid) {
        if (ignore_new_selection_list) return;
        /* Keep old selections, if we don't provide new */
        if (subid==null) {
            subid = new int[sellist.size()];
            for (int i = 0 ; i < subid.length ; i++) {
                subid[i] = sellist.get(i).pos;
            }
        }
        /* Calculate new selections */
        sellist.clear();
        SubEntry entry;
        double cstart, cend;
        Subtitles subs = parent.getSubtitles();
        double vstart = view.getStart();
        double vduration = view.getDuration();
        
        int where;
        for (int i = 0 ; i < subid.length ; i++) {
            entry = subs.elementAt(subid[i]);
            cstart = entry.getStartTime().toSeconds();
            cend = entry.getFinishTime().toSeconds();
            cstart -= vstart; cstart /= vduration;
            cend -= vstart; cend /= vduration;
            sellist.add(new SubInfo(subid[i], cstart, cend));
            if ( (where = findInList(vislist, subid[i])) >= 0 ) {
                vislist.remove(where);
            }
        }
    }
    public double getSelectionStart() {
        double sel = 0;
        if (sellist.size()>0) sel = sellist.get(0).start;
        return view.getStart() + view.getDuration() * sel;
    }
    public double getSelectionEnd() {
        double sel = 0;
        if (sellist.size()>0) sel = sellist.get(sellist.size()-1).end;
        return view.getStart() + view.getDuration() * sel;
    }
    
    
    /** Use this methid to inform that the visible window has been changed AND/OR the
     *  selection has been changed */
    public void windowHasChanged(int[] subid) {
        vislist.clear();
        
        selectionHasChanged(subid);
        /* First find all elemets that are inside this time period */
        SubEntry entry;
        double cstart, cend;
        Subtitles subs = parent.getSubtitles();
        double vstart = view.getStart();
        double vduration = view.getDuration();
        
        for (int i = 0 ; i < subs.size(); i++) {
            entry = subs.elementAt(i);
            cstart = entry.getStartTime().toSeconds();
            cend = entry.getFinishTime().toSeconds();
            
            if (cstart < (vstart+vduration) && cend > vstart && findInList(sellist, i)==-1 ) {
                cstart -= vstart; cstart /= vduration;
                cend -= vstart; cend /= vduration;
                vislist.add( new SubInfo(i,  cstart,  cend));
            }
        }
        
        calcOverlaps();
        repaint();
    }
    
    private int findInList(ArrayList<SubInfo> list, int i) {
        for (int j = 0 ; j < list.size() ; j++) {
            if ( list.get(j).pos == i) return j;
        }
        return -1;
    }
    
    
    private void calcOverlaps() {
        overlaps.clear();
        double ostart, oend, cstart, cend;
        
        /* Find overlaps */
        SubInfo pointer;
        
        for (int i = 0 ; i < vislist.size() ; i++) {
            pointer = vislist.get(i);
            ostart = pointer.start;
            oend = pointer.end;
            
            /* Check for overlaps between visible list and itselft */
            for (int j = i+1 ; j < vislist.size() ; j++) {
                pointer = vislist.get(j);
                cstart = pointer.start;
                cend = pointer.end;
                if (cstart < oend && cend > ostart) {
                    overlaps.add(new SubInfo(-1,  Math.max(ostart,  cstart),  Math.min(oend, cend)));
                }
            }
            
            /* Check for overlaps between visible list and selected list */
            for (int j = 0 ; j < sellist.size() ; j++) {
                pointer = sellist.get(j);
                cstart = pointer.start;
                cend = pointer.end;
                if (cstart < oend && cend > ostart) {
                    overlaps.add(new SubInfo(-1,  Math.max(ostart,  cstart),  Math.min(oend, cend)));
                }
            }
        }
        
        /* Check for overlaps between selected list and itselft */
        for (int i = 0 ; i < sellist.size() ; i++) {
            pointer = sellist.get(i);
            ostart = pointer.start;
            oend = pointer.end;
            
            for (int j = i+1 ; j < sellist.size() ; j++) {
                pointer = sellist.get(j);
                cstart = pointer.start;
                cend = pointer.end;
                if (cstart < oend && cend > ostart) {
                    overlaps.add(new SubInfo(-1,  Math.max(ostart,  cstart),  Math.min(oend, cend)));
                }
            }
        }
    }
    
    public double getCenterOfSelection() {
        if (sellist.size()==0) {
            return view.getStart()+view.getDuration()/2d;
        }
        
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        Subtitles subs = parent.getSubtitles();
        SubEntry entry;
        double cstart, cend;
        for (int i = 0 ; i < sellist.size() ; i++ ) {
            entry = subs.elementAt(sellist.get(i).pos);
            cstart = entry.getStartTime().toSeconds();
            cend = entry.getFinishTime().toSeconds();
            if (min > cstart) min = cstart;
            if (max < cend) max = cend;
        }
        return (min+max)/2d;
    }
    
    
    public void paintComponent(Graphics g) {
        int height = getHeight();
        int width = getWidth();
        SubInfo inf;
        
        g.setColor(BackColor);
        g.fillRect(0, 0, width, height);
        
        g.setColor(SubColor);
        /* First draw unselected subs */
        for (int i = 0 ; i < vislist.size() ; i++) {
            inf = vislist.get(i);
            g.fill3DRect((int)(inf.start*width), 0, (int)((inf.end-inf.start)*width), height, true);
        }
        
        g.setColor(SelectColor);
        /* Then draw selected subs */
        for (int i = 0 ; i < sellist.size() ; i++) {
            inf = sellist.get(i);
            double sstart, send;
            if (inf.start > inf.end) {
                sstart = inf.end;
                send = inf.start;
            } else {
                sstart = inf.start;
                send = inf.end;
            }
            g.fill3DRect((int)(sstart*width), 0, (int)((send-sstart)*width), height, false);
        }
        
        /* At the end draw overlaps */
        g.setColor(OverlapColor);
        for (int i = 0 ; i < overlaps.size() ; i++) {
            inf = overlaps.get(i);
            g.fillRect((int)(inf.start*width), 0, (int)((inf.end-inf.start)*width), height);
        }
    }
    
    class SubInfo {
        int pos;
        double start, end;
        public SubInfo(int p, double s, double e) {
            pos = p;
            start = s;
            end = e;
        }
    }
    
}

