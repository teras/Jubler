/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.options.JExtBasicOptions;
import com.panayotis.jubler.tools.externals.AvailExternals;
import com.panayotis.jubler.tools.spell.SpellChecker;

import javax.swing.*;
import java.awt.*;

import static com.panayotis.jubler.i18n.I18N.__;

public class SpellerGUI extends JPanel {

    private final Speller tool;
    private final JComboBox<String> combo;
    private final JButton configureB;

    public SpellerGUI(Speller tool) {
        this.tool = tool;
        AvailExternals checkers = tool.getCheckers();

        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        setLayout(new BorderLayout());

        JLabel label = new JLabel(__("Spell checker"));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        add(label, BorderLayout.WEST);

        combo = new JComboBox<>();
        add(combo, BorderLayout.CENTER);

        configureB = new JButton(__("Configure…"));
        configureB.addActionListener(evt -> configure());
        add(configureB, BorderLayout.LINE_END);

        if (checkers.size() < 1) {
            combo.setEnabled(false);
            configureB.setEnabled(false);
            return;
        }

        for (int i = 0; i < checkers.size(); i++)
            combo.addItem(checkers.nameDescriptiveAt(i));

        String def = JublerPrefs.getString("list.default." + checkers.getType().toLowerCase(), "").toLowerCase();
        int selected = 0;
        for (int i = 0; i < checkers.size(); i++)
            if (checkers.nameAt(i) != null && checkers.nameAt(i).toLowerCase().equals(def))
                selected = i;
        combo.setSelectedIndex(selected);
        tool.setCurrentChecker(selected);
        updateConfigureState();

        combo.addActionListener(evt -> {
            int i = combo.getSelectedIndex();
            tool.setCurrentChecker(i);
            JublerPrefs.set("list.default." + checkers.getType().toLowerCase(), checkers.nameAt(i));
            updateConfigureState();
        });
    }

    private void updateConfigureState() {
        SpellChecker checker = tool.getCurrentChecker();
        configureB.setEnabled(checker != null && checker.getOptionsPanel() != null);
    }

    private void configure() {
        SpellChecker checker = tool.getCurrentChecker();
        if (checker == null)
            return;
        JExtBasicOptions opts = checker.getOptionsPanel();
        if (opts == null)
            return;
        opts.configureInDialog(SwingUtilities.getWindowAncestor(this), __("Configure…"));
    }
}
