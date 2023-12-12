/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.spell;

import com.panayotis.jubler.tools.externals.ExtProgram;
import com.panayotis.jubler.tools.externals.ExtProgramException;
import java.util.ArrayList;

public abstract class SpellChecker extends ExtProgram {

    public static final String family = "Speller";

    public abstract void start() throws ExtProgramException;

    public abstract ArrayList<SpellError> checkSpelling(String text);

    public abstract void stop();

    public abstract boolean insertWord(String word);

    public abstract boolean supportsInsert();
}
