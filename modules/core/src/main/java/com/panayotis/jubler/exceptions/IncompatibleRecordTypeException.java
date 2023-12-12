/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.exceptions;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * This class is used to raise exception condition when differences between two
 * classes are found. The difference should not based on the result of
 * 'instanceof' test but on the absolute class-name comparisons, as the
 * 'instanceof' treats the implementation and inheritance as the same.
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class IncompatibleRecordTypeException extends Exception {

    /**
     * The default message "Incompatible type detected". This message is used
     * with the default constructor.
     */
    public static final String DEFAULT_MSG = __("Incompatible type detected.");

    /**
     * Constructs a new exception with
     * <code>null</code> as its detail message. The cause is not initialized,
     * and may subsequently be initialized by a call to {@link #initCause}.
     */
    public IncompatibleRecordTypeException() {
        super(DEFAULT_MSG);
    }

    /**
     * Constructs a new exception with the specified detail message. The cause
     * is not initialized, and may subsequently be initialized by a call to
     * {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later
     * retrieval by the {@link #getMessage()} method.
     */
    public IncompatibleRecordTypeException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified classes. The names of two
     * classes are retrieved and included in the message passing to the
     * exception message, with an 'and' connector, to indicate that the
     * execption is raised due to the source of problem.
     *
     * @param source The source of differences.
     * @param target The target of differences.
     */
    public IncompatibleRecordTypeException(Class source, Class target) {
        super(source.getName() + "\n" + __("and") + "\n" + target.getName());
    }
}//end public class IncompatibleRecordType extends Exception
