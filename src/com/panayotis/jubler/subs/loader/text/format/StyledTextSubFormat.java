/*
 * StyledTextSubFormat.java
 *
 * Created on September 8, 2007, 1:47 PM
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

package com.panayotis.jubler.subs.loader.text.format;

import static com.panayotis.jubler.i18n.I18N._;
import static com.panayotis.jubler.subs.loader.text.format.StyledFormat.*;
import static com.panayotis.jubler.subs.style.StyleType.*;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.SubStyle.Direction;
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import com.panayotis.jubler.subs.style.event.StyleoverEvent;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author teras
 */
public abstract class StyledTextSubFormat extends AbstractTextSubFormat {
    
    /* Get the pattern that matches the style events */
    protected abstract Pattern getStylePattern();
    
    /* Get the String that matches the seperator between events at the same time position */
    protected abstract String getTokenizer();
    
    /* Get the String which introduces and finalizes new event */
    protected abstract String getEventIntro();
    protected abstract String getEventFinal();
    /* Define whether this format is compact (i.e. multiple events are under the same intro+final markers */
    protected abstract boolean isEventCompact();
    
    /* The special character that an event has - sometimes the same as getTokenizer */
    protected abstract String getEventMark();
    
    /* Get the dictionary of the supported styles */
    protected abstract Vector<StyledFormat> getStylesDictionary();
    
    /* Since ASS/SSA uses OS/2 font metrics, we need to recalculate the font size with a factor */
    protected float getFontFactor() { return 1; }
    
    @SuppressWarnings("unchecked")
    protected void parseSubText(SubEntry entry) {
        StringBuffer sbuf = new StringBuffer(entry.getText());
        Matcher m = getStylePattern().matcher(sbuf);
        ArrayList<SubEv> events = new ArrayList<SubEv>();
        SubEv s;
        int startoffset = 0 ; /* In order for not parsing the events when the text will be truncated, store the offset right here and substruct it on any new event */
        /* First parse the sub text and add all events */
        while (m.find()) {
            s = new SubEv(m.group(1), m.start(1)-1-startoffset);
            events.add(s);
            startoffset += s.value.length() + 2;
        }
        
        /* Fix the Subtitle text */
        for (SubEv se : events) {
            sbuf.delete(se.start, se.start + se.value.length()+2);
        }
        entry.setText(sbuf.toString());
        
        
        String tok; // Where to store the current event
        StringTokenizer tokens; // handler for the tokens of events
        
        /* Intialize color values */
        HashMap<StyleType, AlphaColor> cols = new HashMap<StyleType, AlphaColor>(4);
        cols.put(PRIMARY, (AlphaColor)entry.getStyle().get(PRIMARY));
        cols.put(SECONDARY, (AlphaColor)entry.getStyle().get(SECONDARY));
        cols.put(OUTLINE, (AlphaColor)entry.getStyle().get(OUTLINE));
        cols.put(SHADOW, (AlphaColor)entry.getStyle().get(SHADOW));
        AlphaColor ccol;
        
        Vector<StyledFormat> dict = getStylesDictionary();
        String tag;
        /* Go through all events */
        for (SubEv se : events) {
            tokens = new StringTokenizer(se.value,  getTokenizer());
            while (tokens.hasMoreElements()) {  // ... and subevents
                tok = tokens.nextToken();
                for (StyledFormat sf : dict) {  // Try to find the specific event
                    if (tok.startsWith(sf.tag)) {
                        tag = tok.substring(sf.tag.length());
                        try {
                            switch(sf.style.getType()) {
                                case FORMAT_INTEGRAL:
                                    int numb = (int)parseNumber(tag);
                                    if (sf.style.equals(FONTSIZE)) numb /= getFontFactor() ;  // A hack to decrease font size
                                    entry.addOverStyle(sf.style, numb, se.start);
                                    break;
                                case FORMAT_REAL:
                                    entry.addOverStyle(sf.style, sf.style.init(tag), se.start);
                                    break;
                                case FORMAT_FLAG:
                                    entry.addOverStyle(sf.style, sf.value, se.start);
                                    break;
                                case FORMAT_COLOR:
                                    ccol = cols.get(sf.style);
                                    int rgb = ccol.getRGB()&0xffffff;
                                    int alpha = ccol.getAlpha();
                                    switch((Byte)sf.value) {
                                        case COLOR_REVERSE:
                                            rgb = reverseByteOrder((int)parseNumber(tag));
                                            break;
                                        case COLOR_ALPHA_NORMAL:
                                            alpha = (int)parseNumber(tag);
                                            break;
                                        case COLOR_ALPHA_REVERSE:
                                            alpha = invertAlpha((int)parseNumber(tag));
                                            break;
                                        default:
                                            rgb = (int)parseNumber(tag);
                                    }
                                    AlphaColor newcol = new AlphaColor(rgb|(alpha<<24));
                                    cols.put(sf.style, newcol);
                                    entry.addOverStyle(sf.style, newcol, se.start);
                                    break;
                                case FORMAT_DIRECTION:
                                    entry.setOverStyle(sf.style, ((HashMap<String, Direction>)sf.value).get(tag), se.start, se.start);
                                    break;
                                default:
                                    entry.addOverStyle(sf.style, tag, se.start);
                                    
                            }
                        } catch (Exception e) {
                            String msg = _("Exception {0} while loading style {1}: {2}", e.getClass().getName(), sf.style.name(), e.getMessage());
                            DEBUG.logger.log(Level.WARNING, msg);
                        }
                        break;
                    }
                }
            }
        }
        entry.cleanupEvents();
    }
    
    
    
