/*
 * AbstractStyleover.java
 *
 * Created on 7 Σεπτέμβριος 2005, 12:53 μμ
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


package com.panayotis.jubler.subs.style.event;

import com.panayotis.jubler.subs.style.SubStyle.Direction;
import java.util.ArrayList;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author teras
 */
public abstract class AbstractStyleover extends ArrayList<AbstractStyleover.Entry> {
       
    protected abstract int findPrevEdge(int start, String txt);
    protected abstract int findNextEdge(int end, String txt);
    protected abstract int offsetByParagraph();
    protected abstract boolean deleteDependingOnStyle(AbstractStyleover.Entry entry, String subtext);
    
    
    
    public final static boolean PREV = false;
    public final static boolean NEXT = true;
    
    private Object styletype;
    
    
    public AbstractStyleover(Object style) {
        super();
        styletype = style;
    }
    

    public AbstractStyleover setMaxStylePosition(int pos) {
        AbstractStyleover.Entry entry;
        for (int i = size() - 1; i >=0 ; i--) {
            entry = get(i);
            if (entry.prev != null) {
                if (entry.prev.position > pos) {
                    remove(i);
                } else {
                    if (entry.next != null && entry.next.position > pos) {
                        entry.next = null;
                    }
                }
            }
        }
        if (size() == 0) return null;
        return this;
    }
    
    public void updateClone(AbstractStyleover old) {
        removeRange(0,size());
        ensureCapacity(old.size());
        for (AbstractStyleover.Entry entry : old) {
            add(new AbstractStyleover.Entry(entry));
        }
    }
    
    public Object getStyleType() { return styletype; }
    public void setStyleType(Object type) { styletype = type; }
    
    
    /** Find the event index which is just to the left of the current postion
     *  Note that the "NEXT" object is more importand than the PREV object */
    private StyleoverEvent findPrevEvent(int position, Object basic) {
        StyleoverEvent ret = new StyleoverEvent(basic,  0);
        for (AbstractStyleover.Entry entry : this) {
            /* Check if we have gone past the desired position */
            if (entry.prev.position > position) {
                break;
            }
            /* Get (by preference) the far-next event */
            if (entry.next==null) ret = entry.prev;
            else ret = entry.next;
        }
        return ret;
    }
    
    public Object getValue(int start, int end, Object basic, String subtext) {
        Object ret = basic;
        cleanupEvents(basic, subtext);
        start+=offsetByParagraph();
        if (end<start) end+=offsetByParagraph();
        for (AbstractStyleover.Entry entry : this) {
            
            // Count how many styles are inside (and only inside)
            if ( entry.prev.position > start && entry.prev.position < end) {
                return null; // At least one style found - I don't know what to return
            }
            if ( entry.prev.position >= end && entry.prev.position != 0 ) {
                break;
            }
            ret = entry.prev.value;
        }
        return ret;
    }
    
    public StyleoverEvent getVisibleEvent(int index) {
        AbstractStyleover.Entry entry = get(index);
        if (entry.next!=null) return entry.next;
        return entry.prev;
    }
    
