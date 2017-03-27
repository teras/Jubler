/*
 * JDirection.java
 *
 * Created on 7 Σεπτέμβριος 2005, 12:34 μμ
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

package com.panayotis.jubler.subs.style.gui;

import com.panayotis.jubler.os.SystemDependent;
import static com.panayotis.jubler.i18n.I18N.__;
import com.panayotis.jubler.plugins.Theme;

import com.panayotis.jubler.subs.style.SubStyle.Direction;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JToggleButton;

/**
 *
 * @author teras
 */
public class JDirection extends javax.swing.JPanel {

    private DirectionListener listener;

    /**
     * Creates new form JDirection
     */
    public JDirection() {
        initComponents();
    }

    public Direction getDirection() {
        if (Top.isSelected())
            return Direction.TOP;
        if (TopRight.isSelected())
            return Direction.TOPRIGHT;
        if (Right.isSelected())
            return Direction.RIGHT;
        if (BottomRight.isSelected())
            return Direction.BOTTOMRIGHT;
        if (Bottom.isSelected())
            return Direction.BOTTOM;
        if (BottomLeft.isSelected())
            return Direction.BOTTOMLEFT;
        if (Left.isSelected())
            return Direction.LEFT;
        if (TopLeft.isSelected())
            return Direction.TOPLEFT;
        return Direction.CENTER;
    }

    public void setDirection(Direction d) {
        if (d == null)
            return;
        switch (d) {
            case TOP:
                Top.setSelected(true);
                break;
            case TOPRIGHT:
                TopRight.setSelected(true);
                break;
            case RIGHT:
                Right.setSelected(true);
                break;
            case BOTTOMRIGHT:
                BottomRight.setSelected(true);
                break;
            case BOTTOM:
                Bottom.setSelected(true);
                break;
            case BOTTOMLEFT:
                BottomLeft.setSelected(true);
                break;
            case LEFT:
                Left.setSelected(true);
                break;
            case TOPLEFT:
                TopLeft.setSelected(true);
                break;
            default:
                Center.setSelected(true);
                break;
        }
    }

    public boolean requestFocusInWindow() {
        return getDirectionButton().requestFocusInWindow();
    }

    private JToggleButton getDirectionButton() {
        if (Top.isSelected())
            return Top;
        if (TopRight.isSelected())
            return TopRight;
        if (Right.isSelected())
            return Right;
        if (BottomRight.isSelected())
            return BottomRight;
        if (Bottom.isSelected())
            return Bottom;
        if (BottomLeft.isSelected())
            return BottomLeft;
        if (Left.isSelected())
            return Left;
        if (TopLeft.isSelected())
            return TopLeft;
        return Center;
    }

    public void setListener(DirectionListener l) {
        listener = l;
    }

    public int getControlX() {
        if (Top.isSelected())
            return 1;
        if (TopRight.isSelected())
            return 2;
        if (Right.isSelected())
            return 2;
        if (BottomRight.isSelected())
            return 2;
        if (Bottom.isSelected())
            return 1;
        if (BottomLeft.isSelected())
            return 0;
        if (Left.isSelected())
            return 0;
        if (TopLeft.isSelected())
            return 0;
        return 1;
    }

    public int getControlY() {
        if (Top.isSelected())
            return 0;
        if (TopRight.isSelected())
            return 0;
        if (Right.isSelected())
            return 1;
        if (BottomRight.isSelected())
            return 2;
        if (Bottom.isSelected())
            return 2;
        if (BottomLeft.isSelected())
            return 2;
        if (Left.isSelected())
            return 1;
        if (TopLeft.isSelected())
            return 0;
        return 1;
    }

    public Icon getIcon() {
        return getDirectionButton().getIcon();
    }

