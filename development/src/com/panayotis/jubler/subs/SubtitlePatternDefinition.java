/*
 * SubtitlePatternDefinition.java
 *
 * Created on 04-Dec-2008, 00:11:43
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

import java.util.regex.Pattern;

/**
 * This class is used to hold the definition of a pattern that is used
 * by the pattern matching mechanism of Java.
 * It's created based on the view that each subtitle data line is treated
 * as having a repeatable pattern, and that the parsing of the data using
 * the pattern recognition is much more effective.
 * The workPattern is used for parsing, whilst the testPattern can be a 
 * simplified version of the workPattern, although it's currently not being
 * used.
 * 
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SubtitlePatternDefinition {
    private Pattern workPattern = null;
    private Pattern testPattern = null;
    
    public SubtitlePatternDefinition(){}

    public SubtitlePatternDefinition(String work_pattern, boolean is_test_and_work_the_same){
        setWorkPattern(work_pattern);
        if (is_test_and_work_the_same){
            testPattern = workPattern;
        }//end if (is_test_and_work_the_same)
    }
    
    public SubtitlePatternDefinition(String work_pattern){
        setWorkPattern(work_pattern);
    }
    
    public SubtitlePatternDefinition(String work_pattern, String test_pattern){
        setWorkPattern(work_pattern);
        setTestPattern(test_pattern);
    }
    
    public Pattern getWorkPattern() {
        return workPattern;
    }

    public void setWorkPattern(String working_pattern) {
        this.workPattern = Pattern.compile(working_pattern);        
    }

    public Pattern getTestPattern() {
        return testPattern;
    }

    public void setTestPattern(String testing_pattern) {
        testPattern = Pattern.compile(testing_pattern);
    }
}
