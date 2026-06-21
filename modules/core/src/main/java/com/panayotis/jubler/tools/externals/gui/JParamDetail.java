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
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Detail editor for a single {@link RecipeParam}: common header (key/label/help/type/
 * persistent) plus a type-specific card. Edits write straight into the bound param.
 */
public class JParamDetail extends JPanel {

    // A bounded preferred width so a long help/default value can't stretch the whole dialog;
    // the fields still fill/grow horizontally when the user resizes the window.
    private static final int FIELD_COLUMNS = 32;
    private final JTextField keyT = new JTextField(FIELD_COLUMNS);
    private final JTextField labelT = new JTextField(FIELD_COLUMNS);
    private final JTextField helpT = new JTextField(FIELD_COLUMNS);
    private final JComboBox<RecipeParam.Type> typeC = new JComboBox<>(RecipeParam.Type.values());
    private final JCheckBox persistentB = new JCheckBox(__("Persistent"));
    private final JPasswordField valueT = new JPasswordField(FIELD_COLUMNS);
    private final char defaultEcho = valueT.getEchoChar();
    private final JToggleButton revealBtn = SecretField.revealToggle(valueT);
    private InfoButton valueInfo;

    private final CardLayout cards = new CardLayout();
    private final JPanel cardHost = new JPanel(cards);

    /** Outer cards: an empty placeholder (nothing selected) and the real detail. */
    private final CardLayout outer = new CardLayout();

    // TextBox
    private final JTextField tbDefault = new JTextField(FIELD_COLUMNS);
    // ComboBox
    private final JTextField cbChoices = new JTextField(FIELD_COLUMNS);
    private final JTextField cbDefault = new JTextField(FIELD_COLUMNS);
    // CheckBox
    private final JTextField chkChecked = new JTextField(FIELD_COLUMNS);
    private final JCheckBox chkDefault = new JCheckBox(__("Checked by default"));
    // Path
    private final JTextField pathDefault = new JTextField(FIELD_COLUMNS);
    private final JButton pathBrowse = new JButton(__("Browse"));
    private final JCheckBox pathFolder = new JCheckBox(__("Is folder"));
    // Language
    private final JTextField langDefault = new JTextField(FIELD_COLUMNS);
    // Video subtitle
    private final JComboBox<String> vsAccept = new JComboBox<>(new String[]{"any", "text", "image"});
    private final JComboBox<String> vsField = new JComboBox<>(new String[]{"index", "id", "language"});

    /** Every row label and the panels holding them, so all GridBag layouts can share one label-column width. */
    private final List<JLabel> formLabels = new ArrayList<>();
    private final Set<JPanel> formPanels = new LinkedHashSet<>();

    private RecipeParam param;
    private Recipe recipe;
    private boolean loading = false;
    private boolean secretDirty = false;
    private Runnable onKeyChanged;

