/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools.externals;

import com.panayotis.jubler.options.JExtBasicOptions;

public abstract class ExtProgram {

    /**
     * Get a JPanel having the GUI controls for the external program options
     */
    public abstract JExtBasicOptions getOptionsPanel();

    /* Get the name of this external program, useful e.g. to save options or for labels */
    public abstract String getName();

    public String getDescriptiveName() {
        return getName();
    }
}
