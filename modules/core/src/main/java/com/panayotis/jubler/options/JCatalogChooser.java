/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.tools.externals.Recipe;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Window;
import java.net.URI;
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
    private final JTextArea descPreview = new JTextArea(3, 40);
    private final JButton urlB = new JButton();
    private boolean accepted = false;

    JCatalogChooser(Window parent, List<Recipe> available) {
        super(parent, __("Available recipes"), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        for (Recipe r : available)
            model.addElement(r);
        list = new JList<>(model);

        // Preview the highlighted recipe's description, so the user knows what it does before importing.
        descPreview.setEditable(false);
        descPreview.setLineWrap(true);
        descPreview.setWrapStyleWord(true);
        descPreview.setOpaque(false);
        descPreview.setFocusable(false);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updatePreview();
        });

        // Globe button: opens the selected recipe's web page; disabled when it has none.
        // The icon keeps its native aspect ratio (it is a 4:3 globe, like the flags — not square).
        ImageIcon globe = Theme.loadIcon("flag-global");
        if (globe != null)
            urlB.setIcon(new ImageIcon(globe.getImage().getScaledInstance(-1, 24, Image.SCALE_SMOOTH)));
        else
            urlB.setText("🌐");
        urlB.setToolTipText(__("Open the recipe's web page"));
        urlB.setEnabled(false);
        urlB.addActionListener(e -> openUrl());

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(new JLabel(__("Select the recipes to import:")), BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(540, 240));
        JScrollPane previewScroll = new JScrollPane(descPreview);
        previewScroll.setBorder(BorderFactory.createTitledBorder(__("What it does")));
        JPanel listAndPreview = new JPanel(new BorderLayout(0, 8));
        listAndPreview.add(scroll, BorderLayout.CENTER);
        listAndPreview.add(previewScroll, BorderLayout.SOUTH);
        content.add(listAndPreview, BorderLayout.CENTER);

        // Bottom row: globe on the left, Cancel/Import on the right (same height).
        JPanel buttons = new JPanel(new BorderLayout());
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftButtons.add(urlB);
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton cancel = new JButton(__("Cancel"));
        JButton ok = new JButton(__("Import"));
        // Match the globe button's height to the text buttons and give it some breathing room.
        int h = cancel.getPreferredSize().height;
        urlB.setPreferredSize(new Dimension(Math.round(h * 1.5f), h));
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> {
            accepted = true;
            dispose();
        });
        rightButtons.add(cancel);
        rightButtons.add(ok);
        buttons.add(leftButtons, BorderLayout.WEST);
        buttons.add(rightButtons, BorderLayout.EAST);
        content.add(buttons, BorderLayout.SOUTH);
        updatePreview();

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
        updatePreview();
    }

    /** Show the highlighted recipe's description (or a hint when there is none / nothing selected). */
    private void updatePreview() {
        Recipe sel = list.getSelectedValue();
        if (sel == null)
            descPreview.setText(__("Select a recipe to see what it does."));
        else if (sel.getDescription().isEmpty())
            descPreview.setText(__("No description provided."));
        else
            descPreview.setText(sel.getDescription());
        descPreview.setCaretPosition(0);
        urlB.setEnabled(sel != null && !sel.getUrl().isEmpty());
    }

    /** Open the selected recipe's web page in the default browser. */
    private void openUrl() {
        Recipe sel = list.getSelectedValue();
        if (sel == null || sel.getUrl().isEmpty())
            return;
        try {
            Desktop.getDesktop().browse(URI.create(sel.getUrl()));
        } catch (Exception e) {
            DEBUG.debug(e);
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
