/*
 *
 * SubtitlePatternProcessor.java
 *
 * Created on 04-Dec-2008, 00:17:13
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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a template for pattern processor. It's incharge of pattern
 * matching, using the pre-defined pattern (the workPattern), and returns
 * the matched groups of data. 
 * The orginal list of group - from the Pattern.matcher() -
 * is returned if the {@link #matchIndexList}  
 * is null, otherwise indexes from the
 * list are used to obtain the parsed data, 
 * resulting in a zero-based ordered 
 * list of the matching results, so instead of having to access to
 * individual data item using the index from the original group list, 
 * programmers can use zero as the first item, one for second item and so on.
 * <br>
 * example:
 * <blockquote><pre class="Java" name="code">
 * private String pattern = digits + sp + son_time + sp + son_time + sp + printable;
 * int index[] = new int[]{1, 3, 4, 5, 6, 8, 9, 10, 11, 13};
 * public SONSubtitleEvent() {
 *      super(pattern);
 *      setMatchIndexList(index);
 * }
 * 
 * public void parsePattern(String[] matched_data, Object record) {        
 *      if (record instanceof SonSubEntry) {
 *          setSonSubEntry((SonSubEntry) record);
 *          sonSubEntry.event_id = DVDMaestro.parseShort(matched_data[0]);
 * 
 *          Time start, finish;
 *          start = new Time(matched_data[1], matched_data[2], matched_data[3], matched_data[4]);
 *          finish = new Time(matched_data[5], matched_data[6], matched_data[7], matched_data[8]);
 * 
 *          sonSubEntry.setStartTime(start);
 *          sonSubEntry.setFinishTime(finish);
 *          sonSubEntry.image_filename = matched_data[9];
 *      }//end if
 * }
 * </pre></blockquote>
 * 
 * To work out the indexes, either count the patterns, or do not set index
 * at all, leaving it to null. The pattern matched result will return in the 
 * array <code>matched_data</code>.
 * @author Hoang Duy Tran <hoang_tran>
 */
public abstract class SubtitlePatternProcessor {

    private SubtitlePatternDefinition pattern = null;
    private String[] found_list = null;
    private int[] matchIndexList = null;    
    private String inputData = null;
    private String textLine = null;
    private Object targetObject = null;
    private String targetObjectClassName = null;
    private boolean removable = false;
    private float FPS = 25f;
    private File inputFile = null;

    /**
     * Default constructor, without any parameters
     */
    public SubtitlePatternProcessor(){}
    
    /**
     * Parameterised constructor
     * @param pattern The regular expression string to be compiled by the 
     * Pattern.compile() method.
     * @param index The index at which the result list of matched groups is to 
     * be accessed when the data is found.
     * @param targetObjectName The fully qualified name of the class object that the parsing
     * routine will use to create in order to pass to the implementation
     * of parse() method. The creation of the object is controlled by 
     * a flag.
     * @see com.panayotis.jubler.subs.SubtitleProcessorList
     */
    public SubtitlePatternProcessor(String pattern, int index, String targetObjectName) {
        this(pattern, new int[]{index}, targetObjectName, false);
    }

    /**
     * Parameterised constructor
     * @param pattern The regular expression string to be compiled by the 
     * Pattern.compile() method.
     * @param index The index at which the result list of matched groups is 
     * accessed when the data is found.
     * @param targetObjectName The fully qualified name of the class object that the parsing
     * routine will use to create in order to pass to the implementation
     * of parse() method. The creation of the object is controlled by 
     * a flag.
     * @param removable Flag to indicate if the processor is to be removed from 
     * the list of processors once the data is recognised and parsed. This is
     * used for data patterns that are ocurred once only within the subtitle
     * file. 
     */
    public SubtitlePatternProcessor(String pattern, int index, String targetObjectName, boolean removable) {
        this(pattern, new int[]{index}, null, removable);
        this.setTargetObjectClassName(targetObjectName);
    }

    /**
     * Parameterised constructor
     * @param pattern The regular expression string to be compiled by the 
     * Pattern.compile() method.
     * @param index_list The list of indexes at which the result list of 
     * matched groups is accessed when the data is found.
     * @param targetObjectName The fully qualified name of the class object that the parsing
     * routine will use to create in order to pass to the implementation
     * of parse() method. The creation of the object is controlled by 
     * a flag.
     */
    public SubtitlePatternProcessor(String pattern, int[] index_list, String targetObjectName) {
        this(pattern, index_list);
        this.setTargetObjectClassName(targetObjectName);
    }
    
    /**
     * Parameterised constructor
     * @param pattern The regular expression string to be compiled by the 
     * Pattern.compile() method.
     * @param index_list The list of indexes at which the result list of 
     * matched groups is accessed when the data is found.
     * @param target_object 
     * @param removable Flag to indicate if the processor is to be removed from 
     * the list of processors once the data is recognised and parsed. This is
     * used for data patterns that are ocurred once only within the subtitle
     * file. 
     */
    public SubtitlePatternProcessor(String pattern, int[] index_list, Object target_object, boolean removable) {
        this(pattern, index_list);
        this.setTargetObject(target_object);
        this.setRemovable(removable);
    }

    /**
     * 
     * @param pattern
     * @param index_list
     * @param removable
     */
    public SubtitlePatternProcessor(String pattern, int[] index_list, boolean removable) {
        this(pattern, index_list);
        this.setRemovable(removable);
    }
    
    /**
     * Creates the pattern definition from the pattern string and set
     * the working, testing pattern to be the same as
     * @param pattern
     */
    public SubtitlePatternProcessor(String pattern){
        this.pattern = new SubtitlePatternDefinition(pattern, true);
    }
    
