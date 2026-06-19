/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals.gui;

import com.panayotis.jubler.tools.externals.Recipe;
import com.panayotis.jubler.tools.externals.RecipeParam;
import com.panayotis.jubler.tools.externals.RecipeSecrets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.BiConsumer;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Detail editor for a single {@link RecipeParam}: common header (key/label/help/type/
 * persistent) plus a type-specific card. Edits write straight into the bound param.
 */
public class JParamDetail extends JPanel {

    private final JTextField keyT = new JTextField();
    private final JTextField labelT = new JTextField();
    private final JTextField helpT = new JTextField();
    private final JComboBox<RecipeParam.Type> typeC = new JComboBox<>(RecipeParam.Type.values());
    private final JCheckBox persistentB = new JCheckBox(__("Persistent (set once, not asked each run)"));
    private final JLabel valueL = new JLabel(__("Stored value:"));
    private final JTextField valueT = new JTextField();

    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);

    // TextBox
    private final JTextField tbDefault = new JTextField();
    private final JTextField tbFormatter = new JTextField();
    // ComboBox
    private final JTextField cbChoices = new JTextField();
    private final JTextField cbDefault = new JTextField();
    private final JTextField cbFormatter = new JTextField();
    // CheckBox
    private final JTextField chkChecked = new JTextField();
    private final JCheckBox chkDefault = new JCheckBox(__("Checked by default"));
    // Path
    private final JTextField pathDefault = new JTextField();
    private final JButton pathBrowse = new JButton(__("Browse"));
    private final JCheckBox pathFolder = new JCheckBox(__("Is folder"));
    private final JTextField pathFormatter = new JTextField();
    // Language
    private final JTextField langDefault = new JTextField();
    private final JTextField langFormatter = new JTextField();
    // Window
    private final JTextField winFormatter = new JTextField();
    // Secret
    private final JTextField secFormatter = new JTextField();

    private static final String FORMATTER_TOOLTIP = "<html>"
            + __("Optional. Defines how the value is turned into command argument(s).")
            + "<br>" + __("Use {0} as a placeholder for the value, e.g. {1}.", "%VALUE", "<code>-m %VALUE</code>")
            + "<br>" + __("If the value is empty, the whole formatter is skipped (optional flags disappear).")
            + "<br>" + __("Without a formatter, the raw value is used as-is.")
            + "</html>";

    private RecipeParam param;
    private Recipe recipe;
    private boolean loading = false;
    private Runnable onKeyChanged;

    public JParamDetail() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        add(buildHeader(), BorderLayout.NORTH);
        add(cardHost, BorderLayout.CENTER);
        buildCards();
        for (JTextField f : new JTextField[]{tbFormatter, cbFormatter, pathFormatter, langFormatter, winFormatter, secFormatter})
            f.setToolTipText(FORMATTER_TOOLTIP);
        setParam(null);
    }

    public void setOnKeyChanged(Runnable r) {
        this.onKeyChanged = r;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        addRow(p, 0, __("Key:"), keyT);
        addRow(p, 1, __("Label:"), labelT);
        addRow(p, 2, __("Help:"), helpT);
        addRow(p, 3, __("Type:"), typeC);
        GridBagConstraints g = gbc(1, 4);
        g.gridwidth = 1;
        p.add(persistentB, g);
        addRow(p, 5, valueL, valueT);

        bindText(keyT, (par, v) -> {
            par.setKey(v);
            if (onKeyChanged != null)
                onKeyChanged.run();
        });
        bindText(labelT, RecipeParam::setLabel);
        bindText(helpT, RecipeParam::setHelp);
        typeC.addActionListener(e -> {
            if (loading || param == null)
                return;
            param.setType((RecipeParam.Type) typeC.getSelectedItem());
            showCard();
        });
        persistentB.addActionListener(e -> {
            if (!loading && param != null) {
                param.setPersistent(persistentB.isSelected());
                updateValueVisibility();
            }
        });
        valueT.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                storeValue();
            }

            public void removeUpdate(DocumentEvent e) {
                storeValue();
            }

            public void changedUpdate(DocumentEvent e) {
                storeValue();
            }
        });
        return p;
    }

    private void buildCards() {
        cardHost.setOpaque(false);

        JPanel tb = card();
        addRow(tb, 0, __("Default:"), tbDefault);
        addRow(tb, 1, __("Formatter:"), tbFormatter);
        cardHost.add(tb, RecipeParam.Type.TEXTBOX.name());
        bindText(tbDefault, RecipeParam::setDefaultValue);
        bindText(tbFormatter, RecipeParam::setFormatter);

        JPanel cb = card();
        addRow(cb, 0, __("Choices (| separated):"), cbChoices);
        addRow(cb, 1, __("Default:"), cbDefault);
        addRow(cb, 2, __("Formatter:"), cbFormatter);
        cardHost.add(cb, RecipeParam.Type.COMBOBOX.name());
        bindText(cbChoices, RecipeParam::setChoices);
        bindText(cbDefault, RecipeParam::setDefaultValue);
        bindText(cbFormatter, RecipeParam::setFormatter);

        JPanel chk = card();
        addRow(chk, 0, __("Value when checked:"), chkChecked);
        GridBagConstraints g = gbc(1, 1);
        chk.add(chkDefault, g);
        cardHost.add(chk, RecipeParam.Type.CHECKBOX.name());
        bindText(chkChecked, RecipeParam::setCheckedValue);
        chkDefault.addActionListener(e -> {
            if (!loading && param != null)
                param.setDefaultValue(Boolean.toString(chkDefault.isSelected()));
        });

        JPanel path = card();
        JPanel pathLine = new JPanel(new BorderLayout(4, 0));
        pathLine.setOpaque(false);
        pathLine.add(pathDefault, BorderLayout.CENTER);
        pathLine.add(pathBrowse, BorderLayout.EAST);
        addRow(path, 0, __("Default:"), pathLine);
        GridBagConstraints gf = gbc(1, 1);
        path.add(pathFolder, gf);
        addRow(path, 2, __("Formatter:"), pathFormatter);
        cardHost.add(path, RecipeParam.Type.PATH.name());
        bindText(pathDefault, RecipeParam::setDefaultValue);
        bindText(pathFormatter, RecipeParam::setFormatter);
        pathFolder.addActionListener(e -> {
            if (!loading && param != null)
                param.setFolder(pathFolder.isSelected());
        });
        pathBrowse.addActionListener(e -> browsePath());

        JPanel lang = card();
        addRow(lang, 0, __("Default (ISO code, e.g. en):"), langDefault);
        addRow(lang, 1, __("Formatter:"), langFormatter);
        cardHost.add(lang, RecipeParam.Type.LANGUAGE.name());
        bindText(langDefault, RecipeParam::setDefaultValue);
        bindText(langFormatter, RecipeParam::setFormatter);

        JPanel win = card();
        addRow(win, 0, __("Formatter:"), winFormatter);
        win.add(note(__("At run time: a dropdown of open subtitle windows; emits the %j path.")), gbc(1, 1));
        cardHost.add(win, RecipeParam.Type.WINDOW.name());
        bindText(winFormatter, RecipeParam::setFormatter);

        JPanel sec = card();
        addRow(sec, 0, __("Formatter:"), secFormatter);
        sec.add(note(__("Stored encrypted; a password field at run time; excluded from Save recipe.")), gbc(1, 1));
        cardHost.add(sec, RecipeParam.Type.SECRET.name());
        bindText(secFormatter, RecipeParam::setFormatter);
    }

    private void browsePath() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(pathFolder.isSelected() ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathDefault.setText(fc.getSelectedFile().getAbsolutePath());
            if (param != null)
                param.setDefaultValue(fc.getSelectedFile().getAbsolutePath());
        }
    }

    public void setParam(RecipeParam param) {
        this.param = param;
        loading = true;
        boolean active = param != null;
        setEnabledDeep(active);
        if (active) {
            keyT.setText(param.getKey());
            labelT.setText(param.getLabel());
            helpT.setText(param.getHelp());
            typeC.setSelectedItem(param.getType());
            persistentB.setSelected(param.isPersistent());
            tbDefault.setText(param.getDefaultValue());
            tbFormatter.setText(param.getFormatter());
            cbChoices.setText(param.getChoices());
            cbDefault.setText(param.getDefaultValue());
            cbFormatter.setText(param.getFormatter());
            chkChecked.setText(param.getCheckedValue());
            chkDefault.setSelected(Boolean.parseBoolean(param.getDefaultValue()));
            pathDefault.setText(param.getDefaultValue());
            pathFolder.setSelected(param.isFolder());
            pathFormatter.setText(param.getFormatter());
            langDefault.setText(param.getDefaultValue());
            langFormatter.setText(param.getFormatter());
            winFormatter.setText(param.getFormatter());
            secFormatter.setText(param.getFormatter());
            String stored = recipe == null ? null : recipe.getStoredValue(param.getKey());
            valueT.setText(stored == null ? "" : (param.isSecret() ? RecipeSecrets.decrypt(stored) : stored));
            updateValueVisibility();
            showCard();
        } else {
            for (JTextField t : new JTextField[]{keyT, labelT, helpT})
                t.setText("");
        }
        loading = false;
    }

    private void showCard() {
        if (param != null)
            cards.show(cardHost, param.getType().name());
    }

    private void updateValueVisibility() {
        boolean show = param != null && param.isPersistent();
        valueL.setVisible(show);
        valueT.setVisible(show);
    }

    private void storeValue() {
        if (loading || param == null || recipe == null || !param.isPersistent())
            return;
        String text = valueT.getText();
        recipe.setStoredValue(param.getKey(), param.isSecret() ? RecipeSecrets.encrypt(text) : text);
    }

    /* ===================== helpers ===================== */

    private void bindText(JTextField field, BiConsumer<RecipeParam, String> setter) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                if (!loading && param != null)
                    setter.accept(param, field.getText());
            }
        });
    }

    private void setEnabledDeep(boolean enabled) {
        java.awt.Component[] comps = {keyT, labelT, helpT, typeC, persistentB,
                tbDefault, tbFormatter, cbChoices, cbDefault, cbFormatter,
                chkChecked, chkDefault, pathDefault, pathBrowse, pathFolder, pathFormatter,
                langDefault, langFormatter, winFormatter, secFormatter};
        for (java.awt.Component c : comps)
            c.setEnabled(enabled);
    }

    private static JPanel card() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    private static JLabel note(String text) {
        JLabel l = new JLabel("<html><i>" + text + "</i></html>");
        return l;
    }

    private static void addRow(JPanel p, int row, String label, java.awt.Component field) {
        addRow(p, row, new JLabel(label), field);
    }

    private static void addRow(JPanel p, int row, JLabel label, java.awt.Component field) {
        GridBagConstraints lg = new GridBagConstraints();
        lg.gridx = 0;
        lg.gridy = row;
        lg.anchor = GridBagConstraints.LINE_END;
        lg.insets = new Insets(2, 4, 2, 4);
        p.add(label, lg);
        p.add(field, gbc(1, row));
    }

    private static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.anchor = GridBagConstraints.LINE_START;
        g.insets = new Insets(2, 4, 2, 4);
        return g;
    }
}
