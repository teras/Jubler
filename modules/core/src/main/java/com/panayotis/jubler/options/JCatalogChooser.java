/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.tools.externals.Recipe;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Modal picker for the downloaded recipe catalog: a multi-select list; {@link #choose()}
 * returns deep copies of the recipes the user selected (empty when cancelled).
 */
class JCatalogChooser extends JDialog {

    private final JList<Recipe> list;
    private final DefaultListModel<Recipe> model = new DefaultListModel<>();
    private boolean accepted = false;

    JCatalogChooser(Window parent, List<Recipe> available) {
        super(parent, __("Available recipes"), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        for (Recipe r : available)
            model.addElement(r);
        list = new JList<>(model);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(new JLabel(__("Select the recipes to import:")), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(540, 240));
        content.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton(__("Cancel"));
        JButton ok = new JButton(__("Import"));
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> {
            accepted = true;
            dispose();
        });
        buttons.add(cancel);
        buttons.add(ok);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setLocationRelativeTo(parent);
    }

    /** Replace the shown recipes (e.g. when a fresh fetch arrives), keeping any selection by name. */
    void setRecipes(List<Recipe> recipes) {
        Recipe sel = list.getSelectedValue();
        String selName = sel == null ? null : sel.getName();
        model.clear();
        for (Recipe r : recipes)
            model.addElement(r);
        if (selName != null)
            for (int i = 0; i < model.size(); i++)
                if (selName.equals(model.get(i).getName())) {
                    list.setSelectedIndex(i);
                    break;
                }
    }

    List<Recipe> choose() {
        setVisible(true);
        if (!accepted)
            return Collections.emptyList();
        List<Recipe> chosen = new ArrayList<>();
        for (Recipe r : list.getSelectedValuesList())
            chosen.add(Recipe.fromJsonString(r.toJsonString(false)));   // deep copy
        return chosen;
    }
}