    /**
     * 
     * @param pattern
     * @param index
     */
    public SubtitlePatternProcessor(String pattern, int index) {
        this(pattern);
        this.matchIndexList = new int[]{index};
    }
    
    /**
     * 
     * @param pattern
     * @param index_list
     */
    public SubtitlePatternProcessor(String pattern, int[] index_list) {
        this(pattern);
        this.matchIndexList = index_list;
    }
    
    /**
     * 
     * @return The array of matched groups
     */
    public String[] matchPattern() {
        try {
            boolean has_data = (this.getTextLine() != null);
            if (has_data) {
                boolean has_index_list = (this.getMatchIndexList() != null);
                if (has_index_list) {
                    return this.matchPattern(getTextLine(), getMatchIndexList());
                } else {
                    return this.matchPattern(getTextLine());
                }//end if if (has_index_list)
            }//end if (has_data)
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return null;
    }

    /**
     * 
     * @param input
     * @param matching_index_list
     * @return The matched group of data, null if no match is found.
     */
    public String[] matchPattern(String input, int[] matching_index_list) {
        try {
            setFoundList(matchPattern(input));
            boolean has_result = (getFoundList() != null);
            if (has_result) {
                int index_len = matching_index_list.length;
                boolean is_selective = (matching_index_list != null && index_len > 0);
                if (is_selective) {
                    String[] selective_values = new String[matching_index_list.length];
                    for (int i = 0; i < index_len; i++) {
                        int matching_group_index = matching_index_list[i];
                        try {
                            selective_values[i] = getFoundList()[matching_group_index];
                        } catch (Exception ex) {
                        }
                    }//end for (int i=0; i < index_len; i++)
                    return selective_values;
                }//end if (is_selective)
            }//end if (has_result)
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return getFoundList();
    }//end public String[] matchPattern(String input, int[] matching_index_list)

    /**
     * This routine matching the working pattern that was defined
     * against the input text line and return the array of matched
     * groups of data within the input. The group of matched data
     * is obtained by the  
     * @param input
     * @return The array of matched group, null if no match was found.
     */
    public String[] matchPattern(String input) { 
        setFoundList(null);
        try {
            Pattern pat = getPattern().getWorkPattern();
            Matcher m = pat.matcher(input);
            boolean is_found = m.find();
            if (is_found) {
                int group_count = m.groupCount() + 1;
                setFoundList(new String[group_count]);
                for (int k = 0; k < group_count; k++) {
                    getFoundList()[k] = m.group(k);
                }//end for            
            }//end String[] found_list
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
        return getFoundList();
    }//end public String[] matchPattern(String input)

    /**
     * 
     * @return The reference to pattern definition.
     */
    public SubtitlePatternDefinition getPattern() {
        return pattern;
    }

    /**
     * 
     * @param pattern
     */
    public void setPattern(SubtitlePatternDefinition pattern) {
        this.pattern = pattern;
    }

    /**
     * 
     * @return The array of matching indices, null if the reference has not
     * been set.
     */
    public int[] getMatchIndexList() {
        return matchIndexList;
    }

    /**
     * 
     * @param matchIndexList
     */
    public void setMatchIndexList(int matchIndexList) {
        this.matchIndexList = new int[]{matchIndexList};
    }
    /**
     * 
     */
    public void setMatchIndexList(int[] matchIndexList) {
        this.matchIndexList = matchIndexList;
    }

    /**
     * 
     * @return The textual input data of the file, null if the reference has
     * not been set.
     */
    public String getInputData() {
        return inputData;
    }

    /**
     * 
     * @param inputData
     */
    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    /**
     * 
     * @return The frame rate per second, 25fps is the default setting.
     */
    public float getFPS() {
        return FPS;
    }

    /**
     * 
     * @param FPS
     */
    public void setFPS(float FPS) {
        this.FPS = FPS;
    }

    /**
     * 
     * @return The reference of the input file.
     */
    public File getInputFile() {
        return inputFile;
    }

    /**
     * 
     * @param inputFile
     */
    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * 
     * @param matched_data
     * @param old_object
     */
    public abstract void parsePattern(String[] matched_data, Object old_object);

    /**
     * 
     * @return The reference to the target object, null if the object has not
     * been created.
     */
    public Object getTargetObject() {
        return targetObject;
    }

    /**
     * 
     * @param targetObject
     */
    public void setTargetObject(Object targetObject) {
        this.targetObject = targetObject;
        if (this.targetObject != null){
            String cl_name = targetObject.getClass().getName();
            setTargetObjectClassName(cl_name);
        }//end 
    }

    /**
     * 
     * @return true if the processor is removable, false otherwise.
     */
    public boolean isRemovable() {
        return removable;
    }

    /**
     * 
     * @param removable
     */
    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    /**
     * 
     * @return The class-name of the target object.
     */
    public String getTargetObjectClassName() {
        return targetObjectClassName;
    }

    /**
     * 
     * @param targetObjectClassName
     */
    public void setTargetObjectClassName(String targetObjectClassName) {
        this.targetObjectClassName = targetObjectClassName;
    }

    /**
     * 
     * @return The text line currently being processed.
     */
    public String getTextLine() {
        return textLine;
    }

    /**
     * 
     * @param textLine
     */
    public void setTextLine(String textLine) {
        this.textLine = textLine;
    }

    /**
     * 
     * @return The reference to the found
     */
    public String[] getFoundList() {
        return found_list;
    }

    /**
     * 
     * @param found_list
     */
    public void setFoundList(String[] found_list) {
        this.found_list = found_list;
    }
}
