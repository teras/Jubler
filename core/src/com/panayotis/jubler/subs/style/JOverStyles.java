/*
 * JOverStyles.java
 *
 * Created on 7 Σεπτέμβριος 2005, 1:04 μμ
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

package com.panayotis.jubler.subs.style;

import static com.panayotis.jubler.i18n.I18N.__;
import static com.panayotis.jubler.subs.style.StyleType.*;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.subs.style.event.AbstractStyleover;
import com.panayotis.jubler.subs.style.gui.AlphaColor;
import com.panayotis.jubler.subs.style.gui.JAlphaIcon;
import com.panayotis.jubler.subs.style.gui.tri.TriToggleButton;
import com.panayotis.jubler.subs.style.gui.tri.TriColorButton;
import com.panayotis.jubler.subs.style.gui.tri.TriComboBox;
import com.panayotis.jubler.subs.style.gui.tri.TriDirectionButton;
import com.panayotis.jubler.subs.style.gui.tri.TriDummy;
import com.panayotis.jubler.subs.style.gui.tri.TriObject;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.AbstractButton;
import javax.swing.JComponent;

/**
 *
 * @author teras
 */
public class JOverStyles extends javax.swing.JPanel {

    private JubFrame parent;
    /* live button icons */
    private JAlphaIcon PrimaryI, SecondaryI, OutlineI, ShadowI;
    private TriObject[] visuals;

    /**
     * Creates new form JOverStyles
     */
    public JOverStyles(JubFrame parent) {
        initComponents();

        visuals = new TriObject[StyleType.values().length];

        FontAttP.add((JComponent) (visuals[0] = new TriComboBox(SubStyle.FontNames)), BorderLayout.CENTER);
        ((TriComboBox) visuals[0]).setToolTipText(__("Font name"));
        FontAttP.add((JComponent) (visuals[1] = new TriComboBox(SubStyle.FontSizes)), BorderLayout.EAST);
        ((TriComboBox) visuals[1]).setToolTipText(__("Font size"));

        TextAttP.add((JComponent) (visuals[2] = new TriToggleButton("bold.png")));
        ((AbstractButton) visuals[2]).setToolTipText(__("Bold"));
        SystemDependent.setCommandButtonStyle((AbstractButton) visuals[2], "first");

        TextAttP.add((JComponent) (visuals[3] = new TriToggleButton("italics.png")));
        SystemDependent.setCommandButtonStyle((AbstractButton) visuals[3], "middle");
        ((AbstractButton) visuals[3]).setToolTipText(__("Italic"));

        TextAttP.add((JComponent) (visuals[4] = new TriToggleButton("underline.png")));
        SystemDependent.setCommandButtonStyle((AbstractButton) visuals[4], "middle");
        ((AbstractButton) visuals[4]).setToolTipText(__("Underline"));

        TextAttP.add((JComponent) (visuals[5] = new TriToggleButton("strike.png")));
        SystemDependent.setCommandButtonStyle((AbstractButton) visuals[5], "last");
        ((AbstractButton) visuals[5]).setToolTipText(__("Strikethrough"));


        ColorP.add((JComponent) (visuals[6] = new TriColorButton(new AlphaColor(Color.WHITE, 180), parent)));
        ColorP.add((JComponent) (visuals[7] = new TriColorButton(new AlphaColor(Color.WHITE, 180), parent)));
        ColorP.add((JComponent) (visuals[8] = new TriColorButton(new AlphaColor(Color.WHITE, 180), parent)));
        ColorP.add((JComponent) (visuals[9] = new TriColorButton(new AlphaColor(Color.WHITE, 180), parent)));
        for (int i = 6; i < 10; i++) {
            SystemDependent.setColorButtonStyle((AbstractButton) visuals[i], "only");
            ((AbstractButton) visuals[i]).setToolTipText(__(TriColorButton.tooltips[i - 6]));
        }


        for (int i = 10; i < visuals.length; i++)
            visuals[i] = new TriDummy();

        TextAttP.add((JComponent) (visuals[DIRECTION.ordinal()] = new TriDirectionButton(parent)));
        SystemDependent.setToolBarButtonStyle((AbstractButton) visuals[DIRECTION.ordinal()], "only");
        ((AbstractButton) visuals[DIRECTION.ordinal()]).setToolTipText(__("Alignment"));

        for (int i = 0; i < visuals.length; i++)
            ((TriObject) visuals[i]).setStyle(StyleType.values()[i]);

        FontP.setVisible(false);
        ColorP.setVisible(false);

        this.parent = parent;
    }

    public void setStyleChangeListener(StyleChangeListener listener) {
        for (int i = 0; i < visuals.length; i++)
            ((TriObject) visuals[i]).setListener(listener);
    }

    public void setPanelVisible(String what, boolean isVisible) {
        if (what.endsWith("font"))
            FontP.setVisible(isVisible);
        if (what.endsWith("color"))
            ColorP.setVisible(isVisible);
        validate();
    }

    public void updateVisualData(SubStyle style, AbstractStyleover[] over, int start, int end, String subtext) {
        for (int i = 0; i < visuals.length; i++) {
            Object basic = style.get(i);
            Object data;
            if (over == null || over[i] == null)
                data = basic;
            else
                data = over[i].getValue(start, end, basic, subtext);
            visuals[i].setData(data);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        FontP = new javax.swing.JPanel();
        FontAttP = new javax.swing.JPanel();
        TextAttP = new javax.swing.JPanel();
        ColorP = new javax.swing.JPanel();

        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        FontP.setOpaque(false);
        FontP.setLayout(new java.awt.BorderLayout());

        FontAttP.setOpaque(false);
        FontAttP.setLayout(new java.awt.BorderLayout());
        FontP.add(FontAttP, java.awt.BorderLayout.CENTER);

        TextAttP.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 6, 0, 0));
        TextAttP.setOpaque(false);
        TextAttP.setLayout(new java.awt.GridLayout(1, 0));
        FontP.add(TextAttP, java.awt.BorderLayout.EAST);

        add(FontP);

        ColorP.setOpaque(false);
        ColorP.setLayout(new java.awt.GridLayout(1, 0, 2, 0));
        add(ColorP);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel ColorP;
    private javax.swing.JPanel FontAttP;
    private javax.swing.JPanel FontP;
    private javax.swing.JPanel TextAttP;
    // End of variables declaration//GEN-END:variables
}
