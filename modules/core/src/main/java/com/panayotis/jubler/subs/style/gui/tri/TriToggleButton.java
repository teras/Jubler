/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.style.gui.tri;

import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.StyleType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

//import static com.panayotis.jubler.i18n.I18N._;
public class TriToggleButton extends JToggleButton implements TriObject {

    private ImageIcon on, off;
    private int state = 0;

    /**
     * Creates a new instance of JTriButton
     */
    public TriToggleButton(String iconname) {
        on = Theme.loadIcon(iconname);
        off = DarkIconFilter.getDisabledIcon(on);
        setState(0);

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (ignore_element_changes)
                    return;
                setState((++state) % 2);
                if (listener != null)
                    listener.changeStyle(styletype, isSelected());
            }
        });
    }
    private StyleType styletype;
    private StyleChangeListener listener;

    public void setStyle(StyleType style) {
        styletype = style;
    }

    public void setListener(StyleChangeListener listener) {
        this.listener = listener;
    }

    public void setData(Object data) {
        if (data == null) {
            setState(2);
            return;
        }
        setState((((Boolean) data).booleanValue()) ? 1 : 0);
    }
    private boolean ignore_element_changes = false;

    public void setState(int s) {
        ignore_element_changes = true;
        state = s;
        if (state == 0) {
            setSelected(false);
            setIcon(on);
        } else if (state == 1) {
            setSelected(true);
            setIcon(on);
        } else {
            setIcon(off);
            setSelected(true);
        }
        ignore_element_changes = false;
    }

    public int getState() {
        return state;
    }
}
