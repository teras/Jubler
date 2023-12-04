/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.options;

import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.tools.ToolsManager;
import com.panayotis.jubler.tools.externals.ExternalTool;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * @author teras
 */
public class JExternalToolsOptions extends JPanel implements OptionsHolder {
    private static final JFileChooser chooser = new JFileChooser();
    public static final ExternalToolList tools = new ExternalToolList();
    private ExternalTool current;

    static {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String prefix = "External.tools.tool" + (i++) + ".";
            String name = Options.getOption(prefix + "name", null);
            String path = Options.getOption(prefix + "path", null);
            String command = Options.getOption(prefix + "command", null);
            boolean inplace = Boolean.parseBoolean(Options.getOption(prefix + "inplace", "false"));
            if (name == null || path == null || command == null)
                break;
            tools.add(new ExternalTool(name, path, command, inplace));
        }
    }

    /**
     * Creates new form JExternalToolsOptions
     */
    public JExternalToolsOptions() {
        initComponents();
        toolsL.setModel(tools);
        addListener(nameT, new CallBack() {
            @Override
            public void exec(String value) {
                current.setName(value);
            }
        });
        addListener(commandT, new CallBack() {
            @Override
            public void exec(String value) {
                current.setCommand(value);
            }
        });
        chooser.setDialogTitle("Please select external tool path");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
    }

    @Override
    public void loadPreferences() {
    }

    @Override
    public void savePreferences() {
        for (int i = 0; i < tools.getSize(); i++) {
            String prefix = "External.tools.tool" + (i + 1) + ".";
            ExternalTool tool = tools.getElementAt(i);
            Options.setOption(prefix + "name", tool.getName());
            Options.setOption(prefix + "path", tool.getPath());
            Options.setOption(prefix + "command", tool.getCommand());
            Options.setOption("inpace", String.valueOf(tool.isInplace()));
        }
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
        return Theme.loadIcon("externals.png");
    }

    @Override
    public void changeProgram() {
    }

    private void setCurrent(ExternalTool tool) {
        boolean active = tool != null;
        nameT.setEditable(active);
        commandT.setEditable(active);
        browseB.setEnabled(active);
        current = null; // also to cut cycle events

        if (active) {
            nameT.setText(tool.getName());
            pathT.setText(tool.getPath());
            commandT.setText(tool.getCommand());
            toolsL.setSelectedValue(tool, true);
            inplaceC.setSelected(tool.isInplace());
            current = tool;
        }
    }

    private void addListener(final JTextField field, final CallBack cb) {
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
                    cb.exec(field.getText());
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

        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        toolsL = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        addB = new javax.swing.JButton();
        removeB = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 8), new java.awt.Dimension(0, 8), new java.awt.Dimension(0, 8));
        jPanel5 = new javax.swing.JPanel();
        nameT = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        browseB = new javax.swing.JButton();
        pathT = new javax.swing.JTextField();
        browseL = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        commandT = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        inplaceC = new javax.swing.JCheckBox();

        setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        toolsL.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                toolsLValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(toolsL);

        jPanel2.add(jScrollPane1);

        jPanel3.setLayout(new java.awt.FlowLayout(0));

        addB.setText("+");
        addB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addBActionPerformed(evt);
            }
        });
        jPanel3.add(addB);

        removeB.setText("-");
        removeB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeBActionPerformed(evt);
            }
        });
        jPanel3.add(removeB);

        jPanel2.add(jPanel3);
        jPanel2.add(filler1);

        jPanel5.setLayout(new java.awt.BorderLayout());
        jPanel5.add(nameT, java.awt.BorderLayout.CENTER);

        jLabel2.setText(__("Name"));
        jPanel5.add(jLabel2, java.awt.BorderLayout.NORTH);

        jPanel2.add(jPanel5);

        jPanel1.setLayout(new java.awt.BorderLayout());

        browseB.setText(__("Browse"));
        browseB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseBActionPerformed(evt);
            }
        });
        jPanel1.add(browseB, java.awt.BorderLayout.EAST);

        pathT.setEditable(false);
        jPanel1.add(pathT, java.awt.BorderLayout.CENTER);

        browseL.setText(__("Path"));
        jPanel1.add(browseL, java.awt.BorderLayout.NORTH);

        jPanel2.add(jPanel1);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jLabel1.setText(__("Command"));
        jPanel6.add(jLabel1, java.awt.BorderLayout.NORTH);

        commandT.setToolTipText("<html>" + __("Advanced argument list:") + "<br>\n" +
                __("%x=executable") + "<br>\n" +
                __("%s=subtitle file"));
        jPanel6.add(commandT, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel6);

        jPanel4.setLayout(new java.awt.BorderLayout());

        inplaceC.setText(__("In-place file subtitution"));
        inplaceC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inplaceCActionPerformed(evt);
            }
        });
        jPanel4.add(inplaceC, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel4);

        add(jPanel2, java.awt.BorderLayout.NORTH);
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

    private void inplaceCActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inplaceCActionPerformed
        if (current != null)
            current.setInplace(inplaceC.isSelected());
    }//GEN-LAST:event_inplaceCActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addB;
    private javax.swing.JButton browseB;
    private javax.swing.JLabel browseL;
    private javax.swing.JTextField commandT;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JCheckBox inplaceC;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField nameT;
    private javax.swing.JTextField pathT;
    private javax.swing.JButton removeB;
    private javax.swing.JList<ExternalTool> toolsL;
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
}

interface CallBack {
    void exec(String value);
}