    /** Search to the left for the next event. If it has the same value then set
     *  start to the found item event start and return.
     *  If it does not have the same value then search to the left for the real beggining
     *  of the event (same, if character based, beginning of paragraph if paragraph based)
     *  if the style should change up to the beginning of the paragraph, it will dump intermediate events */
    private StyleoverEvent makeStartEvent(Object newvalue, int start, Object basic, String txt) {
        //  if (start==txt.length()) return null;
        
        StyleoverEvent prev_style = findPrevEvent(start-1, basic);
        if (prev_style.value.equals(newvalue)) return null;
        int prev_edge = findPrevEdge(start, txt);
        return new StyleoverEvent(newvalue, (prev_edge>prev_style.position) ? prev_edge : prev_style.position);
    }
    
    
    /** Create a new style based of the current one at this position. If the previous style is
     * actually the startevent, then there is no end event (return null)
     *
     *  Or else again search to the right for the real end (same rules as findStart apply).
     *  if the style should change up to the end of the paragraph, it will dump intermediate events
     */
    private StyleoverEvent makeEndEvent(Object newvalue, int start, int end, Object basic, String txt) {
        //  if (end==txt.length()) return null;
        
        StyleoverEvent prev_style = findPrevEvent(end,  basic);
        if (prev_style.value.equals(newvalue)) return null;
        int next_edge = findNextEdge(end, txt);
        if (next_edge < 0) return null; // There is NO next _edge
        return new StyleoverEvent( prev_style.value, next_edge);
    }
    
    
    /* Here we add a new event (style) into the subtitle
     *
     *The main idea is that we read it from left to right. If some
     *events are paragaph based then the paragraph event appears ONLY in the
     *beginning of the paragraph (i.e. first character).
     *
     *In every place one could find the following possibilities:
     *1) nothing: no special style exists for this place onward
     *2) a "left" style - there is a style which will be used onwards up to
     *    the end
     *3) a "left" and a "right" - this usually means that when a user types a chracter in between,
     *   the new characters will have to "left" attribute. If though nothing will be typed, then
     *   the style is discarded and the "right" attribute is used
     */
    public void addEvent(Object event, int start, int end, Object basic, String txt) {
        if (event==null) return;
        
        cleanupEvents(basic, txt);
        
        StyleoverEvent startevent = makeStartEvent(event,  start,  basic, txt);
        StyleoverEvent endevent = makeEndEvent(event, start, end, basic, txt);
        
        deleteEvents(start, end);
        
        if (startevent!=null) {
            AbstractStyleover.Entry startentry = findEntry(startevent.position);
            startentry.prev = startevent;
            startentry.next = null;
        }
        if (endevent != null) { // We do not want hanging invisible styles (again)
            AbstractStyleover.Entry endentry = findEntry(endevent.position);
            if (endentry.prev != null) {
                endentry.next = endevent;
            } else {
                endentry.prev = endevent;
                endentry.next = null;
            }
        }
    }
    
    // Add item into PREV position (together with deleting NEXT event
    private AbstractStyleover.Entry findEntry(int pos) {
        // Find position and delete
        AbstractStyleover.Entry entry;
        for (int i = 0 ; i < size() ; i++) {
            entry = get(i);
            
            if (entry.prev.position == pos) {  // found!
                return entry;
            } else if (entry.prev.position > pos) { // We went past it, insert it here
                add(i, entry = new AbstractStyleover.Entry(null, null));   // Add entry into specific position
                return entry;
            }
        }
        add(entry = new AbstractStyleover.Entry(null, null));   // Add at the end, since it wasn't found
        return entry;
    }
    
    
    
    /** Clear all events in (start, end). Leave border items at it's place -
     *  they will be needed later on */
    private void deleteEvents(int start, int end) {
        int pos;
        AbstractStyleover.Entry entry;
        for ( int i = size() - 1 ; i >= 0 ; i--) {
            entry = get(i);
            pos = entry.prev.position;
            if (pos >= start && pos <= end ) remove(i);
        }
    }
    
    /* This method is used to insert an event, when we are already sure that it's safe enough (e.g. when loading a subtitles file */
    public void add(Object value, int start) {
        if (value==null) return;
        add(new AbstractStyleover.Entry(new StyleoverEvent(value, start), null));
    }
    
    
    public String dump() {
        StringBuffer ret = new StringBuffer();
        ret.append('{');
        for (AbstractStyleover.Entry entry : this) {
            ret.append('(').append(entry.prev.toString());
            if (entry.next!=null) ret.append('|').append(entry.next.toString());
            ret.append(')');
        }
        ret.append('}');
        return ret.toString();
    }
    
    
    public void insertText(int start, int length) {
        AbstractStyleover.Entry entry;
        int offset_start = start+offsetByParagraph();
        for( int i = 0 ; i < size() ; i++) {    // Parse all elements
            entry = get(i);
            if (entry.prev.position == start && entry.next != null) {   // A double point found
                if ( entry.prev.value.equals(entry.next.value)) {   // And it has the same values prev & next (which means that we wan to copy the rigth style to the left
                    entry.next = null;  // Deleting this should be enough
                } else {  // We need to create a new event
                    AbstractStyleover.Entry splitnext = new AbstractStyleover.Entry( new StyleoverEvent(entry.next.value, start+length), null);
                    entry.next = null;
                    i++;
                    add(i, splitnext);
                }
            } else if (entry.prev.position >= offset_start && entry.prev.position != 0 ){  // All other events are further up, so we need to shift their time (except for the zero event)
                entry.prev.position += length;
                if (entry.next != null ) entry.next.position += length;
            }
        }
    }
    
