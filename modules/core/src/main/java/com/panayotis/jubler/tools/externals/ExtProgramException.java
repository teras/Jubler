/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.externals;

public class ExtProgramException extends Exception {

    /**
     * Creates a new instance of ExtProgramException
     */
    public ExtProgramException(Throwable cause) {
        super(cause);
    }

    public ExtProgramException(String message) {
        super(message);
    }
}
