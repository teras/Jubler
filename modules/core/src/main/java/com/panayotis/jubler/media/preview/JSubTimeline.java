/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.os.ExcludingList;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.undo.UndoEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import static com.panayotis.jubler.media.preview.MouseLocation.*;
import static com.panayotis.jubler.os.NumericUtils.areNumbersEqual;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JSubTimeline extends JPanel {

    public static final Color BackColor = Color.BLACK;
    public static final Color SubColor = new Color(30, 240, 50);
    public static final Color SelectColor = new Color(20, 140, 255);
    public static final Color OverlapColor = new Color(230, 50, 20);

    private static final int interactionMargin = 4;
    private static final double equalityMargin = 0.0000001;

    private final JubFrame parent;
    private JWavePreview wave;
    private final JSubPreview preview;
    /* Here we store the start/end/videoduration values of the window*/
    private final ViewWindow view;

    private final List<SubInfo> remaininglist = new ArrayList<>();
    private final List<SubInfo> selectedList = new ArrayList<>();
    private final List<SubInfo> overlaps = new ArrayList<>();

    // Whether the use is allowed to edit timings or only select subtitles
    private boolean isEdit = true;
    // The current mouse action should not exceed the boundaries or an adjacent subtitle
    private boolean isSnap = true;
    // The correct mouse button is pressed
    private boolean isMouseDown = false;
    // The data has changed and needs to update subtitles when the mouse is released
    private boolean dataHasChanged = false;
    // The original X position: used to determine the move offset and the stretch centerpoint
    private int mouseDownPixels;
    private double leftSnap;
    private double rightSnap;
    private MouseResult current;

    /**
     * Creates a new instance of JSubTimeline
     */
    public JSubTimeline(JubFrame parent, ViewWindow view, JSubPreview preview) {
        this.parent = parent;
        this.view = view;
        this.preview = preview;

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                JSubTimeline.this.mouseDragged(e);
            }

            public void mouseMoved(MouseEvent e) {
                JSubTimeline.this.mouseMoved(e);
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                JSubTimeline.this.mousePressed(e);
            }

            public void mouseReleased(MouseEvent e) {
                JSubTimeline.this.mouseReleased(e);
            }
        });
        addMouseWheelListener(this::mouseWheelUpdates);
    }

    public void mouseMoved(MouseEvent e) {
        // Just update the cursor, no action is performed
        findAnyAction(e.getX(), false);
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        mouseDownPixels = e.getX();
        isMouseDown = true;
        current = findAnyAction(mouseDownPixels, true);

        boolean keepSelection = ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);
        if (current.location == OUT) {
            if (!keepSelection && !selectedList.isEmpty()) {
                remaininglist.addAll(selectedList);
                selectedList.clear();
            } else
                return; // Nothing to do, no selection and no action
        } else {
            if (keepSelection) {
                if (current.isSelected) {
                    if (current.location == MouseLocation.IN) {
                        selectedList.remove(current.subInfo);
                        remaininglist.add(current.subInfo);
                    }
                } else {
                    selectedList.add(current.subInfo);
                    remaininglist.remove(current.subInfo);
                }
            } else {
                if (!current.isSelected) {
                    remaininglist.addAll(selectedList);
                    remaininglist.remove(current.subInfo);
                    selectedList.clear();
                    selectedList.add(current.subInfo);
                }
            }
        }

        // requires proper initialized selectedList and remainingList
        leftSnap = 1;
        rightSnap = 1;
        if (isSnap) {
            if (current.location == LEFT) {
                rightSnap = current.subInfo.endPercent - current.subInfo.startPercent;
                if (current.subInfo.startPercent < leftSnap)
                    leftSnap = current.subInfo.startPercent;
                for (SubInfo other : new ExcludingList<>(current.subInfo, selectedList, remaininglist)) {
                    double leftDiff = current.subInfo.startPercent - other.endPercent;
                    if (leftDiff >= 0 && leftDiff < leftSnap)
                        leftSnap = leftDiff;
                }
            } else if (current.location == RIGHT) {
                leftSnap = current.subInfo.endPercent - current.subInfo.startPercent;
                if (1d - current.subInfo.endPercent < rightSnap)
                    rightSnap = 1d - current.subInfo.endPercent;
                for (SubInfo other : new ExcludingList<>(current.subInfo, selectedList, remaininglist)) {
                    double rightDiff = other.startPercent - current.subInfo.endPercent;
                    if (rightDiff >= 0 && rightDiff < rightSnap)
                        rightSnap = rightDiff;
                }
            } else if (current.location == IN) {
                for (SubInfo sel : selectedList) {
                    if (sel.startPercent < leftSnap)
                        leftSnap = sel.startPercent;
                    if (1d - sel.endPercent < rightSnap)
                        rightSnap = 1d - sel.endPercent;
                    for (SubInfo other : remaininglist) {
                        double leftDiff = sel.startPercent - other.endPercent;
                        if (leftDiff >= 0 && leftDiff < leftSnap)
                            leftSnap = leftDiff;
                        double rightDiff = other.startPercent - sel.endPercent;
                        if (rightDiff >= 0 && rightDiff < rightSnap)
                            rightSnap = rightDiff;
                    }
                }
            }
        }

        repaint();
        wave.repaint();
    }

    public void mouseDragged(MouseEvent e) {
        if (!isEdit) return;
        if (!isMouseDown) return;
        dataHasChanged = true;

        int currentPixelPosition = e.getX();
        double dt = ((double) (currentPixelPosition - mouseDownPixels)) / getWidth();
        dt = dt < 0 ? Math.max(dt, -leftSnap) : Math.min(dt, rightSnap);
        if (current.location == IN)
            for (SubInfo inf : selectedList) {
                inf.setDeltaStartPercent(dt);
                inf.setDeltaEndPercent(dt);
            }
        else if (current.location == LEFT)
            current.subInfo.setDeltaStartPercent(dt);
        else if (current.location == RIGHT)
            current.subInfo.setDeltaEndPercent(dt);
        calcOverlaps();
        repaint();
        wave.repaint();
        preview.updateSelectedTime();
    }

    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        findAnyAction(e.getX(), false);
        mouseDownPixels = -1;
        leftSnap = -1;
        rightSnap = -1;
        isMouseDown = false;

        /* Find the actual selected subtitles for the main subtitle window */
        int[] sel = new int[selectedList.size()];
        for (int i = 0; i < sel.length; i++)
            sel[i] = selectedList.get(i).pos;

        /* Try to reposition the selected subtitles */
        if (dataHasChanged) {  // A move has been performed, the subtitles have been changed
            SubEntry entry;
            SubInfo info;

            /* Find subtitle offset (if someone is below zero, add an offset so that everything owuld be positive) */
            double offset_time = getOffsetTime();
            /* Create an undo entry */
            parent.getUndoList().addUndo(new UndoEntry(parent.getSubtitles(), "Subtitle time changes"));
            for (SubInfo subInfo : selectedList) {
                info = subInfo;  /* get the information for this selected subtitle */

                entry = parent.getSubtitles().elementAt(info.pos);  /* get the actual subtitle */
                /* Set new times */
                entry.setStartTime(new Time(view.getStart() + view.getDuration() * info.startPercent + offset_time));
                entry.setFinishTime(new Time(view.getStart() + view.getDuration() * info.endPercent + offset_time));
            }
        }
        dataHasChanged = false;
        parent.setSelectedSub(sel, true);
    }

    public void setEdit(boolean isEdit) {
        this.isEdit = isEdit;
    }

    public void setSnap(boolean isSnap) {
        this.isSnap = isSnap;
    }

    public void setWavePreview(JWavePreview wave) {
        this.wave = wave;
    }

    public JubFrame getJubler() {
        return parent;
    }

    List<SubInfo> getSelectedList() {
        return selectedList;
    }

    public void setCursor(Cursor c) {
        super.setCursor(c);
        wave.setCursor(c);
    }

    void mouseWheelUpdates(MouseWheelEvent e) {
        JSlider slider = parent.getSubPreview().ZoomS;
        slider.setValue(slider.getValue() + e.getWheelRotation());
    }

    private MouseResult findAnyAction(int mousePosition, boolean isMouseDown) {
        MouseResult msub = findAction(selectedList, mousePosition, true);  // First check the selected list
        if (msub.location == OUT)
            msub = findAction(remaininglist, mousePosition, false); // Then check the remaining list
        msub.setCursor(this, isMouseDown, isEdit);
        return msub;
    }

    /**
     * Check if a subinfo is inside this list AND it's position is relative to
     * the mouse cursor (so we need to take action like moving or resizing
     */
    private MouseResult findAction(List<SubInfo> sourcelist, int mousePosition, boolean isSelected) {
        for (SubInfo subInfo : sourcelist) {
            /* Check if we're on the right position of a subinfo */
            if (checkForEdge(mousePosition, subInfo.endPercent)) {
                // One more check if this subtitle is zero sized, then, if it is not expandable on the right, expand it on the left no matter what.
                if (areNumbersEqual(subInfo.startPercent, subInfo.endPercent, equalityMargin))
                    for (SubInfo other : new ExcludingList<>(subInfo, selectedList, remaininglist))
                        if (areNumbersEqual(subInfo.endPercent, other.startPercent, equalityMargin))
                            return new MouseResult(MouseLocation.LEFT, isSelected, subInfo);
                return new MouseResult(MouseLocation.RIGHT, isSelected, subInfo);
            }
            /* Check if we're on the left position of a subinfo */
            if (checkForEdge(mousePosition, subInfo.startPercent))
                return new MouseResult(MouseLocation.LEFT, isSelected, subInfo);
            /* At the end check if we're in the center of a subinfo */
            double currentPercent = (double) mousePosition / getWidth();
            if (currentPercent >= subInfo.startPercent && currentPercent <= subInfo.endPercent)
                return new MouseResult(MouseLocation.IN, isSelected, subInfo);
        }
        return new MouseResult(MouseLocation.OUT, isSelected, null);
    }

    /* Just a plain routine to check if the clicked position is near the edge of a subinfo */
    private boolean checkForEdge(int mousePosition, double subTimeEdge) {
        int edgeInPixel = (int) (subTimeEdge * getWidth());
        return Math.abs(edgeInPixel - mousePosition) <= interactionMargin;
    }

    private double getOffsetTime() {
        double offset_time = Double.MAX_VALUE;
        double current;
        /* Find minimum value first */
        for (SubInfo subInfo : selectedList) {
            current = subInfo.startPercent;
            if (current < offset_time)
                offset_time = current;
        }
        /* Convert gui positioning into absolute time */
        offset_time = view.getStart() + view.getDuration() * offset_time;
        /* If the time is negative, offset is added */
        if (offset_time < 0)
            offset_time = -offset_time;
        else /* or else, keep it in place */
            offset_time = 0;
        return offset_time;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(scale(20), scale(20));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(scale(40), scale(40));
    }

    /**
     * Use this function when a change has been performed in the selected
     * subtitles. No changes to the displayed subtitles will be performed
     */
    private void selectionHasChanged(int[] subid) {
        /* Keep old selections, if we don't provide new */
        if (subid == null) {
            subid = new int[selectedList.size()];
            for (int i = 0; i < subid.length; i++)
                subid[i] = selectedList.get(i).pos;
        }

        /* Calculate new selections */
        selectedList.clear();
        SubEntry entry;
        double cstart, cend;
        Subtitles subs = parent.getSubtitles();
        double vstart = view.getStart();
        double vduration = view.getDuration();

        int where;
        for (int j : subid) {
            entry = subs.elementAt(j);
            cstart = entry.getStartTime().toSeconds();
            cend = entry.getFinishTime().toSeconds();
            cstart -= vstart;
            cstart /= vduration;
            cend -= vstart;
            cend /= vduration;
            selectedList.add(new SubInfo(j, cstart, cend));
            if ((where = findInList(remaininglist, j)) >= 0)
                remaininglist.remove(where);
        }
    }

    public double getSelectionStart() {
        double sel = 0;
        if (!selectedList.isEmpty())
            sel = selectedList.get(0).startPercent;
        return view.getStart() + view.getDuration() * sel;
    }

    public double getSelectionEnd() {
        double sel = 0;
        if (!selectedList.isEmpty())
            sel = selectedList.get(selectedList.size() - 1).endPercent;
        return view.getStart() + view.getDuration() * sel;
    }

    /**
     * Use this method to inform that the visible window has been changed AND/OR
     * the selection has been changed
     */
    public void windowHasChanged(int[] subid) {
        remaininglist.clear();

        selectionHasChanged(subid);
        /* First find all elemets that are inside this time period */
        SubEntry entry;
        double cstart, cend;
        Subtitles subs = parent.getSubtitles();
        double vstart = view.getStart();
        double vduration = view.getDuration();

        for (int i = 0; i < subs.size(); i++) {
            entry = subs.elementAt(i);
            cstart = entry.getStartTime().toSeconds();
            cend = entry.getFinishTime().toSeconds();

            if (cstart < (vstart + vduration) && cend > vstart && findInList(selectedList, i) == -1) {
                cstart -= vstart;
                cstart /= vduration;
                cend -= vstart;
                cend /= vduration;
                remaininglist.add(new SubInfo(i, cstart, cend));
            }
        }

        calcOverlaps();
        repaint();
    }

    private static int findInList(List<SubInfo> list, int i) {
        for (int j = 0; j < list.size(); j++)
            if (list.get(j).pos == i)
                return j;
        return -1;
    }

    private void calcOverlaps() {
        overlaps.clear();
        double ostart, oend, cstart, cend;

        /* Find overlaps */
        SubInfo pointer;

        for (int i = 0; i < remaininglist.size(); i++) {
            pointer = remaininglist.get(i);
            ostart = pointer.startPercent;
            oend = pointer.endPercent;

            /* Check for overlaps between visible list and itselft */
            for (int j = i + 1; j < remaininglist.size(); j++) {
                pointer = remaininglist.get(j);
                cstart = pointer.startPercent;
                cend = pointer.endPercent;
                if (cstart < oend && cend > ostart)
                    overlaps.add(new SubInfo(-1, Math.max(ostart, cstart), Math.min(oend, cend)));
            }

            /* Check for overlaps between visible list and selected list */
            for (SubInfo subInfo : selectedList) {
                pointer = subInfo;
                cstart = pointer.startPercent;
                cend = pointer.endPercent;
                if (cstart < oend && cend > ostart)
                    overlaps.add(new SubInfo(-1, Math.max(ostart, cstart), Math.min(oend, cend)));
            }
        }

        /* Check for overlaps between selected list and itselft */
        for (int i = 0; i < selectedList.size(); i++) {
            pointer = selectedList.get(i);
            ostart = pointer.startPercent;
            oend = pointer.endPercent;

            for (int j = i + 1; j < selectedList.size(); j++) {
                pointer = selectedList.get(j);
                cstart = pointer.startPercent;
                cend = pointer.endPercent;
                if (cstart < oend && cend > ostart)
                    overlaps.add(new SubInfo(-1, Math.max(ostart, cstart), Math.min(oend, cend)));
            }
        }
    }

    public double getCenterOfSelection() {
        if (selectedList.isEmpty())
            return view.getStart() + view.getDuration() / 2d;

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        Subtitles subs = parent.getSubtitles();
        SubEntry entry;
        double cstart, cend;
        for (SubInfo subInfo : selectedList) {
            entry = subs.elementAt(subInfo.pos);
            cstart = entry.getStartTime().toSeconds();
            cend = entry.getFinishTime().toSeconds();
            if (min > cstart)
                min = cstart;
            if (max < cend)
                max = cend;
        }
        return (min + max) / 2d;
    }

    public void paintComponent(Graphics g) {
        int height = getHeight();
        int width = getWidth();
        SubInfo inf;

        g.setColor(BackColor);
        g.fillRect(0, 0, width, height);

        g.setColor(SubColor);
        /* First draw unselected subs */
        for (SubInfo subInfo : remaininglist) {
            inf = subInfo;
            g.fill3DRect((int) (inf.startPercent * width), 0, (int) ((inf.endPercent - inf.startPercent) * width), height, true);
        }

        g.setColor(SelectColor);
        /* Then draw selected subs */
        for (SubInfo subInfo : selectedList) {
            inf = subInfo;
            double sstart, send;
            if (inf.startPercent > inf.endPercent) {
                sstart = inf.endPercent;
                send = inf.startPercent;
            } else {
                sstart = inf.startPercent;
                send = inf.endPercent;
            }
            g.fill3DRect((int) (sstart * width), 0, (int) ((send - sstart) * width), height, false);
        }

        /* At the end draw overlaps */
        g.setColor(OverlapColor);
        for (SubInfo overlap : overlaps) {
            inf = overlap;
            g.fillRect((int) (inf.startPercent * width), 0, (int) ((inf.endPercent - inf.startPercent) * width), height);
        }
    }
}
