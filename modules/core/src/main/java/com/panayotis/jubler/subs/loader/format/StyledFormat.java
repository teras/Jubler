/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.format;

import com.panayotis.jubler.subs.style.StyleType;

public class StyledFormat {

    public final static byte COLOR_NORMAL = 0;
    public final static byte COLOR_REVERSE = 1;
    public final static byte COLOR_ALPHA_NORMAL = 2;
    public final static byte COLOR_ALPHA_REVERSE = 3;
    public final static byte FORMAT_UNDEFINED = 0;
    public final static byte FORMAT_STRING = 1;
    public final static byte FORMAT_INTEGRAL = 2;
    public final static byte FORMAT_REAL = 3;
    public final static byte FORMAT_FLAG = 4;
    public final static byte FORMAT_COLOR = 5;
    public final static byte FORMAT_DIRECTION = 6;
    public StyleType style;
    public String tag;
    public Object value;
    public boolean storable;

    public StyledFormat(StyleType style, String tag, Object value) {
        this(style, tag, value, true);
    }

    public StyledFormat(StyleType style, String tag, Object value, boolean storable) {
        this.style = style;
        this.tag = tag;
        this.value = value;
        this.storable = storable;
    }
}
