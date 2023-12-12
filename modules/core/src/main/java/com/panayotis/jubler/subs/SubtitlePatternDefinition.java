/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs;

import java.util.regex.Pattern;

/**
 * This class is used to hold the definition of a pattern that is used by the
 * pattern matching mechanism of Java. It's created based on the view that each
 * subtitle data line is treated as having a repeatable pattern, and that the
 * parsing of the data using the pattern recognition is much more effective. The
 * workPattern is used for parsing, whilst the testPattern can be a simplified
 * version of the workPattern, although it's currently not being used.
 *
 * @author Hoang Duy Tran <hoang_tran>
 */
public class SubtitlePatternDefinition {

    private Pattern workPattern = null;
    private Pattern testPattern = null;

    /**
     * Default constructor
     */
    public SubtitlePatternDefinition() {
    }

    /**
     * Parameterised constructor
     *
     * @param work_pattern The pattern to be compiled by the Pattern.compile()
     * @param is_test_and_work_the_same Flag to set the work-pattern to be the
     * same as test pattern. When this flag is true, the compiled work-pattern
     * is used for the test-pattern as well. Both references are pointed to the
     * same compiled Pattern object.
     */
    public SubtitlePatternDefinition(String work_pattern, boolean is_test_and_work_the_same) {
        setWorkPattern(work_pattern);
        if (is_test_and_work_the_same)
            testPattern = workPattern;//end if (is_test_and_work_the_same)
    }

    /**
     * Parameterised constructor
     *
     * @param work_pattern The pattern to be compiled by the Pattern.compile()
     */
    public SubtitlePatternDefinition(String work_pattern) {
        setWorkPattern(work_pattern);
    }

    /**
     * Parameterised constructor
     *
     * @param work_pattern The pattern to be compiled by the Pattern.compile()
     * @param test_pattern The pattern to be compiled by the Pattern.compile()
     * and is used for testing purpose only
     */
    public SubtitlePatternDefinition(String work_pattern, String test_pattern) {
        setWorkPattern(work_pattern);
        setTestPattern(test_pattern);
    }

    /**
     * Return the compiled work-pattern
     *
     * @return The compiled work-pattern
     */
    public Pattern getWorkPattern() {
        return workPattern;
    }

    /**
     * Set the work-pattern.
     *
     * @param work_pattern The pattern to be compiled by the Pattern.compile()
     */
    public void setWorkPattern(String work_pattern) {
        this.workPattern = Pattern.compile(work_pattern);
    }

    /**
     * Gets the compiled test pattern.
     *
     * @return The compiled pattern, null if the pattern has not been set.
     */
    public Pattern getTestPattern() {
        return testPattern;
    }

    /**
     * Sets the test pattern.
     *
     * @param testing_pattern The pattern to be compiled by the
     * Pattern.compile()
     *
     */
    public void setTestPattern(String testing_pattern) {
        testPattern = Pattern.compile(testing_pattern);
    }
}
