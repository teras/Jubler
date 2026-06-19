/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.tools.externals.AvailExternals;

import static com.panayotis.jubler.i18n.I18N.__;

public class JSpellerOptions extends JExternalOptions {
    public JSpellerOptions(AvailExternals list) {
        super(list);
    }

    @Override
    public String getTabName() {
        return __("Speller");
    }
}
