/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui.tri;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.StyleType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;

public class TriComboBox extends JComboBox implements TriObject {

    /**
     * Creates a new instance of TriComboBox
     */
    public TriComboBox(Object[] values) {
        super();
        for (Object data : values)
            addItem(data);
        addItem(__("Unspecified"));
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (ignore_element_changes)
                    return;
                Object val = getSelectedItem();
                if (val.equals(__("Unspecified")))
                    return;
                if (listener != null)
                    listener.changeStyle(styletype, getSelectedItem());
            }
        });
    }
    private boolean ignore_element_changes = false;

    public void setData(Object data) {
        ignore_element_changes = true;
        if (data == null)
            setSelectedItem(__("Unspecified"));
        else
            setSelectedItem(data);
        ignore_element_changes = false;
    }
    protected StyleType styletype;
    protected StyleChangeListener listener;

    public void setStyle(StyleType style) {
        styletype = style;
    }

    public void setListener(StyleChangeListener listener) {
        this.listener = listener;
    }
}
