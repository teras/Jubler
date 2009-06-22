/*
 *  IncompatibleRecordType.java 
 * 
 *  Created on: Jun 21, 2009 at 3:34:41 PM
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

package com.panayotis.jubler.exceptions;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author Hoang Duy Tran <hoangduytran1960@googlemail.com>
 */
public class IncompatibleRecordTypeException extends Exception{
    public static final String DEFAULT_MSG = _("Incompatible type detected.");
    /**
     * Constructs a new exception with <code>null</code> as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public IncompatibleRecordTypeException() {
	super(DEFAULT_MSG);
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param   message   the detail message. The detail message is saved for 
     *          later retrieval by the {@link #getMessage()} method.
     */
    public IncompatibleRecordTypeException(String message) {
	super(message);
    }
    
    public IncompatibleRecordTypeException(Class source, Class target) {
	super(source.getName() + "\n" + _("and") + "\n" + target.getName());
    }    
}//end public class IncompatibleRecordType extends Exception
