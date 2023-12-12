/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.translate;

public class Language {

    public final String id;
    public final String displayName;

    public Language(String id, String displayName) {
        this.displayName = displayName;
        this.id = id;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