    /* NOTE: since we can not handle unknown events, this method does NOTHING to this kind of events */
    public void cleanupEvents(Object basic, String subtext) {
        if (styletype==null) return;    // Unsupported StyleType
        
        Object data = basic, olddata = null;
        AbstractStyleover.Entry entry, lastentry = null;
        /* First clean up double entries in the same position */
        for (int i = 0 ; i < size() ; i++) {
            entry = get(i);
            /* First clean up double <prev-next> events */
            if (entry.next!=null) {
                entry.prev = entry.next;
                entry.next = null;
            }
            /* Then make sure that we don't have entries in the same position */
            if (lastentry!=null && entry.prev.position == lastentry.prev.position) {
                i--;
                remove(i);
                data = olddata;
            }
            /* Then check that the previous entry has different values with this one,
             * or we should ignore this (because of the style) or it's at the end of the subtext.
             *   The reason we don't check on lastentry, is the "basic" value with is initialized */
            if (entry.prev.value.equals(data) || deleteDependingOnStyle(entry, subtext) || entry.prev.position==subtext.length()) {
                remove(i);
            } else {
                /* OK, this entry is correct */
                olddata = data; //  This entry is used for storing the real data value, if we have more entried in the same position than 1
                data = entry.prev.value;
                lastentry = entry;
            }
        }
    }
    
    
    public void removeText(int start, int length, int textlength, Object basic, String subtext) {
        AbstractStyleover.Entry entry;
        for (int i = size() -1 ; i >= 0 ; i-- ) {
            entry = get(i);
            if (entry.prev.position >= start) {
                if (entry.prev.position <= (start+length)) {
                    entry.prev.position = start;
                } else {
                    entry.prev.position -= length;
                }
                if (entry.next != null) entry.next.position = entry.prev.position;
            }
        }
        cleanupEvents(basic, subtext);
    }
    
    public void applyAttributesToDocument(StyledDocument doc, Object defaultval, int textsize) {
        applyAttributesToDocument(doc, defaultval, this, textsize);
    }
    
    
    /* NOTE: since we can not handle unknown events, this method does NOTHING to this kind of events */
    public static void applyAttributesToDocument(StyledDocument doc, Object defaultval, AbstractStyleover over, int textsize) {
        if (over.styletype==null) return;
        
        SimpleAttributeSet set;
        int from = 0;
        Object value = defaultval;
        if (over!=null) {
            for (AbstractStyleover.Entry entry : over) {
                set =  new SimpleAttributeSet();
                applyAtributesToSection(doc, over.styletype, value, from, entry.prev.position-from);
                from = entry.prev.position;
                if (entry.next == null ) value = entry.prev.value;
                else value = entry.next.value;
            }
        }
        applyAtributesToSection(doc, over.styletype, value, from, textsize-from);
    }
    
    
    /* This method is for characted based styles mostly, adapted to perform OK with alignment (paragraph based */
    private static void applyAtributesToSection(StyledDocument doc, Object type, Object value, int from, int length) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        
        if (type==StyleConstants.Alignment) {
            if ( value==Direction.TOPRIGHT || value==Direction.RIGHT || value==Direction.BOTTOMRIGHT) value = StyleConstants.ALIGN_RIGHT;
            else if ( value==Direction.TOPLEFT || value==Direction.LEFT || value==Direction.BOTTOMLEFT) value = StyleConstants.ALIGN_LEFT;
            else value = StyleConstants.ALIGN_CENTER;
            
            if (from!=0) from++;    // FIx the offset by one error with the marking of the end of the paragraph
            set.addAttribute(type, value);
            doc.setParagraphAttributes(from, length, set, false);
        } else {
            set.addAttribute(type, value);
            doc.setCharacterAttributes(from, length, set, false);
        }
    }
    
    public StyleoverEvent getEvent(int i) {
        Entry e = get(i);
        if (e.next!=null) return e.next;
        return e.prev;
    }
    
    protected class Entry {
        StyleoverEvent prev = null;
        StyleoverEvent next = null;
        
        public Entry(Entry old) {
            prev = new StyleoverEvent(old.prev);
        }
        
        public Entry(StyleoverEvent p, StyleoverEvent n) {
            prev = p;
            next = n;
        }
        
    }
}
