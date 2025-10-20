/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import java.util.ArrayList;
import java.util.Collection;

public class ExcludingList<E> extends ArrayList<E> {
    public ExcludingList(E excluding, Collection<E> list1, Collection<E> list2) {
        if (list1 != null)
            addAll(list1);
        if (list2 != null)
            addAll(list2);
        if (excluding != null)
            remove(excluding);
    }
}
