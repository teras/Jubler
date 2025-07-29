/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.preview;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.undo.UndoEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import static com.panayotis.jubler.media.preview.MouseLocation.OUT;
import static com.panayotis.jubler.os.UIUtils.scale;

public class JSubTimeline extends JPanel {

    public static final Color BackColor = Color.BLACK;
    public static final Color SubColor = new Color(30, 240, 50);
    public static final Color SelectColor = new Color(20, 140, 255);
    public static final Color OverlapColor = new Color(230, 50, 20);
    private static final int interactionMargin = 4;

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
    // The correct mouse button is pressed
    private boolean isMouseDown = false;
    // The data has changed and needs to update subtitles when the mouse is released
    private boolean dataHasChanged = false;
    // The original X position: used to determine the move offset and the stretch centerpoint
    private int onMouseDownPixelPosition;
    // This variable stored the central point for a resize effect
    private double centralPoint;

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
        onMouseDownPixelPosition = e.getX();
        isMouseDown = true;

        MouseResult mouse = findAnyAction(onMouseDownPixelPosition, true);
        boolean keepSelection = ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0);
        if (mouse.location == OUT) {
            if (!keepSelection && !selectedList.isEmpty()) {
                remaininglist.addAll(selectedList);
                selectedList.clear();
            } else
                return; // Nothing to do, no selection and no action
        } else {
            if (keepSelection) {
                if (mouse.isSelected) {
                    if (mouse.location == MouseLocation.IN) {
                        selectedList.remove(mouse.subInfo);
                        remaininglist.add(mouse.subInfo);
                    }
                } else {
                    selectedList.add(mouse.subInfo);
                    remaininglist.remove(mouse.subInfo);
                }
            } else {
                if (!mouse.isSelected) {
                    remaininglist.addAll(selectedList);
                    remaininglist.remove(mouse.subInfo);
                    selectedList.clear();
                    selectedList.add(mouse.subInfo);
                }
            }

            switch (mouse.location) {
                case LEFT:
                    centralPoint = Double.MIN_VALUE;
                    for (SubInfo inf : selectedList)
                        if (centralPoint < inf.endPercent)
                            centralPoint = inf.endPercent;
                    break;
                case RIGHT:
                    centralPoint = Double.MAX_VALUE;
                    for (SubInfo inf : selectedList)
                        if (centralPoint > inf.startPercent)
                            centralPoint = inf.startPercent;
                    break;
                default:
                    centralPoint = Double.NaN;
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
        if (Double.isNaN(centralPoint)) {
            double dt = ((double) (currentPixelPosition - onMouseDownPixelPosition)) / getWidth();
            for (SubInfo inf : selectedList)
                inf.setDeltaPercent(dt);
        } else {
            double factor = (currentPixelPosition - centralPoint * getWidth()) / ((double) onMouseDownPixelPosition - centralPoint * getWidth());
            double start = centralPoint * (1d - factor);
            for (SubInfo inf : selectedList)
                inf.setProportionalDelta(factor, start);
        }
        calcOverlaps();
        repaint();
        wave.repaint();
        preview.updateSelectedTime();
    }

    public void mouseReleased(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1) return;
        findAnyAction(e.getX(), false);
        onMouseDownPixelPosition = -1;
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

    public void setAction(boolean isEdit) {
        this.isEdit = isEdit;
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
            /* Check if we're on the left position of a subinfo */
            if (checkForEdge(mousePosition, subInfo.startPercent))
                return new MouseResult(MouseLocation.LEFT, isSelected, subInfo);
            /* Check if we're on the right position of a subinfo */
            if (checkForEdge(mousePosition, subInfo.endPercent))
                return new MouseResult(MouseLocation.RIGHT, isSelected, subInfo);
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

    static class SubInfo {

        final int pos;
        double startPercent, endPercent;
        private final double initialStartPercent, initialEndPercent;

        public SubInfo(int p, double s, double e) {
            pos = p;
            startPercent = initialStartPercent = s;
            endPercent = initialEndPercent = e;
        }

        public void setDeltaPercent(double delta) {
            startPercent = initialStartPercent + delta;
            endPercent = initialEndPercent + delta;
        }

        public void setProportionalDelta(double factor, double start) {
            startPercent = initialStartPercent * factor + start;
            endPercent = initialEndPercent * factor + start;
            if (startPercent > endPercent) {
                double buffer = startPercent;
                startPercent = endPercent;
                endPercent = buffer;
            }
        }
    }
}
