/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

import java.util.ArrayList;
import java.util.Collection;

public class ExcludingList<E> extends ArrayList<E> {
    @SafeVarargs
    public ExcludingList(E excluding, Collection<E>... lists) {
        if (lists != null)
            for (Collection<E> list : lists)
                if (list != null)
                    addAll(list);
        if (excluding != null)
            remove(excluding);
    }
}
