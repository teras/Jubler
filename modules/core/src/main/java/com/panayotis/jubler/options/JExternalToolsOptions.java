/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.JublerPrefs;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.loader.SubFormat;
import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.tools.ToolsManager;
import com.panayotis.jubler.tools.externals.ExternalTool;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.panayotis.jubler.i18n.I18N.__;

public class JExternalToolsOptions extends JPanel implements OptionsHolder {
    private static final JFileChooser chooser = new JFileChooser();
    private static final ExternalToolList tools = new ExternalToolList();
    private ExternalTool current;

    static {
        loadTools();
    }

    private static void loadTools() {
        tools.clear();
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String prefix = "external.tools.tool" + i + ".";
            String name = JublerPrefs.getString(prefix + "name", null);
            String path = JublerPrefs.getString(prefix + "path", null);
            String command = JublerPrefs.getString(prefix + "command", null);
            String format = JublerPrefs.getString(prefix + "format", null);
            if (name == null || path == null || command == null)
                break;
            tools.add(new ExternalTool(name, path, command, format));
        }
    }

    private static final String COMMAND_TOOLTIP = __("Advanced argument list:") + "\n" +
            __("  %x : executable") + "\n" +
            __("  %i : input subtitle file") + "\n" +
            __("  %o : output subtitle file") + "\n" +
            "\n" +
            __("If %o is missing, then %i will be considered as output file");

    /**
     * Creates new form JExternalToolsOptions
     */
    public JExternalToolsOptions() {
        initComponents();

        SubFormat[] formats = Availabilities.formats.getFormats().toArray(new SubFormat[0]);
        typelistC.setModel(new DefaultComboBoxModel<>(formats));
        typelistC.setSelectedIndex(-1);
        toolsL.setModel(tools);
        addListener(nameT, value -> current.setName(value));
        addListener(commandT, value -> current.setCommand(value));
        chooser.setDialogTitle("Please select external tool path");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
    }

    @Override
    public void loadPreferences() {
        loadTools();
    }

    @Override
    public void savePreferences() {
        for (int i = 0; i < tools.getSize(); i++) {
            String prefix = "external.tools.tool" + (i + 1) + ".";
            ExternalTool tool = tools.getElementAt(i);
            JublerPrefs.set(prefix + "name", tool.getName());
            JublerPrefs.set(prefix + "path", tool.getPath());
            JublerPrefs.set(prefix + "command", tool.getCommand());
            JublerPrefs.set(prefix + "format", tool.getFormat().getClass().getName());
        }
        String next = "external.tools.tool" + (tools.getSize() + 1) + ".";
        JublerPrefs.set(next + "name", null);
        JublerPrefs.set(next + "path", null);
        JublerPrefs.set(next + "command", null);
        JublerPrefs.set(next + "format", null);
        ToolsManager.updateExternals();
    }

    @Override
    public JPanel getTabPanel() {
        return this;
    }

    @Override
    public String getTabName() {
        return "Externals";
    }

    @Override
    public String getTabTooltip() {
        return "Configure external tools";
    }

    @Override
    public Icon getTabIcon() {
        return Theme.loadIcon("externals");
    }

    @Override
    public void changeProgram() {
    }

    private void setCurrent(ExternalTool tool) {
        boolean active = tool != null;
        nameT.setEnabled(active);
        browseB.setEnabled(active);
        commandT.setEnabled(active);
        typelistC.setEnabled(active);

        current = null; // also to cut cycle events
        if (active) {
            nameT.setText(tool.getName());
            pathT.setText(tool.getPath());
            commandT.setText(tool.getCommand());
            toolsL.setSelectedValue(tool, true);
            typelistC.setSelectedItem(tool.getFormat());
            current = tool;
        } else {
            nameT.setText("");
            pathT.setText("");
            commandT.setText("");
            toolsL.setSelectedValue(null, true);
            typelistC.setSelectedIndex(-1);
        }
    }

    private void addListener(final JTextField field, final Consumer<String> cb) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                anyUpdate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                anyUpdate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                anyUpdate();
            }

            private void anyUpdate() {
                if (current != null) {
                    cb.accept(field.getText());
                    tools.update(current);
                }
            }
        });
    }

    public static Iterable<ExternalTool> getList() {
        return tools.getList();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jScrollPane1 = new javax.swing.JScrollPane();
        toolsL = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        addB = new javax.swing.JButton();
        removeB = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        nameL = new javax.swing.JLabel();
        browseL = new javax.swing.JLabel();
        commandL = new javax.swing.JLabel();
        typelistL = new javax.swing.JLabel();
        nameT = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        browseB = new javax.swing.JButton();
        pathT = new javax.swing.JTextField();
        commandT = new javax.swing.JTextField();
        typelistC = new javax.swing.JComboBox<>();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        toolsL.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                toolsLValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(toolsL);

        add(jScrollPane1);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        addB.setText("+ " + __("Add"));
        addB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBActionPerformed(evt);
            }
        });
        jPanel3.add(addB);

        removeB.setText("- " + __("Remove"));
        removeB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeBActionPerformed(evt);
            }
        });
        jPanel3.add(removeB);

        add(jPanel3);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        nameL.setText(__("Tool name"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(nameL, gridBagConstraints);

        browseL.setText(__("Executable path"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(browseL, gridBagConstraints);

        commandL.setText(__("Command"));
        commandL.setToolTipText(COMMAND_TOOLTIP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(commandL, gridBagConstraints);

        typelistL.setText(__("Subtitle type"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(typelistL, gridBagConstraints);

        nameT.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(nameT, gridBagConstraints);

        jPanel1.setLayout(new java.awt.BorderLayout());

        browseB.setText(__("Browse"));
        browseB.setEnabled(false);
        browseB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseBActionPerformed(evt);
            }
        });
        jPanel1.add(browseB, java.awt.BorderLayout.EAST);

        pathT.setEditable(false);
        jPanel1.add(pathT, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(jPanel1, gridBagConstraints);

        commandT.setToolTipText(COMMAND_TOOLTIP);
        commandT.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(commandT, gridBagConstraints);

        typelistC.setEnabled(false);
        typelistC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                typelistCActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 0, 4);
        jPanel2.add(typelistC, gridBagConstraints);

        add(jPanel2);
    }// </editor-fold>//GEN-END:initComponents

    private void addBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addBActionPerformed
        ExternalTool newTool = new ExternalTool();
        tools.add(newTool);
        setCurrent(newTool);
        nameT.requestFocus();
    }//GEN-LAST:event_addBActionPerformed

    private void removeBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeBActionPerformed
        tools.remove(current);
        setCurrent(null);
    }//GEN-LAST:event_removeBActionPerformed

    private void browseBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseBActionPerformed
        chooser.showOpenDialog(this);
        File selectedFile = chooser.getSelectedFile();
        if (selectedFile != null && selectedFile.isFile()) {
            if (current != null) {
                String path = selectedFile.getAbsolutePath();
                current.setPath(path);
                pathT.setText(path);
                tools.update(current);
            }
        }
    }//GEN-LAST:event_browseBActionPerformed

    private void toolsLValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_toolsLValueChanged
        ExternalTool tool = tools.getElementAt(toolsL.getSelectedIndex());
        if (tool != current)
            setCurrent(tool);
    }//GEN-LAST:event_toolsLValueChanged

    private void typelistCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_typelistCActionPerformed
        if (current != null)
            current.setFormat((SubFormat) typelistC.getSelectedItem());
    }//GEN-LAST:event_typelistCActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addB;
    private javax.swing.JButton browseB;
    private javax.swing.JLabel browseL;
    private javax.swing.JLabel commandL;
    private javax.swing.JTextField commandT;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel nameL;
    private javax.swing.JTextField nameT;
    private javax.swing.JTextField pathT;
    private javax.swing.JButton removeB;
    private javax.swing.JList<ExternalTool> toolsL;
    private javax.swing.JComboBox<SubFormat> typelistC;
    private javax.swing.JLabel typelistL;
    // End of variables declaration//GEN-END:variables
}

class ExternalToolList extends AbstractListModel<ExternalTool> {
    private final List<ExternalTool> tools = new ArrayList<>();

    @Override
    public int getSize() {
        return tools.size();
    }

    @Override
    public ExternalTool getElementAt(int index) {
        return index >= 0 && index < getSize() ? tools.get(index) : null;
    }

    public void add(ExternalTool tool) {
        tools.add(tool);
        fireIntervalAdded(this, tools.size() - 1, tools.size() - 1);
    }

    public void remove(ExternalTool tool) {
        int index = tools.indexOf(tool);
        if (index >= 0) {
            tools.remove(index);
            fireContentsChanged(this, index, index);
        }
    }

    public void update(ExternalTool tool) {
        int index = tools.indexOf(tool);
        if (index >= 0)
            fireContentsChanged(this, index, index);
    }

    public Iterable<ExternalTool> getList() {
        return tools;
    }

    public void clear() {
        if (!tools.isEmpty()) {
            fireIntervalRemoved(this, 0, tools.size() - 1);
            tools.clear();
        }
    }
}