    public JParamDetail() {
        setLayout(outer);
        JPanel stack = new JPanel(new BorderLayout(0, 6));
        stack.setOpaque(false);
        stack.add(buildHeader(), BorderLayout.NORTH);
        stack.add(cardHost, BorderLayout.CENTER);
        buildCards();
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        content.add(stack, BorderLayout.NORTH);
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        add(empty, "empty");
        add(content, "content");
        alignFormLabels();
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
        addRow(p, 0, __("Key:"), keyT, new InfoButton(__("Key"),
                __("Internal identifier used in the command as %&lt;key&gt;. Must start with a letter and contain only letters and digits (at least 2 characters), and be unique within the recipe.")));
        addRow(p, 1, __("Label:"), labelT, new InfoButton(__("Label"),
                __("The name shown to the user next to this field when the recipe runs. If left empty, the key is used.")));
        addRow(p, 2, __("Help:"), helpT, new InfoButton(__("Help"),
                __("Extra explanation shown to the user (behind an info button) next to this field when the recipe runs.")));
        addRow(p, 3, __("Type:"), typeC, new InfoButton(__("Type"), this::typeHelp));
        // The "Persistent" checkbox doubles as the label for the stored value: check it to keep a permanent
        // value (asked once, not on every run); the field on the right holds that value.
        GridBagConstraints pg = new GridBagConstraints();
        pg.gridx = 0;
        pg.gridy = 4;
        pg.anchor = GridBagConstraints.LINE_END;
        pg.insets = new Insets(2, 4, 2, 4);
        p.add(persistentB, pg);
        JPanel valuePanel = new JPanel(new BorderLayout(4, 0));
        valuePanel.setOpaque(false);
        valuePanel.add(valueT, BorderLayout.CENTER);
        valuePanel.add(revealBtn, BorderLayout.EAST);   // eye toggle, shown only for secrets
        p.add(valuePanel, gbc(1, 4));
        valueInfo = new InfoButton(__("Persistent"), this::valueHelp);
        p.add(valueInfo, infoGbc(4));

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
            RecipeParam.Type oldType = param.getType();
            RecipeParam.Type newType = (RecipeParam.Type) typeC.getSelectedItem();
            if (newType == oldType)
                return;
            // If secret-ness changed, re-encode the stored value (plain<->encrypted) before switching.
            boolean secretnessChanged = (oldType == RecipeParam.Type.SECRET) != (newType == RecipeParam.Type.SECRET);
            if (secretnessChanged && param.isPersistent()
                    && !RecipeSecrets.recodeForSecretChange(recipe, param.getKey(), newType == RecipeParam.Type.SECRET)) {
                loading = true;                       // could not convert -> revert the dropdown, keep old type/value
                typeC.setSelectedItem(oldType);
                loading = false;
                return;
            }
            param.setType(newType);
            reloadValueField();
            updateSecretMask();
            showCard();
        });
        persistentB.addActionListener(e -> {
            if (!loading && param != null) {
                param.setPersistent(persistentB.isSelected());
                updateValueEnabled();
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
        // Secrets use an expensive PBKDF2 encrypt; commit once on focus loss, not on every keystroke.
        valueT.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                commitSecret();
            }
        });
        return p;
    }

    private void buildCards() {
        cardHost.setOpaque(false);

        JPanel tb = card();
        addRow(tb, 0, __("Default:"), tbDefault);
        cardHost.add(tb, RecipeParam.Type.TEXTBOX.name());
        bindText(tbDefault, RecipeParam::setDefaultValue);

        JPanel cb = card();
        addRow(cb, 0, __("Choices:"), cbChoices, new InfoButton(__("Choices"),
                __("The list of options offered to the user, separated by | (for example: tiny|base|small).")));
        addRow(cb, 1, __("Default:"), cbDefault);
        cardHost.add(cb, RecipeParam.Type.COMBOBOX.name());
        bindText(cbChoices, RecipeParam::setChoices);
        bindText(cbDefault, RecipeParam::setDefaultValue);

        JPanel chk = card();
        addRow(chk, 0, __("Value:"), chkChecked, new InfoButton(__("Value when checked"),
                __("The text added to the command line when the box is checked. Nothing is added when it is unchecked.")));
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
        path.add(new InfoButton(__("Is folder"),
                __("If on, the Browse button lets the user pick a folder instead of a file.")), infoGbc(1));
        cardHost.add(path, RecipeParam.Type.PATH.name());
        bindText(pathDefault, RecipeParam::setDefaultValue);
        pathFolder.addActionListener(e -> {
            if (!loading && param != null)
                param.setFolder(pathFolder.isSelected());
        });
        pathBrowse.addActionListener(e -> browsePath());

        JPanel lang = card();
        addRow(lang, 0, __("ISO default:"), langDefault, new InfoButton(__("ISO default"),
                __("The pre-selected language, as a 2-letter ISO 639 code (for example en, fr, de).")));
        cardHost.add(lang, RecipeParam.Type.LANGUAGE.name());
        bindText(langDefault, RecipeParam::setDefaultValue);

        cardHost.add(card(), RecipeParam.Type.WINDOW.name());

        JPanel vs = card();
        addRow(vs, 0, __("Show:"), vsAccept, new InfoButton(__("Show"),
                __("Which streams to list: only text subtitles (convertible to text), only image subtitles (PGS/DVD, for OCR tools), or all of them.")));
        addRow(vs, 1, __("Emit:"), vsField, new InfoButton(__("Emit"),
                __("Which property of the chosen subtitle stream is passed to the tool: its index (0,1,2… for ffmpeg -map 0:s:N), its container id (for mkvextract), or its language code.")));
        cardHost.add(vs, RecipeParam.Type.VIDEO_SUBTITLE.name());
        vsAccept.addActionListener(e -> {
            if (!loading && param != null)
                param.setAccept((String) vsAccept.getSelectedItem());
        });
        vsField.addActionListener(e -> {
            if (!loading && param != null)
                param.setField((String) vsField.getSelectedItem());
        });

        cardHost.add(card(), RecipeParam.Type.SECRET.name());
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
        commitSecret();          // flush the previous param's pending secret before switching
        this.param = param;
        secretDirty = false;
        loading = true;
        boolean active = param != null;
        outer.show(this, active ? "content" : "empty");
        if (active) {
            keyT.setText(param.getKey());
            labelT.setText(param.getLabel());
            helpT.setText(param.getHelp());
            typeC.setSelectedItem(param.getType());
            persistentB.setSelected(param.isPersistent());
            tbDefault.setText(param.getDefaultValue());
            cbChoices.setText(param.getChoices());
            cbDefault.setText(param.getDefaultValue());
            chkChecked.setText(param.getCheckedValue());
            chkDefault.setSelected(Boolean.parseBoolean(param.getDefaultValue()));
            pathDefault.setText(param.getDefaultValue());
            pathFolder.setSelected(param.isFolder());
            langDefault.setText(param.getDefaultValue());
            vsAccept.setSelectedItem(param.getAccept());
            vsField.setSelectedItem(param.getField());
            String stored = recipe == null ? null : recipe.getStoredValue(param.getKey());
            valueT.setText(stored == null ? "" : (param.isSecret() ? RecipeSecrets.decrypt(stored) : stored));
            updateValueEnabled();
            updateSecretMask();
            showCard();
            // setText leaves the caret at the end, scrolling long values out of view; show the start.
            for (JTextField f : new JTextField[]{keyT, labelT, helpT, valueT, tbDefault,
                    cbChoices, cbDefault, chkChecked, pathDefault, langDefault})
                f.setCaretPosition(0);
        }
        loading = false;
    }

    /** General type help, then a blank line, then the description of the currently selected type. */
    private String typeHelp() {
        String general = __("The kind of input shown to the user at run time (text, choice, checkbox, path, language, window or secret).");
        Object sel = typeC.getSelectedItem();
        if (sel instanceof RecipeParam.Type) {
            RecipeParam.Type t = (RecipeParam.Type) sel;
            return general + "<br><br><b>" + t.getLabel() + ":</b> " + t.getDescription();
        }
        return general;
    }

    /** What "Persistent" + the stored value mean, then a blank line, then the part that differs by secret/non-secret. */
    private String valueHelp() {
        String general = __("If on, the value is set once here in the configuration and is not asked on every run (e.g. an API key or a default). If off, the user is asked each run.");
        boolean secret = param != null && param.isSecret();
        String specific = secret
                ? __("Because this is a secret, it is stored encrypted and is never included when you save or share the recipe.")
                : __("It is stored as plain text and is included when you save or share the recipe.");
        return general + "<br><br>" + specific;
    }

    private void showCard() {
        if (param != null)
            cards.show(cardHost, param.getType().name());
    }

    /** The stored value field is editable only when "Persistent" is checked (the info button stays available). */
    private void updateValueEnabled() {
        valueT.setEnabled(param != null && param.isPersistent());
    }

    /** Re-read the stored value into the field after a type change (decrypting if it is now a secret). */
    private void reloadValueField() {
        loading = true;
        String stored = recipe == null ? null : recipe.getStoredValue(param.getKey());
        valueT.setText(stored == null ? "" : (param.isSecret() ? RecipeSecrets.decrypt(stored) : stored));
        loading = false;
    }

    /** Mask the stored value (and show the eye toggle) only for Secret params; plain text for everything else. */
    private void updateSecretMask() {
        boolean secret = param != null && param.isSecret();
        revealBtn.setVisible(secret);
        revealBtn.setSelected(false);   // start masked whenever the param/type changes
        valueT.setEchoChar(secret ? defaultEcho : (char) 0);
    }

    /**
     * Make the header and every card share one label-column width: set the GridBag column-0 minimum
     * to the widest label. The labels keep their natural size and stay right-aligned (LINE_END), so the
     * fields sit right after each label as usual — only the column is shared so everything lines up.
     */
    private void alignFormLabels() {
        int max = 0;
        for (JLabel l : formLabels)
            max = Math.max(max, l.getPreferredSize().width);
        for (JPanel p : formPanels)
            ((GridBagLayout) p.getLayout()).columnWidths = new int[]{max, 0, 0};
    }

    private void storeValue() {
        if (loading || param == null || recipe == null || !param.isPersistent())
            return;
        if (param.isSecret()) {
            secretDirty = true;   // defer the costly encrypt to focus loss / flush
            return;
        }
        recipe.setStoredValue(param.getKey(), new String(valueT.getPassword()));
    }

    /** Encrypt and store the secret value once (called on focus loss and before leaving the param). */
    private void commitSecret() {
        if (loading || !secretDirty || param == null || recipe == null || !param.isPersistent() || !param.isSecret())
            return;
        recipe.setStoredValue(param.getKey(), RecipeSecrets.encrypt(new String(valueT.getPassword())));
        secretDirty = false;
    }

    /** Flush any pending secret edit (call before the editor accepts/closes). */
    public void flush() {
        commitSecret();
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

    private static JPanel card() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    private void addRow(JPanel p, int row, String label, java.awt.Component field) {
        addRow(p, row, new JLabel(label), field, null);
    }

    private void addRow(JPanel p, int row, String label, java.awt.Component field, InfoButton info) {
        addRow(p, row, new JLabel(label), field, info);
    }

    private void addRow(JPanel p, int row, JLabel label, java.awt.Component field, InfoButton info) {
        formLabels.add(label);
        formPanels.add(p);
        GridBagConstraints lg = new GridBagConstraints();
        lg.gridx = 0;
        lg.gridy = row;
        lg.anchor = GridBagConstraints.LINE_END;
        lg.insets = new Insets(2, 4, 2, 4);
        p.add(label, lg);
        p.add(field, gbc(1, row));
        if (info != null)
            p.add(info, infoGbc(row));
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

    private static GridBagConstraints infoGbc(int row) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 2;
        g.gridy = row;
        g.fill = GridBagConstraints.VERTICAL;
        g.anchor = GridBagConstraints.LINE_START;
        g.insets = new Insets(2, 0, 2, 2);
        return g;
    }
}
