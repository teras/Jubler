/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.format;

public class SubEv implements Comparable<SubEv> {

    String value;
    int start;

    public SubEv(String val, int start) {
        value = val;
        this.start = start;
    }

    public boolean equals(Comparable other) {
        return compareTo((SubEv) other) == 0;
    }

    /* Since we want ot add the events in reverse order, the sorting is done on reverse! */
    public int compareTo(SubEv other) {
        if (start < other.start)
            return 1;
        if (start > other.start)
            return -1;
        if (value.equals(other.value))
            return 0;
        return -1;  // In all other occasions, means that it's a different object so just put it somewhere
    }
}
