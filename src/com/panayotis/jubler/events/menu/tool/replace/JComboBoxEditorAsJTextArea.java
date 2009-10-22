/*
 *  JComboBoxEditorAsJTextArea.java 
 * 
 *  Created on: 25-Sep-2009 at 19:21:03
 * 
 *  
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * This file is part of Jubler.
 * 
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 * 
 * 
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * Contributor(s):
 * 
 */
package com.panayotis.jubler.events.menu.tool.replace;

import java.awt.Component;
import java.awt.event.ActionListener;
import javax.swing.ComboBoxEditor;
import javax.swing.JTextArea;

/**
 * This class implements the combo-box editor as the JTextArea, in order
 * for it to handle control characters automatically, such as new-line,
 * tab etc..
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class JComboBoxEditorAsJTextArea implements ComboBoxEditor {

    protected JTextArea textArea = new JTextArea();

    /** Return the component that should be added to the tree hierarchy for
     * this editor
     */
    public Component getEditorComponent() {
        return textArea;
    }

    /** Set the item that should be edited. Cancel any editing if necessary **/
    public void setItem(Object anObject) {
        if (anObject != null) {
            String str = anObject.toString();
            textArea.setText(str);
        }//end if
    }

    /** Return the edited item **/
    public Object getItem() {
        String str = textArea.getText();
        return str;
    }

    /** Ask the editor to start editing and to select everything **/
    public void selectAll() {
        textArea.selectAll();
    }

    /** Add an ActionListener. An action event is generated when the edited item changes **/
    public void addActionListener(ActionListener l) {
    }

    /** Remove an ActionListener **/
    public void removeActionListener(ActionListener l) {
    }
}//end public class JComboBoxEditorAsJTextArea

