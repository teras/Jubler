/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals.gui;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.externals.Recipe;
import com.panayotis.jubler.tools.externals.RecipeParam;
import com.panayotis.jubler.tools.externals.RecipeSecrets;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Per-run dialog: auto-generated widgets for the recipe's <b>per-run</b> params, plus a
 * scope selector and (when the command needs {@code %j}) an "other window" picker.
 * Persistent params are taken from config (stored values) and are not asked here, which
 * removes the old popup-in-popup.
 */
public class JRecipeRunDialog extends JDialog {

    private final Recipe recipe;
    private final JubFrame jubler;
    private final Map<RecipeParam, JComponent> widgets = new LinkedHashMap<>();
    private final JComboBox<String> scopeC = new JComboBox<>();
    private boolean accepted = false;

    public JRecipeRunDialog(Window parent, JubFrame jubler, Recipe recipe) {
        super(parent, recipe.getName(), ModalityType.APPLICATION_MODAL);
        this.recipe = recipe;
        this.jubler = jubler;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        int row = 0;

        for (RecipeParam p : recipe.getParams()) {
            // A window selection is inherently per-run (a window can't be a stored value).
            if (p.isPersistent() && p.getType() != RecipeParam.Type.WINDOW)
                continue;
            JComponent w = widgetFor(p);
            widgets.put(p, w);
            if (w instanceof JCheckBox) {
                // The whole row toggles: the label is the checkbox's own text and it spans the full width.
                ((JCheckBox) w).setText(p.getLabel());
                addCheckboxRow(form, row++, (JCheckBox) w, p.getHelp());
            } else {
                addRow(form, row++, p.getLabel() + ":", w, p.getHelp());
            }
        }

        scopeC.addItem(__("All subtitles"));
        boolean hasSelection = jubler.getSelectedRows().length > 0;
        if (hasSelection)
            scopeC.addItem(__("Selected rows only"));
        addRow(form, row++, __("Apply to:"), scopeC, "");
        if (hasSelection)
            scopeC.setSelectedIndex(1);

        JPanel buttons = new JPanel(new BorderLayout());
        JPanel right = new JPanel();
        JButton cancel = new JButton(__("Cancel"));
        JButton ok = new JButton(__("Run"));
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(e -> {
            accepted = true;
            dispose();
        });
        right.add(cancel);
        right.add(ok);
        buttons.add(right, BorderLayout.EAST);

        JPanel content = new JPanel(new BorderLayout());
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setLocationRelativeTo(parent);
    }

    /** True if there's anything worth asking before running (else just run with defaults/all). */
    public static boolean needsPrompt(Recipe recipe, JubFrame jubler) {
        for (RecipeParam p : recipe.getParams())
            if (!p.isPersistent() || p.getType() == RecipeParam.Type.WINDOW)
                return true;
        return jubler.getSelectedRows().length > 0;
    }

    public boolean showRun() {
        setVisible(true);
        return accepted;
    }

    /* ===================== results ===================== */

    /** Resolved values for ALL params (per-run from widgets, persistent from stored config). */
    public Map<String, String> getValues() {
        Map<String, String> values = new LinkedHashMap<>();
        for (RecipeParam p : recipe.getParams()) {
            if (p.getType() == RecipeParam.Type.WINDOW) {
                // Resolved by the executor (the selected window's content is serialized to temp).
                values.put(p.getKey(), "");
            } else if (p.isPersistent()) {
                String stored = recipe.getStoredValue(p.getKey());
                values.put(p.getKey(), stored == null ? p.getDefaultValue()
                        : (p.isSecret() ? RecipeSecrets.decrypt(stored) : stored));
            } else {
                values.put(p.getKey(), readWidget(p, widgets.get(p)));
            }
        }
        return values;
    }

    /** Selected window per per-run WINDOW param (its content is serialized by the executor). */
    public Map<String, JubFrame> getWindowSelections() {
        Map<String, JubFrame> sel = new LinkedHashMap<>();
        for (RecipeParam p : recipe.getParams()) {
            if (p.getType() != RecipeParam.Type.WINDOW)
                continue;
            JComponent w = widgets.get(p);
            if (w instanceof JComboBox)
                sel.put(p.getKey(), (JubFrame) ((JComboBox<?>) w).getSelectedItem());
        }
        return sel;
    }

    public List<SubEntry> getScope() {
        if (scopeC.getSelectedIndex() <= 0)
            return null;   // all
        SubEntry[] sel = jubler.getSelectedSubs();
        List<SubEntry> list = new ArrayList<>();
        for (SubEntry e : sel)
            list.add(e);
        return list;
    }

    /* ===================== widgets ===================== */

