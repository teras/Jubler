/*
 *  SubtitleUpdaterThread.java 
 * 
 *  Created on: 24-Jun-2009 at 10:26:43
 * 
 *  
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
package com.panayotis.jubler.subs;

import com.panayotis.jubler.subs.events.SubtitleRecordUpdatedEvent;
import com.panayotis.jubler.subs.events.SubtitleRecordUpdatedEventListener;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPostProcessingEvent;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPostProcessingEventListener;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPreProcessingEvent;
import com.panayotis.jubler.subs.events.SubtitleUpdaterPreProcessingEventListener;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Vector;

/**
 * This class is base class for all subtitle updating events that are
 * threaded. It holds the list and routines to fire events before and after
 * the processing are done, and after a subtitle-record has been updated.
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class SubtitleUpdaterThread extends Thread {

    private Subtitles subList = null;
    private SubEntry entry = null;
    private int row = -1;
    private Vector<SubtitleRecordUpdatedEventListener> recordUpdatedEventList = new Vector<SubtitleRecordUpdatedEventListener>();
    private Vector<SubtitleUpdaterPreProcessingEventListener> updaterPreProcessingEventList = new Vector<SubtitleUpdaterPreProcessingEventListener>();
    private Vector<SubtitleUpdaterPostProcessingEventListener> updaterPostProcessingEventList = new Vector<SubtitleUpdaterPostProcessingEventListener>();


    public void addSubtitleUpdaterPreProcessingEventListener(
            Collection<SubtitleUpdaterPreProcessingEventListener> cl) {
        this.updaterPreProcessingEventList.addAll(cl);
    }
    
    public void addSubtitleUpdaterPreProcessingEventListener(SubtitleUpdaterPreProcessingEventListener l) {
        this.updaterPreProcessingEventList.add(l);
    }

    public void removeSubtitleUpdaterPreProcessingEventListener(SubtitleUpdaterPreProcessingEventListener l) {
        this.updaterPreProcessingEventList.remove(l);
    }

    public void clearSubtitleUpdaterPreProcessingEventListener() {
        this.updaterPreProcessingEventList.clear();
    }

    public void fireSubtitleUpdaterPreProcessingEvent() {
        SubtitleUpdaterPreProcessingEvent event = new SubtitleUpdaterPreProcessingEvent(
                this,
                ActionEvent.ACTION_PERFORMED,
                "Subtitle Updater Pre-Processing Event");
        event.setSubList(subList);
        int len = this.updaterPreProcessingEventList.size();
        for (int i = len - 1; i >=
                0; i--) {
            SubtitleUpdaterPreProcessingEventListener e = this.updaterPreProcessingEventList.elementAt(i);
            e.preProcessing(event);
        }//end for
    }

    public void addSubtitleRecordUpdatedEventListener(
            Collection<SubtitleRecordUpdatedEventListener> cl) {
        this.recordUpdatedEventList.addAll(cl);
    }
    
    public void addSubtitleRecordUpdatedEventListener(SubtitleRecordUpdatedEventListener l) {
        this.recordUpdatedEventList.add(l);
    }

    public void removeSubtitleRecordUpdatedEventListener(SubtitleRecordUpdatedEventListener l) {
        this.recordUpdatedEventList.remove(l);
    }

    public void clearSubtitleRecordUpdatedEventListener() {
        this.recordUpdatedEventList.clear();
    }

    public void fireSubtitleRecordUpdatedEvent() {
        SubtitleRecordUpdatedEvent event = new SubtitleRecordUpdatedEvent(
                this,
                ActionEvent.ACTION_PERFORMED,
                "Subtitle Updated");
        int len = this.recordUpdatedEventList.size();
        for (int i = len - 1; i >=
                0; i--) {
            SubtitleRecordUpdatedEventListener e = this.recordUpdatedEventList.elementAt(i);
            event.setRow(row);
            event.setSubEntry(entry);
            event.setSubList(subList);
            e.recordUpdated(event);
        }//end for
    }
     
    public void addSubtitleUpdaterPostProcessingEventListener(
            Collection<SubtitleUpdaterPostProcessingEventListener> cl) {
        this.updaterPostProcessingEventList.addAll(cl);
    }
    
    public void addSubtitleUpdaterPostProcessingEventListener(SubtitleUpdaterPostProcessingEventListener l) {
        this.updaterPostProcessingEventList.add(l);
    }

    public void removeSubtitleUpdaterPostProcessingEventListener(SubtitleUpdaterPostProcessingEventListener l) {
        this.updaterPostProcessingEventList.remove(l);
    }

    public void clearSubtitleUpdaterPostProcessingEventListener() {
        this.updaterPostProcessingEventList.clear();
    }

    public void fireSubtitleUpdaterPostProcessingEvent() {
        SubtitleUpdaterPostProcessingEvent event = new SubtitleUpdaterPostProcessingEvent(
                this,
                ActionEvent.ACTION_PERFORMED,
                "Subtitle Updater Post-Processing Event");
        event.setSubList(subList);
        int len = this.updaterPostProcessingEventList.size();
        for (int i = len - 1; i >=
                0; i--) {
            SubtitleUpdaterPostProcessingEventListener e = this.updaterPostProcessingEventList.elementAt(i);            
            e.postProcessing(event);
        }//end for
    }  
    
    public Subtitles getSubList() {
        return subList;
    }

    public void setSubList(Subtitles subList) {
        this.subList = subList;
    }

    public SubEntry getEntry() {
        return entry;
    }

    public void setEntry(SubEntry entry) {
        this.entry = entry;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public Vector<SubtitleRecordUpdatedEventListener> getRecordUpdatedEventList() {
        return recordUpdatedEventList;
    }

    public Vector<SubtitleUpdaterPreProcessingEventListener> getUpdaterPreProcessingEventList() {
        return updaterPreProcessingEventList;
    }

    public Vector<SubtitleUpdaterPostProcessingEventListener> getUpdaterPostProcessingEventList() {
        return updaterPostProcessingEventList;
    }
}
