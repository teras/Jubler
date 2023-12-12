/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.time;

import java.text.NumberFormat;
import java.text.ParseException;
import javax.swing.text.NumberFormatter;

public class FramesFormatter extends NumberFormatter {

    private final static NumberFormat format;

    static {
        format = NumberFormat.getIntegerInstance();
        format.setMaximumIntegerDigits(7);
    }

    public FramesFormatter() throws ParseException {
        super(format);
    }
}
