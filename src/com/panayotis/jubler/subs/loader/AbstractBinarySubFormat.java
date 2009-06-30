/*
 * AbstractBinarySubFormat.java
 *
 * Created on 8 Αύγουστος 2006, 11:05 πμ
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
package com.panayotis.jubler.subs.loader;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.options.JPreferences;
import com.panayotis.jubler.subs.CommonDef;
import com.panayotis.jubler.subs.SubtitleProcessorList;
import com.panayotis.jubler.subs.Subtitles;
import com.panayotis.jubler.subs.events.PostParseActionEvent;
import com.panayotis.jubler.subs.events.PostParseActionEventListener;
import com.panayotis.jubler.subs.events.PreParseActionEvent;
import com.panayotis.jubler.subs.events.PreParseActionEventListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Vector;
import java.util.logging.Level;

/**
 * The parse routine is modified to use the list of processors and a mechanism
 * using a loop to call individual SubtitleProcessor to parse the data line
 * that is read by the loop. The loop is surrounded by a PreParseActionEvent and 
 * a PostParseActionEvent, making it possible to re-initialise processor list
 * and post-processing the loaded subtitle records. 
 * Processing of the data line is done within the 
 * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor#parsePattern parsePattern}
 * routine.
 * 
 * @see com.panayotis.jubler.subs.SubtitlePatternProcessor
 * @author teras & Hoang Duy Tran
 */
public abstract class AbstractBinarySubFormat extends SubFormat implements CommonDef {

    private Vector<PostParseActionEventListener> postParseEventList = new Vector<PostParseActionEventListener>();
    private Vector<PreParseActionEventListener> preParseEventList = new Vector<PreParseActionEventListener>();
    protected Subtitles subtitle_list = null;
    protected SubtitleProcessorList processorList = null;
    protected File subtitleFile = null;
    protected String textLine = null;
    protected String inputData = null;

    @Override
    public void init() {
        JPreferences prefs = Jubler.prefs;
        if (prefs == null) {
            FPS = 25f;
            ENCODING = "UTF-8";
        } else {
            FPS = prefs.getLoadFPS();
            ENCODING = prefs.getLoadEncodings()[0];
        }
    }

    /**
     * The parsing of the data file. The input data, though has been loaded,
     * cannot be used 
     * @param input
     * @param FPS
     * @param f
     * @return
     */
    public Subtitles parse(String input, float FPS, File f) {
        boolean is_sub_type = isSubType(input, f);
        if (!is_sub_type) {
            return null;    // Not valid - test pattern does not match
        }
        DEBUG.logger.log(Level.INFO, "Recognize file: " + getExtendedName());

        this.inputData = input;
        this.subtitleFile = f;
        this.FPS = FPS;

        subtitle_list = new Subtitles();
        try {
            processorList.backupList();
            processorList.setInputFile(f);
            processorList.setFPS(FPS);
            processorList.setInputData(input);
            processorList.setTextLineNumber(0);
            subtitle_list = new Subtitles();
            subtitleFile = f;

            firePreParseActionEvent();

            parsingData();
            
            if (subtitle_list.isEmpty()) {
                subtitle_list = null;
            } else {
                firePostParseActionEvent();
            }//end if            
            return subtitle_list;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        } finally {
            processorList.restoreList();
        }
    }

    /**
     * Default line by line parsing. This routine split the textual
     * content into lines at the single new-line ('\n') character.
     * This will gives each separacte text line on a new-line of the
     * array list, including the empty line that separate blocks.
     * Each processor in the processor list will take turns to 
     * process the text line. Extended classes can override this routine
     * to modify the behaviour of the parsing model.
     */
    protected void parsingData() {
        String input = processorList.getInputData();
        String[] text_list = input.split(single_nl);
        for (int i = 0; i < text_list.length; i++) {
            textLine = text_list[i];
            int line_no = i + 1;
            processorList.setTextLineNumber(line_no);

            textLine = textLine.trim();
            processorList.setTextLine(textLine);
            processorList.parse();
        }//end while         
    }

    public void addPostParseActionEventListener(PostParseActionEventListener l) {
        this.postParseEventList.add(l);
    }

    public void removePostParseActionEventListener(PostParseActionEventListener l) {
        this.postParseEventList.remove(l);
    }

    public void clearPostParseActionEventListener() {
        this.postParseEventList.clear();
    }

    public void firePostParseActionEvent() {
        int len = this.postParseEventList.size();
        for (int i = len - 1; i >=
                0; i--) {
            PostParseActionEvent event = new PostParseActionEvent(
                    this,
                    ActionEvent.ACTION_PERFORMED,
                    "Record Created");

            PostParseActionEventListener e = this.postParseEventList.elementAt(i);
            event.setSubtitleFile(subtitleFile);
            event.setSubtitleList(subtitle_list);
            e.postParseAction(event);
        }//end for

    }

    public void addPreParseActionEventListener(PreParseActionEventListener l) {
        this.preParseEventList.add(l);
    }

    public void removePreParseActionEventListener(PreParseActionEventListener l) {
        this.preParseEventList.remove(l);
    }

    public void clearPreParseActionEventListener() {
        this.preParseEventList.clear();
    }

    public void firePreParseActionEvent() {
        int len = this.preParseEventList.size();
        for (int i = len - 1; i >=
                0; i--) {
            PreParseActionEvent event = new PreParseActionEvent(
                    this,
                    ActionEvent.ACTION_PERFORMED,
                    "Record Created");

            PreParseActionEventListener e = this.preParseEventList.elementAt(i);
            event.setSubtitleFile(subtitleFile);
            event.setInputData(inputData);
            event.setFPS(FPS);
            e.preParseAction(event);
        }//end for

    }

    public String getTextLine() {
        return textLine;
    }

    public void setTextLine(String textLine) {
        this.textLine = textLine;
    }

    public abstract boolean isSubType(String input, File f);
    //protected abstract void parseBinary(float FPS, BufferedReader in);    
}