    private JComponent widgetFor(RecipeParam p) {
        switch (p.getType()) {
            case COMBOBOX: {
                JComboBox<String> c = new JComboBox<>(p.getChoiceList());
                c.setSelectedItem(p.getDefaultValue());
                return c;
            }
            case CHECKBOX: {
                JCheckBox c = new JCheckBox();
                c.setSelected(Boolean.parseBoolean(p.getDefaultValue()));
                return c;
            }
            case PATH: {
                JPanel line = new JPanel(new BorderLayout(4, 0));
                JTextField t = new JTextField(p.getDefaultValue(), 20);
                JButton b = new JButton(__("Browse"));
                b.addActionListener(e -> {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileSelectionMode(p.isFolder() ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
                    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                        t.setText(fc.getSelectedFile().getAbsolutePath());
                });
                line.add(t, BorderLayout.CENTER);
                line.add(b, BorderLayout.EAST);
                line.putClientProperty("field", t);
                return line;
            }
            case SECRET: {
                return new JPasswordField(20);
            }
            case WINDOW: {
                JComboBox<JubFrame> c = new JComboBox<>(new DefaultComboBoxModel<>(otherWindows().toArray(new JubFrame[0])));
                c.setRenderer(windowRenderer());
                return c;
            }
            case LANGUAGE: {
                JComboBox<String> c = new JComboBox<>(languageCodes());
                c.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean foc) {
                        super.getListCellRendererComponent(list, value, index, sel, foc);
                        if (value != null)
                            setText(languageLabel(value.toString()));
                        return this;
                    }
                });
                if (!p.getDefaultValue().isEmpty())
                    c.setSelectedItem(p.getDefaultValue());
                return c;
            }
            case TEXTBOX:
            default:
                return new JTextField(p.getDefaultValue(), 20);
        }
    }

    @SuppressWarnings("unchecked")
    private String readWidget(RecipeParam p, JComponent w) {
        switch (p.getType()) {
            case COMBOBOX:
                Object sel = ((JComboBox<String>) w).getSelectedItem();
                return sel == null ? "" : sel.toString();
            case CHECKBOX:
                return ((JCheckBox) w).isSelected() ? p.getCheckedValue() : "";
            case PATH:
                return ((JTextField) ((JPanel) w).getClientProperty("field")).getText();
            case SECRET:
                return new String(((JPasswordField) w).getPassword());
            case LANGUAGE: {
                Object lang = ((JComboBox<String>) w).getSelectedItem();
                return lang == null ? "" : lang.toString();
            }
            case TEXTBOX:
            default:
                return ((JTextField) w).getText();
        }
    }

    private List<JubFrame> otherWindows() {
        List<JubFrame> list = new ArrayList<>();
        for (JubFrame w : JubFrame.windows)
            if (w != jubler)
                list.add(w);
        return list;
    }

    /** Show a window by its subtitle file name (not the frame's toString). */
    private static String windowName(JubFrame win) {
        if (win != null && win.getSubtitles() != null && win.getSubtitles().getSubFile() != null
                && win.getSubtitles().getSubFile().getSaveFile() != null)
            return win.getSubtitles().getSubFile().getSaveFile().getName();
        return __("Untitled");
    }

    private static DefaultListCellRenderer windowRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean foc) {
                super.getListCellRendererComponent(list, value, index, sel, foc);
                if (value instanceof JubFrame)
                    setText(windowName((JubFrame) value));
                return this;
            }
        };
    }

    /** ISO 639 language codes, sorted by their display name in the current locale. */
    private static String[] languageCodes() {
        String[] codes = Locale.getISOLanguages();
        Arrays.sort(codes, Comparator.comparing(c -> languageLabel(c).toLowerCase()));
        return codes;
    }

    private static String languageLabel(String code) {
        String name = new Locale(code).getDisplayLanguage();
        return name == null || name.isEmpty() ? code : name + " (" + code + ")";
    }

    private static void addCheckboxRow(JPanel p, int row, JCheckBox box, String help) {
        GridBagConstraints fg = new GridBagConstraints();
        fg.gridx = 0;
        fg.gridy = row;
        fg.gridwidth = 2;
        fg.fill = GridBagConstraints.HORIZONTAL;
        fg.weightx = 1.0;
        fg.anchor = GridBagConstraints.LINE_START;
        fg.insets = new Insets(3, 4, 3, 4);
        p.add(box, fg);
        if (help != null && !help.isEmpty()) {
            GridBagConstraints ig = new GridBagConstraints();
            ig.gridx = 2;
            ig.gridy = row;
            ig.fill = GridBagConstraints.VERTICAL;
            ig.insets = new Insets(3, 0, 3, 2);
            p.add(new InfoButton(box.getText(), help), ig);
        }
    }

    private static void addRow(JPanel p, int row, String label, Component field, String help) {
        GridBagConstraints lg = new GridBagConstraints();
        lg.gridx = 0;
        lg.gridy = row;
        lg.anchor = GridBagConstraints.LINE_END;
        lg.insets = new Insets(3, 4, 3, 6);
        p.add(new JLabel(label), lg);
        GridBagConstraints fg = new GridBagConstraints();
        fg.gridx = 1;
        fg.gridy = row;
        fg.fill = GridBagConstraints.HORIZONTAL;
        fg.weightx = 1.0;
        fg.insets = new Insets(3, 0, 3, 4);
        p.add(field, fg);
        if (help != null && !help.isEmpty()) {
            GridBagConstraints ig = new GridBagConstraints();
            ig.gridx = 2;
            ig.gridy = row;
            ig.fill = GridBagConstraints.VERTICAL;
            ig.insets = new Insets(3, 0, 3, 2);
            p.add(new InfoButton(label.endsWith(":") ? label.substring(0, label.length() - 1) : label, help), ig);
        }
    }
}