    @SuppressWarnings("unchecked")
    protected String rebuildSubText(SubEntry entry) {
        TreeSet<SubEv> events = new TreeSet<SubEv>();
        AbstractStyleover over;
        StyleoverEvent ev;
        
        entry.cleanupEvents();
        Vector<StyledFormat> dict = getStylesDictionary();
        /* Put all events in a sorted list */
        if (entry.overstyle != null) {  // First assure that the overstyle list is not empty
            for (StyledFormat sf : getStylesDictionary()) { // Iterate for every supported format. Note: only supported formats are saved
                over = entry.overstyle[sf.style.ordinal()]; // Get this AbstractStyleover for this format entry
                if (sf.storable && over!=null) {
                    for (int j = 0 ; j < over.size() ; j++) {   // Iterate through all events in this AbstractStyleover list
                        ev = over.getVisibleEvent(j);           // Get the value data
                        switch(sf.style.getType()) {
                            case FORMAT_FLAG:
                                if (sf.value.equals(ev.value) )
                                    events.add(new SubEv(sf.tag, ev.position));
                                break;
                            case FORMAT_DIRECTION:
                                String dirkey = getDirectionKey( (HashMap<String, Direction>)sf.value, (Direction)ev.value );
                                if (dirkey!=null)
                                    events.add(new SubEv(sf.tag+dirkey, ev.position) );
                                break;
                            case FORMAT_COLOR:
                                AlphaColor acol = (AlphaColor)ev.value;
                                String data;
                                switch((Byte)sf.value) {
                                    case COLOR_REVERSE:
                                        data = produceHexNumber(reverseByteOrder(acol.getRGB()&0xffffff), true, 6);
                                        break;
                                    case COLOR_ALPHA_NORMAL:
                                        data = produceHexNumber(acol.getAlpha(), true, 2);
                                        break;
                                    case COLOR_ALPHA_REVERSE:
                                        data = produceHexNumber(invertAlpha(acol.getAlpha()), true, 2);
                                        break;
                                    default:
                                        data = produceHexNumber(acol.getRGB()&0xffffff, true, 6);
                                }
                                events.add(new SubEv(sf.tag+data, ev.position));
                                break;
                            default:
                                String value = ev.value.toString();
                                if (sf.style.equals(StyleType.FONTSIZE))
                                    value = String.valueOf( Math.round( ((Integer)ev.value) * getFontFactor() ) ) ;  // A hack to increase font size
                                events.add(new SubEv(sf.tag+value, ev.position));
                        }
                    }
                }
            }
        }
        
        StringBuffer btxt = new StringBuffer(entry.getText());
        for (SubEv cevent : events) {
            if ( (!isEventCompact()) || (! btxt.substring(cevent.start).startsWith(getEventIntro())) )
                btxt.insert(cevent.start, getEventIntro()+getEventFinal());
            btxt.insert(cevent.start+getEventIntro().length(), getEventMark()+cevent.value);
        }
        return btxt.toString();
    }
    
    
    
    protected int invertAlpha(int alpha) {
        return 0xff-(alpha&0xff);
    }
    protected int reverseByteOrder(int old) {
        return ((old&0xff0000)>>16) | (old&0xff00) | ((old&0xff)<<16);
    }
    protected long parseNumber(String value) {
        long ret = 0;
        value = value.toLowerCase();
        try {
            if (value.startsWith("&h")) {
                value = value.substring(2);
                if (value.endsWith("&"))
                    value = value.substring(0, value.length()-1);
                ret = Long.parseLong(value, 16);
            } else {
                ret = Long.parseLong(value);
            }
        } catch ( NumberFormatException e) {}
        return ret;
    }
    
    protected String produceHexNumber(long number, boolean trailing_and, int length) {
        String n = Long.toHexString(number).toUpperCase();
        n = zeros.substring(0, length-n.length()) + n;
        return "&H"+n+(trailing_and ? "&" : "");
    }
    private final static String zeros = "0000000000000000";
    
    
    protected static final String getDirectionKey(HashMap<String, Direction> dict, Direction dir) {
        if (dir==null) dir = Direction.BOTTOM;
        for (Entry<String, Direction> en : dict.entrySet()) {
            if (en.getValue().equals(dir) ) {
                return en.getKey();
            }
        }
        return "";
    }
    
}
