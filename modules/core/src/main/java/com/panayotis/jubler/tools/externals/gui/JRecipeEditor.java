/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals.gui;

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.tools.externals.OutputMode;
import com.panayotis.jubler.tools.externals.Recipe;
import com.panayotis.jubler.tools.externals.RecipeParam;
import com.panayotis.jubler.tools.externals.RecipeResolver;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Single-window recipe editor: a top zone (name, executable + status, command, wire
 * format, result mode) and a Parameters master-detail (list + {@link JParamDetail}).
 * In-process recipes show a read-only module badge and hide the command fields.
 */
public class JRecipeEditor extends JDialog {

    private final Recipe recipe;
    private final Recipe snapshot;
    private boolean accepted = false;

    private final JTextField nameT = new JTextField();
    private final JTextField pathT = new JTextField();
    private final JButton browseB = new JButton(__("Browse"));
    private final JLabel statusL = new JLabel(" ");
    private final JButton infoB = new JButton(__("Install info"));
    private final JTextField commandT = new JTextField();
    private final JComboBox<SubFormat> formatC = new JComboBox<>();
    private final JComboBox<OutputMode> resultC = new JComboBox<>(OutputMode.values());

    private final DefaultListModel<RecipeParam> paramModel = new DefaultListModel<>();
    private final JList<RecipeParam> paramList = new JList<>(paramModel);
    private final JParamDetail detail = new JParamDetail();

    public JRecipeEditor(Window parent, Recipe recipe) {
        super(parent, __("Edit recipe"), ModalityType.APPLICATION_MODAL);
        this.recipe = recipe;
        this.snapshot = Recipe.fromJsonString(recipe.toJsonString(false));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(buildTop(), BorderLayout.NORTH);
        content.add(buildParams(), BorderLayout.CENTER);
        content.add(buildButtons(), BorderLayout.SOUTH);
        setContentPane(content);

        loadFromRecipe();
        pack();
        setMinimumSize(new Dimension(640, getHeight()));
        setLocationRelativeTo(parent);
    }

    /** @return true if the user accepted the edits; false if cancelled (recipe restored). */
    public boolean showEditor() {
        setVisible(true);
        return accepted;
    }

    /* ===================== top zone ===================== */

