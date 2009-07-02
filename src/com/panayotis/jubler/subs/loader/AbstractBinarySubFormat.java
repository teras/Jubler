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
 * using a loop to call individual 
 * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor} 
 * to parse the data line that is read by the loop. The loop is surrounded by a 
 * {@link PreParseActionEvent} and a {@link PostParseActionEvent}, 
 * making it possible to re-initialise processor list
 * and post-processing the loaded subtitle records. 
 * Processing of the data line is done within the 
 * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor#parsePattern parsePattern}
 * routine.
 * <br><br>
 * A class making use of this pattern processing mechanism 
 * must create a number of processors, each deals with a single pattern.
 * The class, at startup, must initialise all processors and instances
 * of them are put into the {@link SubtitleProcessorList processorList}.
 * The processor list must know the target-record type, and whether the 
 * processors are removed once the parsing of a single line of data 
 * is completed. 
 * <br><br>
 * Since this class is the parent class, which holds the overall routine that
 * runs throught every single data-line, and calling the 
 * {@link SubtitleProcessorList} on every individual line of text, this
 * class will generate two events:
 * <ol>
 *  <li>{@link PreParseActionEvent} : This event is generated before
 *      parsing loop begins, that is before it runs through every lines
 *      of text in the subtitle file and choosing a processor to parse
 *      the text-line.</li>
 *  <li>{@link PostParseActionEvent} : This event is generated after
 *      all text-lines of the input textual data has been parsed. 
 *      This event only occurs if there subtitle records in the 
 *      list. This means that if the processing through the textual content
 *      did not produce any subtitle record, this event will not happen.</li>
 * </ol>
 * Before the loop is running, the processor list is backup, and the list
 * is restored at the end of the {@link #parse parse} routine. This is to
 * ensure that all processors that have been removed during the parsing 
 * process, when they were no longer needed, are restored, ready for the
 * next file.
 * @see SubtitleProcessorList
 * @see com.panayotis.jubler.subs.SubtitlePatternProcessor
 * @author teras & Hoang Duy Tran
 */
public abstract class AbstractBinarySubFormat extends SubFormat implements CommonDef {

    private Vector<PostParseActionEventListener> postParseEventList = new Vector<PostParseActionEventListener>();
    private Vector<PreParseActionEventListener> preParseEventList = new Vector<PreParseActionEventListener>();
    /**
     * The list of processors. This list exists when the class is created,
     * hence there is no need to create it. A clear down maybe neede if
     * unsure.
     */
    protected SubtitleProcessorList processorList = new SubtitleProcessorList();
    /**
     * The list of subtitle. This is created when the parsing commence, and
     * return to caller. It's therefore always a non-null list.
     */
    protected Subtitles subtitle_list = null;
    /**
     * The input subtitle file that is currently being loaded.
     */
    protected File subtitleFile = null;
    /**
     * The text-line that is currently being processed.
     */
    protected String textLine = null;
    /**
     * The entire textual content of the subtitle file.
     */
    protected String inputData = null;

    /**
     * Initialise the frame rate per second and the encoding scheme.
     * If the preference is not available, set default to
     * <blockquote><pre>
     *  FPS = 25f;
     *  ENCODING = "UTF-8";
     * </pre></blockquote>
     */
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
     * cannot be used without the program making sense of what data is.
     * This routine provide the mechanism for 
     * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor}s 
     * to make sense of the loaded data. Before commencement, this routine
     * expects that the local {@link SubtitleProcessorList} has been loaded
     * and that each 
     * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor} holds
     * a pattern and implemented the default routine 
     * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor#parsePattern 
     * parsePattern}.
     * This routine, by default,  breaks the data-input into a list of text
     * lines at the {@link CommonDef#single_nl new-line} character ('\n').
     * Blank lines are retained, so during the processing, the blank lines
     * can be tested. For instance the following example:
     * <blockquote><pre>
     * "1,1
     *
     * [ItemData]"
     * </pre></blockquote>
     * will produce three lines:
     * <blockquote><pre><ol>
     * <li>"1,1"</li>
     * <li>""</li>
     * <li>"[ItemData]"</li>
     * </ol></pre></blockquote>
     * This allows blocks of data to be distinguishable.
     * Each line of text is passed to a 
     * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor} 
     * in the local {@link SubtitleProcessorList}
     * and each will determine if the pattern it holds matched with the
     * data being passed in. This is done within the 
     * {@link com.panayotis.jubler.subs.SubtitlePatternProcessor#parsePattern} 
     * routine.
     * Each of the text line can either be consumed by a processor, 
     * or not being recognised by any of processors and must be throw away,
     * or it was programmatically chosen to be ignored.
     * By listening to events in the 
     * {@link SubtitleProcessorList}, the parsing of each text-line can be
     * carefully crafted to achieve a desired result.
     * @param input The textual content of the loaded file.
     * @param FPS The frame per second value for the media.
     * @param f The reference to the processing file.
     * @return A non-null list of subtitles, event when there are no subtitle
     * record found.
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
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        } finally {
            try {
                processorList.restoreList();
            } catch (Exception ex) {
            }
            return subtitle_list;
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

    /**
     * Add a post-parsing action listener.
     * @param l The listener to add.
     */
    public void addPostParseActionEventListener(PostParseActionEventListener l) {
        this.postParseEventList.add(l);
    }

    /**
     * Remove the post-parsing action listener.
     * @param l The listener to be removed.
     */
    public void removePostParseActionEventListener(PostParseActionEventListener l) {
        this.postParseEventList.remove(l);
    }

    /**
     * Clear all post-parsing action listeners.
     */
    public void clearPostParseActionEventListener() {
        this.postParseEventList.clear();
    }

    /**
     * Fire all post-parsing action listeners.
     */
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

    /**
     * Add a pre-parsing action listener.
     * @param l The listener to add.
     */
    public void addPreParseActionEventListener(PreParseActionEventListener l) {
        this.preParseEventList.add(l);
    }

    /**
     * Remove the pre-parsing action listener.
     * @param l The listener to be removed.
     */
    public void removePreParseActionEventListener(PreParseActionEventListener l) {
        this.preParseEventList.remove(l);
    }

    /**
     * Clear all pre-parsing action listeners.
     */
    public void clearPreParseActionEventListener() {
        this.preParseEventList.clear();
    }

    /**
     * Fire all pre-parsing action listeners.
     */
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

    /**
     * Gets the reference to the text-line being parsed.
     * @return Reference to the text-line being parsed.
     */
    public String getTextLine() {
        return textLine;
    }

    /**
     * Sets the reference of the text-line being parsed.
     * @param textLine Reference to the text-line being parsed.
     */
    public void setTextLine(String textLine) {
        this.textLine = textLine;
    }

    /**
     * Checking to see if the input contains the pattern that matches
     * the file's signature pattern. Also it is possible to user the
     * file reference to check for the ceontent or extension etc..
     * @param input The textual content of the file being parsed.
     * @param f The reference of the file being parsed.
     * @return true if the data contains the signature pattern
     */
    public abstract boolean isSubType(String input, File f);
    //protected abstract void parseBinary(float FPS, BufferedReader in);    
}
