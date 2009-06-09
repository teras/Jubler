/*
 * SubtitleProcessorList.java
 *
 * Created on 04-Dec-2008, 00:47:34
 * 
 * This file is part of Jubler.
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
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
package com.panayotis.jubler.subs;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEvent;
import com.panayotis.jubler.subs.events.SubtitleRecordCreatedEventListener;
import com.panayotis.jubler.subs.events.ParsedDataLineEvent;
import com.panayotis.jubler.subs.events.ParsedDataLineEventListener;
import com.panayotis.jubler.subs.events.PreParsingDataLineActionEvent;
import com.panayotis.jubler.subs.events.PreParsingDataLineActionEventListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;

/**
 *
 * This class is used to hold the list of {@link SubtitlePatternProcessor}s,
 * each will be in charge of a recognising and parsing a data line. 
 * There are a groups of action events to be fired:
 * <ul>
 * <li>{@link PreParsingDataLineActionEvent}</li> Occurs before the data line is parsed.
 * It is possible to set flag to ignore the data line using 
 * {@link #setIgnoreData(boolean)}
 * <li>{@link SubtitleRecordCreatedEvent}</li> Occurs before the data line is parsed and
 * when either {@link #isCreateNewObject()}flag is set or the reference to the
 * target object of the {@link SubtitlePatternProcessor} is "null". Initialisation
 * to the created object or changing the state of the 
 * {@link SubtitlePatternProcessor}s are possible using this event. Record 
 * creation is possible using the name of the target class, therefore the
 * {@link SubtitlePatternProcessor}'s getTargetObjectClassName() is used. 
 * As this uses the Class.forname().newInstance(), the default constructor is
 * used and no parameterised constructors are recognised.
 * <li>{@link ParsedDataLineEvent}</li> Occurs after the data line has been parsed by a
 * SubtitlePatternProcessor. Resetting of the {@link #isCreateNewObject()} flag
 * is possible within this event to commence the creation of a new record on the
 * next parsing turn.
 * </ul>
 * The {@link #parse} routine also removes the {@link SubtitlePatternProcessor}
 * from the list after it has completed parsing the data line it's set to
 * removable. This is to avoid the unnecessary testing of pattern in the next
 * run, when the pattern is known to be occurred only once. Voluntary removal 
 * of processors is possible within processing events, for instance after a
 * certain point of processing occured. Instance of 
 * {@link SubtitlePatternProcessor} must have access to the 
 * SubtitleProcessorList in order to do this.
 * The {@link #parse()} routine terminates after an instance of 
 * {@link SubtitlePatternProcessor} processed its data. 
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SubtitleProcessorList extends Vector<SubtitlePatternProcessor> {

    private Vector<SubtitlePatternProcessor> patternProcessorListCopy = new Vector<SubtitlePatternProcessor>();
    private Vector<SubtitleRecordCreatedEventListener> recordCreatedEventList = new Vector<SubtitleRecordCreatedEventListener>();
    private Vector<ParsedDataLineEventListener> parsedEventList = new Vector<ParsedDataLineEventListener>();
    private Vector<PreParsingDataLineActionEventListener> preParsingEventList = new Vector<PreParsingDataLineActionEventListener>();
    private String inputData = null;
    private String textLine = null;
    private int textLineNumber = 0;
    private float FPS = 25f;
    private File inputFile = null;
    private boolean createNewObject = false;
    private Object createdRecord = null;
    private boolean ignoreData = false;    
    private SubtitlePatternProcessor currentProcessor = null;
    private boolean isFound = false;

    public SubtitleProcessorList() {
        super();
    }

    public SubtitleProcessorList(SubtitlePatternProcessor[] processor_list) {
        boolean valid = (processor_list != null && processor_list.length > 0);
        for (int i = 0; valid && i < processor_list.length; i++) {
            this.add(processor_list[i]);
        }//end for(int i=0; valid && i < processor_list.length; i++)
    }

    public SubtitleProcessorList(SubtitlePatternProcessor processor, String input_data, float FPS, File input_file) {
        this.add(processor);
        setInputData(input_data);
        setFPS(FPS);
        setInputFile(input_file);
    }

    public SubtitleProcessorList(SubtitlePatternProcessor processor) {
        this.add(processor);
    }

    public void addSubtitleRecordCreatedEventListener(SubtitleRecordCreatedEventListener l) {
        this.recordCreatedEventList.add(l);
    }

    public void removeSubtitleRecordCreatedEventListener(SubtitleRecordCreatedEventListener l) {
        this.recordCreatedEventList.remove(l);
    }

    public void clearSubtitleRecordCreatedEventListener() {
        this.recordCreatedEventList.clear();
    }

    public void fireSubtitleRecordCreatedEvent() {
        int len = this.recordCreatedEventList.size();
        for (int i = len - 1; i >= 0; i--) {
            SubtitleRecordCreatedEvent event = new SubtitleRecordCreatedEvent(
                    this,
                    ActionEvent.ACTION_PERFORMED,
                    "Record Created");

            SubtitleRecordCreatedEventListener e = this.recordCreatedEventList.elementAt(i);
            event.setCreatedObject(createdRecord);
            e.recordCreated(event);
        }//end for
    }

    public void addSubtitleDataParsedEventListener(ParsedDataLineEventListener l) {
        this.parsedEventList.add(l);
    }

    public void removeSubtitleDataParsedEventListener(ParsedDataLineEventListener l) {
        this.parsedEventList.remove(l);
    }

    public void clearSubtitleDataParsedEventListener() {
        this.parsedEventList.clear();
    }

    public void fireSubtitleDataParsedEvent() {
        int len = this.parsedEventList.size();
        for (int i = len - 1; i >= 0; i--) {
            ParsedDataLineEvent event = new ParsedDataLineEvent(
                    this,
                    ActionEvent.ACTION_PERFORMED,
                    "Parsed Data Line");

            ParsedDataLineEventListener e = this.parsedEventList.elementAt(i);
            event.setProcessor(getCurrentProcessor());
            e.dataLineParsed(event);
        }//end for
    }

    public void addSubtitleDataPreParsingEventListener(PreParsingDataLineActionEventListener l) {
        this.preParsingEventList.add(l);
    }

    public void removeSubtitleDataPreParsingEventListener(PreParsingDataLineActionEventListener l) {
        this.preParsingEventList.remove(l);
    }

    public void clearSubtitleDataPreParsingEventListener() {
        this.preParsingEventList.clear();
    }

    public void fireSubtitleDataPreParsingEvent() {
        int len = this.preParsingEventList.size();
        for (int i = len - 1; i >= 0; i--) {
            PreParsingDataLineActionEvent event = new PreParsingDataLineActionEvent(
                    this,
                    ActionEvent.ACTION_PERFORMED,
                    "Pre-Parsing Data Line");

            PreParsingDataLineActionEventListener e = this.preParsingEventList.elementAt(i);
            event.setProcessor(getCurrentProcessor());
            e.preParsingDataLineAction(event);
        }//end for
    }

    public void backupList() {
        this.patternProcessorListCopy.clear();
        this.patternProcessorListCopy.addAll(this);
    }

    public void restoreList() {
        this.clear();
        this.addAll(patternProcessorListCopy);
    }

    public void setAllTargetObjectClassNameAndRemovable(Collection<SubtitlePatternProcessor> processorCollection, String name, boolean removable) {
        this.setAllTargetObjectClassName(processorCollection, name);
        this.setAllTargetObjectRemovable(processorCollection, removable);
    }

    public void setAllTargetObjectClassName(Collection<SubtitlePatternProcessor> processorCollection, String className) {
        boolean valid = !(processorCollection == null || processorCollection.size() == 0);
        if (valid) {
            Iterator<SubtitlePatternProcessor> it = processorCollection.iterator();
            while (it.hasNext()) {
                SubtitlePatternProcessor ps = it.next();
                ps.setTargetObjectClassName(className);
            }//end while(it.hasNext())
        }//end if (valid)        
    }

    public void setAllTargetObjectRemovable(Collection<SubtitlePatternProcessor> processorCollection, boolean removable) {
        boolean valid = !(processorCollection == null || processorCollection.size() == 0);
        if (valid) {
            Iterator<SubtitlePatternProcessor> it = processorCollection.iterator();
            while (it.hasNext()) {
                SubtitlePatternProcessor ps = it.next();
                ps.setRemovable(removable);
            }//end while(it.hasNext())
        }//end if (valid)
    }

    public void setAllTargetObjectClassNameAndRemovable(SubtitlePatternProcessor[] processorCollection, String name, boolean removable) {
        setAllTargetObjectRemovable(processorCollection, removable);
        setAllTargetObjectClassName(processorCollection, name);
    }

    public void setAllTargetObjectRemovable(SubtitlePatternProcessor[] processorCollection, boolean removable) {
        boolean valid = (processorCollection != null && processorCollection.length > 0);
        for (int i = 0; valid && i < processorCollection.length; i++) {
            SubtitlePatternProcessor ps = processorCollection[i];
            ps.setRemovable(removable);
        }//end for (int i=0; valid && i < processorCollection.length; i++)                
    }

    public void setAllTargetObjectClassName(SubtitlePatternProcessor[] processorCollection, String name) {
        boolean valid = (processorCollection != null && processorCollection.length > 0);
        for (int i = 0; valid && i < processorCollection.length; i++) {
            SubtitlePatternProcessor ps = processorCollection[i];
            ps.setTargetObjectClassName(name);
        }//end for (int i=0; valid && i < processorCollection.length; i++)        
    }

    public void setAllTargetObjectClassName(String name) {
        Enumeration<SubtitlePatternProcessor> en = this.elements();
        while (en.hasMoreElements()) {
            SubtitlePatternProcessor ps = en.nextElement();
            ps.setTargetObjectClassName(name);
        }//end while(en.hasMoreElements())        
    }

    public void setAllTargetObject(SubtitlePatternProcessor[] processorCollection, Object target_object) {
        boolean valid = (processorCollection != null && processorCollection.length > 0);
        for (int i = 0; valid && i < processorCollection.length; i++) {
            SubtitlePatternProcessor ps = processorCollection[i];
            ps.setTargetObject(target_object);
        }//end for (int i=0; valid && i < processorCollection.length; i++)
    }

    public void setAllTargetObject(Collection<SubtitlePatternProcessor> processorCollection, Object target_object) {
        Iterator<SubtitlePatternProcessor> it = processorCollection.iterator();
        while (it.hasNext()) {
            SubtitlePatternProcessor ps = it.next();
            ps.setTargetObject(target_object);
        }//end while(it.hasNext())
    }

    public void setAllTargetObject(Object target_object) {
        Enumeration<SubtitlePatternProcessor> en = this.elements();
        while (en.hasMoreElements()) {
            SubtitlePatternProcessor ps = en.nextElement();
            ps.setTargetObject(target_object);
        }//end while(en.hasMoreElements())
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public float getFPS() {
        return FPS;
    }

    public void setFPS(float FPS) {
        this.FPS = FPS;
    }

    public File getInputFile() {
        return inputFile;
    }

    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    public boolean isCreateNewObject() {
        return createNewObject;
    }

    public void setCreateNewObject(boolean createNewObject) {
        this.createNewObject = createNewObject;
    }

    public Object getCreatedRecord() {
        return createdRecord;
    }

    public void setCreatedRecord(Object createdRecord) {
        this.createdRecord = createdRecord;
    }

    public boolean isIgnoreData() {
        return ignoreData;
    }

    public void setIgnoreData(boolean ignoreData) {
        this.ignoreData = ignoreData;
    }

    
    public void parse(String input_data) {
        setInputData(input_data);
        parse();
    }

   
    public void parse() {
        try {
            for (int i = 0; i < this.size(); i++) {
                SubtitlePatternProcessor ps = this.elementAt(i);
                this.currentProcessor = ps;

                ps.setInputData(getInputData());
                ps.setTextLine(textLine);
                ps.setInputFile(inputFile);
                ps.setFPS(FPS);

                fireSubtitleDataPreParsingEvent();
                if (isIgnoreData()) {
                    setIgnoreData(false);
                    return;
                }//end if
                
                String[] matched_data = ps.matchPattern();
                isFound = (matched_data != null);
                if (isIsFound()) {
                    boolean is_create_new = (ps.getTargetObject() == null || this.isCreateNewObject());
                    if (is_create_new) {
                        String class_name = ps.getTargetObjectClassName();
                        boolean is_empty = Share.isEmpty(class_name);
                        if (is_empty) {
                            String msg = "Cannot create new record. Processor: [" + ps.getClass().getName() + "]. Reason: target object class name is empty.";
                            DEBUG.logger.log(Level.SEVERE, msg);
                            throw new Exception(msg);
                        }//if (is_empty)
                        Object new_object = Class.forName(class_name).newInstance();
                        ps.setTargetObject(new_object);
                        setCreateNewObject(false);
                        setCreatedRecord(new_object);
                        fireSubtitleRecordCreatedEvent();
                    }//end if (is_create_new)

                    ps.parsePattern(matched_data, ps.getTargetObject());
                    fireSubtitleDataParsedEvent();
                    boolean is_remove = ps.isRemovable();
                    if (is_remove) {
                        this.remove(ps);
                    }//end if (is_remove)
                    return;
                }//end if (is_found)
            }//end for (int i = 0; i < this.getPatternList().size(); i++) 
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }//end public Object parse()
    public String getTextLine() {
        return textLine;
    }

    public void setTextLine(String textLine) {
        this.textLine = textLine;
    }

    public int getTextLineNumber() {
        return textLineNumber;
    }

    public void setTextLineNumber(int textLineNumber) {
        this.textLineNumber = textLineNumber;
    }

    public SubtitlePatternProcessor getCurrentProcessor() {
        return currentProcessor;
    }

    public boolean isIsFound() {
        return isFound;
    }
}//public class SubtitleProcessorList extends Vector<SubtitlePatternProcessor>