    private JPanel buildTop() {
        JPanel p = new JPanel(new GridBagLayout());
        int row = 0;
        addRow(p, row++, __("Name:"), nameT);

        if (recipe.isInProcess()) {
            JLabel badge = new JLabel(__("In-process module: {0}", recipe.getModule()));
            badge.setFont(badge.getFont().deriveFont(Font.ITALIC));
            addRow(p, row++, __("Type:"), badge);
        } else {
            JPanel execLine = new JPanel(new BorderLayout(4, 0));
            execLine.setOpaque(false);
            execLine.add(pathT, BorderLayout.CENTER);
            execLine.add(browseB, BorderLayout.EAST);
            addRow(p, row++, __("Executable:"), execLine);

            JPanel statusLine = new JPanel(new BorderLayout(6, 0));
            statusLine.setOpaque(false);
            statusLine.add(statusL, BorderLayout.CENTER);
            statusLine.add(infoB, BorderLayout.EAST);
            addRow(p, row++, "", statusLine);

            commandT.setToolTipText("<html>%x " + __("executable") + " · %i " + __("input")
                    + " · %j " + __("second window") + " · %a " + __("audio") + " · %v " + __("video")
                    + " · %o " + __("output") + "</html>");
            addRow(p, row++, __("Command:"), commandT);
        }

        addRow(p, row++, __("Wire format:"), formatC);
        addRow(p, row++, __("Result:"), resultC);

        SubFormat[] formats = Availabilities.formats.getFormats().toArray(new SubFormat[0]);
        for (SubFormat f : formats)
            formatC.addItem(f);
        formatC.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean foc) {
                super.getListCellRendererComponent(list, value, index, sel, foc);
                if (value instanceof SubFormat)
                    setText(((SubFormat) value).getName() + " (." + ((SubFormat) value).getExtension() + ")");
                return this;
            }
        });

        bindText(nameT, recipe::setName);
        bindText(pathT, v -> {
            recipe.setPath(v);
            updateStatus();
        });
        bindText(commandT, recipe::setCommand);
        browseB.addActionListener(e -> browseExecutable());
        infoB.addActionListener(e -> showInstallInfo());
        formatC.addActionListener(e -> {
            Object f = formatC.getSelectedItem();
            if (f instanceof SubFormat)
                recipe.setFormat((SubFormat) f);
        });
        resultC.addActionListener(e -> recipe.setOutputMode((OutputMode) resultC.getSelectedItem()));
        return p;
    }

    /* ===================== parameters master-detail ===================== */

    private JPanel buildParams() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBorder(BorderFactory.createTitledBorder(__("Parameters")));

        detail.setRecipe(recipe);
        paramList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paramList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                detail.setParam(paramList.getSelectedValue());
        });
        detail.setOnKeyChanged(paramList::repaint);

        JScrollPane listScroll = new JScrollPane(paramList);
        listScroll.setPreferredSize(new Dimension(180, 200));

        JButton addB = small("+");
        JButton remB = small("−");
        JButton upB = small("∧");
        JButton dnB = small("∨");
        addB.addActionListener(e -> addParam());
        remB.addActionListener(e -> removeParam());
        upB.addActionListener(e -> moveParam(-1));
        dnB.addActionListener(e -> moveParam(1));
        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        listButtons.add(addB);
        listButtons.add(remB);
        listButtons.add(upB);
        listButtons.add(dnB);

        JPanel left = new JPanel(new BorderLayout());
        left.add(listScroll, BorderLayout.CENTER);
        left.add(listButtons, BorderLayout.SOUTH);

        p.add(left, BorderLayout.WEST);
        p.add(detail, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton(__("Cancel"));
        JButton ok = new JButton(__("OK"));
        cancel.addActionListener(e -> {
            recipe.copyFrom(snapshot);
            accepted = false;
            dispose();
        });
        ok.addActionListener(e -> {
            String error = validateRecipe();
            if (error != null) {
                JIDialog.error(this, error, __("Invalid recipe"));
                return;
            }
            accepted = true;
            dispose();
        });
        p.add(cancel);
        p.add(ok);
        return p;
    }

    /* ===================== actions ===================== */

    private void addParam() {
        RecipeParam p = new RecipeParam(uniqueKey(), RecipeParam.Type.TEXTBOX);
        recipe.addParam(p);
        paramModel.addElement(p);
        paramList.setSelectedValue(p, true);
    }

    private void removeParam() {
        int idx = paramList.getSelectedIndex();
        if (idx < 0)
            return;
        recipe.removeParam(paramModel.get(idx));
        paramModel.remove(idx);
        if (paramModel.getSize() > 0)
            paramList.setSelectedIndex(Math.min(idx, paramModel.getSize() - 1));
        else
            detail.setParam(null);
    }

    private void moveParam(int delta) {
        int idx = paramList.getSelectedIndex();
        int to = idx + delta;
        if (idx < 0 || to < 0 || to >= paramModel.getSize())
            return;
        RecipeParam p = paramModel.get(idx);
        paramModel.remove(idx);
        paramModel.add(to, p);
        recipe.getParams().remove(idx);
        recipe.getParams().add(to, p);
        paramList.setSelectedIndex(to);
    }

    private String uniqueKey() {
        for (int i = 1; ; i++) {
            String candidate = "param" + i;
            if (!recipe.keysExcept(null).contains(candidate))
                return candidate;
        }
    }

    private void browseExecutable() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(__("Select executable"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathT.setText(fc.getSelectedFile().getAbsolutePath());
            recipe.setPath(fc.getSelectedFile().getAbsolutePath());
            updateStatus();
        }
    }

    private void showInstallInfo() {
        String info = recipe.getInstallInfo();
        JIDialog.info(this, info.isEmpty() ? __("No install information provided for this recipe.") : info,
                __("How to install"));
    }

    private void updateStatus() {
        if (recipe.isInProcess()) {
            statusL.setText(" ");
            infoB.setVisible(false);
            return;
        }
        boolean ok = RecipeResolver.isAvailable(recipe);
        statusL.setForeground(ok ? new Color(0, 128, 0) : Color.RED);
        statusL.setText(ok ? __("Found") : "⚠ " + __("Executable not found"));
        infoB.setVisible(!ok);
    }

    private String validateRecipe() {
        if (recipe.getName().trim().isEmpty())
            return __("The recipe needs a name.");
        for (RecipeParam p : recipe.getParams()) {
            String err = RecipeParam.validateKey(p.getKey(), recipe.keysExcept(p));
            if (err != null)
                return __("Parameter \"{0}\": {1}", p.getLabel(), err);
        }
        return null;
    }

    /* ===================== load ===================== */

    private void loadFromRecipe() {
        nameT.setText(recipe.getName());
        pathT.setText(recipe.getPath());
        commandT.setText(recipe.getCommand());
        if (recipe.getFormat() != null)
            formatC.setSelectedItem(recipe.getFormat());
        resultC.setSelectedItem(recipe.getOutputMode());
        paramModel.clear();
        for (RecipeParam p : recipe.getParams())
            paramModel.addElement(p);
        if (!paramModel.isEmpty())
            paramList.setSelectedIndex(0);
        updateStatus();
    }

    /* ===================== helpers ===================== */

    private void bindText(JTextField field, java.util.function.Consumer<String> setter) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                setter.accept(field.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                setter.accept(field.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                setter.accept(field.getText());
            }
        });
    }

    private static JButton small(String text) {
        JButton b = new JButton(text);
        b.setMargin(new Insets(0, 6, 0, 6));
        return b;
    }

    private static void addRow(JPanel p, int row, String label, Component field) {
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
    }
}
