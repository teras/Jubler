/*
 *  StringTransformer.java
 * 
 *  Created on: 26-Sep-2009 at 03:01:07
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

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.events.WordLocatedEvent;
import com.panayotis.jubler.subs.events.WordLocatedEventListener;
import java.awt.event.ActionEvent;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * This class provides a way to transform words within a string of text. It
 * provides a mechanism to add {@link WordLocatedEventListener} to transform
 * the word within the string when it is located. The pattern defining the
 * word-boundaries must be defined in advance. The class making use the
 * function of Pattern class, compile the pattern, match the pattern against
 * the text to locate a word, and calling the instances of
 * {@link WordLocatedEventListener}s in the lister-list. Once the transformation
 * is completed, it is expected that the result is put back into the
 * {@link WordLocatedEvent}, using the {@link WordLocatedEvent#setWord setWord()}
 * method. The {@link #transformWord transformWord} method re-attach the
 * transformed word back into the original string after each transformation,
 * and start searching for the next word-boundary in the result string, starting
 * from the next position, after the last word-boundary was located. In order
 * to make sure that the last word of the string will be taken into account,
 * make sure that the end of string is considered a word-boundary. A trick
 * that one can perform is to append the input text with an instance of the
 * word-boundary pattern (ie. "." for word-boundary, as this is considered as
 * a sign of punctuation) then remove the appended instance when the
 * transformation completed.
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class StringTransformer {

    Vector<WordLocatedEventListener> listenerList = new Vector<WordLocatedEventListener>();
    /**
     * The old text to be split.
     */
    private String text = null;
    /**
     * The word-boundary pattern uses to split old-text into words.
     */
    private String pattern = null;

    /**
     * Default constructor
     */
    public StringTransformer() {
    }

    /**
     * Parameterised constructor
     * @param text The text to be split.
     */
    public StringTransformer(String text) {
        this.text = text;
    }//end public StringTransformer(String text)

    public void addWordLocatedEventListener(WordLocatedEventListener l) {
        this.listenerList.add(l);
    }

    public void removeWordLocatedEventListener(WordLocatedEventListener l) {
        this.listenerList.remove(l);
    }

    public void clearWordLocatedEventListener() {
        this.listenerList.clear();
    }

    public void fireWordLocatedEventListener(WordLocatedEvent e) {
        int len = this.listenerList.size();
        for (int i = len - 1; i >= 0; i--) {
            WordLocatedEventListener l = this.listenerList.elementAt(i);
            l.wordLocated(e);
        }//end for(int i = len-1; i >= 0; i--)
    }//end public void fireWordLocatedEventListener(WordLocatedEvent e)

    /**
     * This function splits the old-text input into words, using the
     * word-boundary pattern defined. It compiles the pattern, using
     * {@link Pattern#compile} then try to match the pattern against
     * the old-text. By default it assumes the first word starts from 0
     * and ends at the position where the pattern is first found. Where
     * the pattern ends, it is the beginning of a the next word. The
     * word located is put into an instance of {@link WordLocatedEvent},
     * and it is expected that the word will be replaced with a transformed
     * instance. The text string is re-attached with the transformed word,
     * and the search commences again from the last position where the pattern
     * was found, taken into account the length of the replaced word.
     */
    public void transformWord() {
        try {
            WordList w_list = new WordList(text, pattern);
            w_list.split();
            for (int i = 0; i < w_list.size(); i++) {
                WordRecord w_rec = w_list.elementAt(i);
                String word = w_rec.getWord();

                WordLocatedEvent wle = new WordLocatedEvent(this, ActionEvent.ACTION_PERFORMED, "Word Transform");
                wle.setWord(word);
                //System.out.println("about to transform the word: [" + word + "]");
                fireWordLocatedEventListener(wle);
                String new_word = wle.getWord();
                //System.out.println("transformed word: [" + word + "]");
                w_rec.setWord(new_word);
            }//end for(int i=0; i < w_list.size(); i++)
            this.text = w_list.restore();
        } catch (Exception ex) {
            DEBUG.logger.log(Level.WARNING, ex.toString());
        }
    }//end public void split()

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}//end public class StringTransformer

class WordList extends Vector<WordRecord> {

    private String text;
    private String pattern;

    public WordList(String text, String pattern) {
        super();
        this.text = text;
        this.pattern = pattern;
    }

    public void split() {
        try {
            String[] word_list = text.split(pattern);
            int start_from = 0;
            int word_len = 0;
            int len = word_list.length;
            for (int i = 0; i < len; i++) {
                String word = word_list[i];
                int start_index = text.indexOf(word, start_from);
                word_len = word.length();
                WordRecord w_rec = new WordRecord(word, start_index, word_len);
                add(w_rec);
                start_from += word_len;
            }//end for(int i=0; i < len; i++)
        } catch (Exception ex) {
        }
    }//end public void split()

    public String restore() {
        StringBuffer buf = new StringBuffer();
        int len = this.size();
        int non_word_start = 0;
        int non_word_end = 0;
        String non_word = "";
        for (int i = 0; i < len; i++) {
            WordRecord w_rec = elementAt(i);
            int w_start = w_rec.getStart();
            int w_len = w_rec.getLen();
            String word = w_rec.getWord();
            
            if (w_start > 0) {
                non_word_end = w_start;
                non_word = text.substring(non_word_start, non_word_end);
                buf.append(non_word);
            }//end if (w_start > 0)
            non_word_start = w_start + w_len;
            buf.append(word);
        }//end for (int i=0; i < len; i++)
        if (non_word_start < text.length()){
            non_word = text.substring(non_word_start);
            buf.append(non_word);
        }//end if (non_word_start < text.length())
        return buf.toString();
    }//end public void restore()

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}//end class WordList

class WordRecord {

    private String word;
    private int start;
    private int len;

    public WordRecord() {
    }

    public WordRecord(String word, int start, int len) {
        this.word = word;
        this.start = start;
        this.len = len;
    }//end public WordRecord(String word, int start, int len)

    /**
     * @return the word
     */
    public String getWord() {
        return word;
    }

    /**
     * @param word the word to set
     */
    public void setWord(String word) {
        this.word = word;
    }

    /**
     * @return the start
     */
    public int getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * @return the len
     */
    public int getLen() {
        return len;
    }

    /**
     * @param len the len to set
     */
    public void setLen(int len) {
        this.len = len;
    }
}//end class WordRecord
