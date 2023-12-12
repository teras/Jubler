/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.options;

import com.panayotis.jubler.options.gui.TabPage;

public interface OptionsHolder extends TabPage {

    void loadPreferences();

    void savePreferences();
}
