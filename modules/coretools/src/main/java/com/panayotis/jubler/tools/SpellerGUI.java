/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.tools.externals.AvailExternals;
import com.panayotis.jubler.tools.spell.SpellChecker;
import com.panayotis.jubler.tools.spell.SpellLanguage;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * The spell-check setup bar: pick the checker and its active language, and manage (download / remove)
 * languages — all up front, driven by the checker's language API. Hunspell is preselected by default.
 */
public class SpellerGUI extends JPanel {

    private final Speller tool;
    private final AvailExternals checkers;
    private JComboBox<String> checkerCombo;
    private JComboBox<SpellLanguage> languageCombo;
    private JLabel languageLabel;
    private JButton manageButton;
    private boolean updating = false;

    public SpellerGUI(Speller tool) {
        this.tool = tool;
        this.checkers = tool.getCheckers();
        buildUI();
        if (checkers.size() < 1) {
            checkerCombo.setEnabled(false);
            setLanguageControlsVisible(false);
            return;
        }
        populateCheckers();
    }

    private void buildUI() {
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        g.gridy = 0;
        add(new JLabel(__("Spell checker")), g);
        checkerCombo = new JComboBox<>();
        checkerCombo.addActionListener(e -> onCheckerChanged());
        g.gridx = 1;
        g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        add(checkerCombo, g);

        g.gridwidth = 1;
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        g.gridx = 0;
        g.gridy = 1;
        languageLabel = new JLabel(__("Language"));
        add(languageLabel, g);
        languageCombo = new JComboBox<>();
        languageCombo.addActionListener(e -> onLanguageChanged());
        g.gridx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        add(languageCombo, g);
        manageButton = new JButton(__("Manage languages…"));
        manageButton.addActionListener(e -> onManage());
        g.gridx = 2;
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        add(manageButton, g);
    }

    private void populateCheckers() {
        updating = true;
        checkerCombo.removeAllItems();
        for (int i = 0; i < checkers.size(); i++)
            checkerCombo.addItem(checkers.nameDescriptiveAt(i));
        int selected = defaultCheckerIndex();
        checkerCombo.setSelectedIndex(selected);
        tool.setCurrentChecker(selected);
        updating = false;
        refreshLanguages();
    }

    /** Saved preference if it still matches a present checker, else Hunspell, else the first checker. */
    private int defaultCheckerIndex() {
        String def = JublerPrefs.getString("list.default." + checkers.getType().toLowerCase(), "");
        int hunspell = -1;
        for (int i = 0; i < checkers.size(); i++) {
            String name = checkers.nameAt(i);
            if (name == null)
                continue;
            if (name.equalsIgnoreCase(def))
                return i;
            if (name.equalsIgnoreCase("Hunspell"))
                hunspell = i;
        }
        return hunspell >= 0 ? hunspell : 0;
    }

    private void onCheckerChanged() {
        if (updating)
            return;
        int i = checkerCombo.getSelectedIndex();
        if (i < 0)
            return;
        tool.setCurrentChecker(i);
        JublerPrefs.set("list.default." + checkers.getType().toLowerCase(), checkers.nameAt(i));
        refreshLanguages();
    }

    private void refreshLanguages() {
        SpellChecker checker = tool.getCurrentChecker();
        List<SpellLanguage> langs = checker == null ? Collections.emptyList() : checker.getInstalledLanguages();
        boolean hasLangs = !langs.isEmpty();

        updating = true;
        languageCombo.removeAllItems();
        for (SpellLanguage l : langs)
            languageCombo.addItem(l);
        if (hasLangs) {
            SpellLanguage active = checker.getActiveLanguage();
            if (active != null)
                languageCombo.setSelectedItem(active);
        }
        updating = false;

        languageLabel.setVisible(hasLangs);
        languageCombo.setVisible(hasLangs);
        manageButton.setVisible(checker != null && checker.supportsDownload());
        revalidate();
        repaint();
    }

    private void onLanguageChanged() {
        if (updating)
            return;
        SpellChecker checker = tool.getCurrentChecker();
        SpellLanguage sel = (SpellLanguage) languageCombo.getSelectedItem();
        if (checker != null && sel != null)
            checker.setActiveLanguage(sel);
    }

    private void onManage() {
        SpellChecker checker = tool.getCurrentChecker();
        if (checker == null)
            return;
        LanguageManagerDialog dialog = new LanguageManagerDialog(SwingUtilities.getWindowAncestor(this), checker);
        dialog.setVisible(true);
        if (dialog.wasChanged())
            refreshLanguages();
    }

    private void setLanguageControlsVisible(boolean visible) {
        languageLabel.setVisible(visible);
        languageCombo.setVisible(visible);
        manageButton.setVisible(visible);
    }
}
