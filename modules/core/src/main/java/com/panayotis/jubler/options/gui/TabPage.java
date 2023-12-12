/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.options.gui;

import javax.swing.*;

public interface TabPage {

    JPanel getTabPanel();

    String getTabName();

    String getTabTooltip();

    Icon getTabIcon();

    /* Fire this method if the tab should be updated */
    void changeProgram();
}
