/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.time.gui;

import com.panayotis.jubler.time.Time;
import com.panayotis.jubler.time.TimeSpinnerEditor;
import com.panayotis.jubler.time.TimeSpinnerModel;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class JTimeSpinner extends JSpinner {
    public static final String NAVIGATION_EVENT = "NAVIGATION_EVENT";
    public static final int NEXT_LOCK = 0;
    public static final int PREVIOUS_LOCK = 1;
    public static final int NEXT_TIME_SPINNER = 2;

    private final TimeSpinnerEditor editor;
    private int dot = 7;

    /**
     * Creates a new instance of JTimeSpinner
     */
    public JTimeSpinner() {
        super();

        final TimeSpinnerModel model = new TimeSpinnerModel();
        setModel(model);

        editor = new TimeSpinnerEditor(this);
        editor.getTextField().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch ((e.getKeyCode())) {
                    case KeyEvent.VK_PAGE_UP:
                        firePropertyChange(NAVIGATION_EVENT, -1, PREVIOUS_LOCK);
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        firePropertyChange(NAVIGATION_EVENT, -1, NEXT_LOCK);
                        break;
                    case KeyEvent.VK_ENTER:
                        firePropertyChange(NAVIGATION_EVENT, -1, NEXT_TIME_SPINNER);
                        break;
                }
            }
        });
        editor.getTextField().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        editor.getTextField().setCaretPosition(dot);
                    }
                });
            }
        });
        editor.getTextField().addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                if (e.getDot() == 0 || e.getDot() == 12)
                    return;
                switch (dot = e.getDot()) {
                    case 1:
                        model.setSpeed(3600);
                        break;
                    case 3:
                        model.setSpeed(600);
                        break;
                    case 4:
                        model.setSpeed(60);
                        break;
                    case 6:
                        model.setSpeed(10);
                        break;
                    case 7:
                        model.setSpeed(1);
                        break;
                    case 9:
                        model.setSpeed(0.1);
                        break;
                    case 10:
                        model.setSpeed(0.01);
                        break;
                    case 11:
                        model.setSpeed(0.001);
                        break;
                }
            }
        });
        setEditor(editor);
    }

    public Time getTimeValue() {
        return (Time) getModel().getValue();
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        editor.getTextField().requestFocus();
    }

    public void setTimeValue(Time t) {
        getModel().setValue(t);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                editor.getTextField().setCaretPosition(dot);
            }
        });
    }
}
