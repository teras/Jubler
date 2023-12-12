/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.spell;

@SuppressWarnings("UseOfObsoleteCollectionType")
public class SpellError {

    public int position;
    public String original;
    public java.util.Vector<String> alternatives;

    /**
     * Creates a new instance of SpellMistake
     */
    public SpellError(int position, String original, java.util.Vector<String> alts) {
        this.position = position;
        this.original = original;
        alternatives = alts;
    }
}
