/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.event;

public class StyleoverEvent {

    public Object value;
    public int position;

    public StyleoverEvent(Object value, int position) {
        this.value = value;
        this.position = position;
    }

    public StyleoverEvent(StyleoverEvent old) {
        value = old.value;
        position = old.position;
    }

    @Override
    public String toString() {
        return value.toString() + "," + position;
    }

    @Override
    public boolean equals(Object value) {
        StyleoverEvent e = (StyleoverEvent) value;
        return e.value.equals(value) && e.position == position;
    }
}
