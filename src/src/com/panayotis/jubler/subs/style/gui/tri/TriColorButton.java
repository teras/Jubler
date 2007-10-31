/*
 * TriColorButton.java
 *
 * Created on 14 Σεπτέμβριος 2005, 1:50 πμ
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
 */

package com.panayotis.jubler.subs.style.gui.tri;

import static com.panayotis.jubler.i18n.I18N._;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.subs.style.StyleChangeListener;
import com.panayotis.jubler.subs.style.StyleType;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import com.panayotis.jubler.subs.style.gui.JAlphaColorDialog;
import com.panayotis.jubler.subs.style.gui.JAlphaIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

/**
 *
 * @author teras
 */
public class TriColorButton extends JButton implements TriObject {
    
    private JAlphaIcon icon;
    private JAlphaColorDialog colordialog;
    
    public final static String [] labels = {_("Primary"), _("Secondary"), _("Outline"), _("Shadow")};
    
    public final static String []  tooltips = {_("Set the primary color of the style"),
            _("Set the secondary color of the style"),
            _("Set the outline color of the style"),
            _("Set the shadow (or the background) color of the style")
    };
    
    
    public TriColorButton(AlphaColor c, Jubler parent) {
        
        icon = new JAlphaIcon(c);
        setIcon(icon);
        colordialog = new JAlphaColorDialog(parent);
        
        addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent evt) {
                if (ignore_element_changes) return;
                colordialog.setAlphaColor(icon.getAlphaColor());
                colordialog.setVisible(true);
                AlphaColor newc = colordialog.getAlphaColor();
                if (newc!= null)
                    icon.setAlphaColor(newc);
                if (listener!=null) listener.changeStyle(styletype, icon.getAlphaColor());
            }
        });
    }
    
    
    private StyleType styletype;
    private StyleChangeListener listener;
    public void setListener(StyleChangeListener listener) { this.listener = listener; }
    
    
    public void setStyle(StyleType style) {
        styletype = style;
        setText( labels[style.ordinal()-StyleType.PRIMARY.ordinal()] );
        setToolTipText( tooltips[style.ordinal()-StyleType.PRIMARY.ordinal()] );
    }
    
    private boolean ignore_element_changes = false;
    public void setData(Object data) {
        ignore_element_changes = true;
        icon.setAlphaColor((AlphaColor)data);
        repaint();
        ignore_element_changes = false;
    }
    
}
