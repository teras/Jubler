/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.tools;

import javax.swing.JPanel;
import com.panayotis.jubler.os.SystemDependent;
import javax.swing.JOptionPane;

import static com.panayotis.jubler.i18n.I18N.__;

public class JRegExpReplaceGUI extends JPanel {

    private final JRegExpReplace tool;

    /**
     * Creates new form JRegExpReplace
     *
     * @param tool
     */
    public JRegExpReplaceGUI(JRegExpReplace tool) {
        initComponents();
        this.tool = tool;
        TextList.setListData(tool.getRlist().getModel().getReplaceList());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        TextList = new javax.swing.JList();
        EditB = new javax.swing.JButton();

        setBorder(SystemDependent.getBorder(__("Regular expressions to be executed")));
        setOpaque(false);
        setLayout(new java.awt.BorderLayout());

        jScrollPane1.setPreferredSize(new java.awt.Dimension(259, 80));

        TextList.setToolTipText(__("List of replacements to be done"));
        jScrollPane1.setViewportView(TextList);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);

        EditB.setText(__("Edit"));
        EditB.setActionCommand("Edit");
        EditB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditBActionPerformed(evt);
            }
        });
        add(EditB, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    private void EditBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditBActionPerformed
        int ret;
        Object[] options = {__("Use"), __("Cancel"), __("Reset")};
        ret = JOptionPane.showOptionDialog(this, tool.getRlist(), __("Edit regular expression replace list"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        switch (ret) {
            case 0:
                //do
                tool.getRlist().getModel().saveOptions();
                break;
            case 1:
            case JOptionPane.CLOSED_OPTION:
                //cancel
                tool.getRlist().getModel().loadOptions();
                break;
            case 2:
                // reset
                tool.getRlist().getModel().reset();
                tool.getRlist().getModel().saveOptions();
                break;
        }
        TextList.setListData(tool.getRlist().getModel().getReplaceList());
    }//GEN-LAST:event_EditBActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton EditB;
    private javax.swing.JList TextList;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