    public Icon getIcon(Direction d) {
        switch (d) {
            case TOP:
                return Top.getIcon();
            case TOPRIGHT:
                return TopRight.getIcon();
            case RIGHT:
                return Right.getIcon();
            case BOTTOMRIGHT:
                return BottomRight.getIcon();
            case BOTTOM:
                return Bottom.getIcon();
            case BOTTOMLEFT:
                return BottomLeft.getIcon();
            case LEFT:
                return Left.getIcon();
            case TOPLEFT:
                return TopLeft.getIcon();
            default:
                return Center.getIcon();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        AlignGroup = new javax.swing.ButtonGroup();
        TopLeft = new javax.swing.JToggleButton();
        Top = new javax.swing.JToggleButton();
        TopRight = new javax.swing.JToggleButton();
        Left = new javax.swing.JToggleButton();
        Center = new javax.swing.JToggleButton();
        Right = new javax.swing.JToggleButton();
        BottomLeft = new javax.swing.JToggleButton();
        Bottom = new javax.swing.JToggleButton();
        BottomRight = new javax.swing.JToggleButton();

        setLayout(new java.awt.GridLayout(3, 3));

        AlignGroup.add(TopLeft);
        TopLeft.setIcon(Theme.loadIcon("upleft.png"));
        TopLeft.setToolTipText(__("Top left"));
        SystemDependent.setDirectionButtonStyle(TopLeft);
        TopLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        TopLeft.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(TopLeft);

        AlignGroup.add(Top);
        Top.setIcon(Theme.loadIcon("up.png"));
        Top.setToolTipText(__("Top"));
        SystemDependent.setDirectionButtonStyle(Top);
        Top.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        Top.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(Top);

        AlignGroup.add(TopRight);
        TopRight.setIcon(Theme.loadIcon("upright.png"));
        TopRight.setToolTipText(__("Top right"));
        SystemDependent.setDirectionButtonStyle(TopRight);
        TopRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        TopRight.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(TopRight);

        AlignGroup.add(Left);
        Left.setIcon(Theme.loadIcon("left.png"));
        Left.setToolTipText(__("Left"));
        SystemDependent.setDirectionButtonStyle(Left);
        Left.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        Left.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(Left);

        AlignGroup.add(Center);
        Center.setIcon(Theme.loadIcon("center.png"));
        Center.setToolTipText(__("Center"));
        SystemDependent.setDirectionButtonStyle(Center);
        Center.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        Center.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(Center);

        AlignGroup.add(Right);
        Right.setIcon(Theme.loadIcon("right.png"));
        Right.setToolTipText(__("Right"));
        SystemDependent.setDirectionButtonStyle(Right);
        Right.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        Right.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(Right);

        AlignGroup.add(BottomLeft);
        BottomLeft.setIcon(Theme.loadIcon("downleft.png"));
        BottomLeft.setToolTipText(__("Bottom left"));
        SystemDependent.setDirectionButtonStyle(BottomLeft);
        BottomLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        BottomLeft.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(BottomLeft);

        SystemDependent.setDirectionButtonStyle(Bottom);
        AlignGroup.add(Bottom);
        Bottom.setIcon(Theme.loadIcon("down.png"));
        Bottom.setToolTipText(__("Bottom"));
        Bottom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        Bottom.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(Bottom);

        SystemDependent.setDirectionButtonStyle(BottomRight);
        AlignGroup.add(BottomRight);
        BottomRight.setIcon(Theme.loadIcon("downright.png"));
        BottomRight.setToolTipText(__("Bottom right"));
        BottomRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                optionClicked(evt);
            }
        });
        BottomRight.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                JDirection.this.focusLost(evt);
            }
        });
        add(BottomRight);
    }// </editor-fold>//GEN-END:initComponents

    private void focusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_focusLost
        Component luckyb = evt.getOppositeComponent();
        if (luckyb == TopLeft || luckyb == Top || luckyb == TopRight
                || luckyb == Left || luckyb == Center || luckyb == Right
                || luckyb == BottomLeft || luckyb == Bottom || luckyb == BottomRight)
            return;
        if (listener != null)
            listener.focusLost();
    }//GEN-LAST:event_focusLost

    private void optionClicked(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionClicked
        if (listener != null)
            listener.directionUpdated();
    }//GEN-LAST:event_optionClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup AlignGroup;
    private javax.swing.JToggleButton Bottom;
    private javax.swing.JToggleButton BottomLeft;
    private javax.swing.JToggleButton BottomRight;
    private javax.swing.JToggleButton Center;
    private javax.swing.JToggleButton Left;
    private javax.swing.JToggleButton Right;
    private javax.swing.JToggleButton Top;
    private javax.swing.JToggleButton TopLeft;
    private javax.swing.JToggleButton TopRight;
    // End of variables declaration//GEN-END:variables
}
