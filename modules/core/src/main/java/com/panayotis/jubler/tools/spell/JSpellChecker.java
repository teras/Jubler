/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.spell;

import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.theme.Theme;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * The spell-check dialog: walks the misspellings one by one, showing each in its sentence context with
 * suggested replacements. A single "apply to all occurrences" toggle collapses the old ignore/ignore-all
 * and replace/replace-all button pairs into two buttons.
 */
public class JSpellChecker extends JDialog {

    private final JFrame jparent;
    private SpellChecker checker;
    private List<SubEntry> textlist;
    private int pos_in_list = -1;
    private int count_changes = 0;
    private List<String> ignored;
    private Map<String, String> replaced;
    private List<SpellError> errors;   // null until a successful start(); findNextWord() then no-ops
    private String current;

    private JTextPane contextPane;
    private JLabel wordLabel;
    private JTextField replaceField;
    private JList<String> sugList;
    private JCheckBox allBox;
    private JButton addButton;
    private JButton replaceButton;

    public JSpellChecker(JFrame parent, SpellChecker checker, List<SubEntry> list) {
        super(parent, __("Check spelling"), true);
        this.jparent = parent;

        try {
            checker.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    __("Unable to start spell checker:\n{0}", describe(ex)),
                    __("Spell Checker Error"), JOptionPane.ERROR_MESSAGE);
            checker.stop();
            return;   // errors stays null -> findNextWord() is a no-op, the dialog is never shown
        }

        this.checker = checker;
        this.textlist = list;
        this.errors = new ArrayList<>();
        this.ignored = new ArrayList<>();
        this.replaced = new HashMap<>();

        buildUI();
        if (!checker.supportsInsert())
            addButton.setVisible(false);
    }

    /* ===================== walking the misspellings ===================== */

    public void findNextWord() {
        if (errors == null)   // start() failed; nothing to do
            return;

        if (!errors.isEmpty())
            errors.remove(0);
        updateKnownErrors();

        while (errors.isEmpty() && ((++pos_in_list) < textlist.size())) {
            try {
                errors = checker.checkSpelling(textlist.get(pos_in_list).getText());
                updateKnownErrors();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        __("Error while checking spelling:\n{0}", describe(ex)),
                        __("Spell Checker Error"), JOptionPane.ERROR_MESSAGE);
                stop();
                return;
            }
        }
        if (errors.isEmpty()) {
            stop();
            return;
        }

        SpellError mistake = errors.get(0);
        current = mistake.original;
        wordLabel.setText(mistake.original);
        sugList.setListData(mistake.alternatives.toArray(new String[0]));
        setSentence(textlist.get(pos_in_list).getText().replace('\n', '|'), mistake.position, mistake.original.length());

        if (sugList.getModel().getSize() > 0)
            sugList.setSelectedIndex(0);
        else
            replaceField.setText(mistake.original);

        setVisible(true);
    }

    /* Drop from the current error list the words the user already chose to ignore or replace. */
    private void updateKnownErrors() {
        for (int i = errors.size() - 1; i >= 0; i--) {
            String original = errors.get(i).original;
            if (ignored.contains(original))
                errors.remove(i);
            else if (replaced.containsKey(original)) {
                count_changes++;
                replaceText(replaced.get(original), i);
                errors.remove(i);
            }
        }
    }

    private void setSentence(String txt, int pos, int len) {
        contextPane.setText(txt);
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.ColorConstants.Foreground, Color.RED);
        StyleConstants.setBold(set, true);
        contextPane.getStyledDocument().setCharacterAttributes(pos, len, set, true);
    }

    private void useSuggestedWord() {
        int which = sugList.getSelectedIndex();
        if (which < 0) {
            if (sugList.getModel().getSize() > 0)
                sugList.setSelectedIndex(0);
            return;
        }
        replaceField.setText(sugList.getModel().getElementAt(which));
    }

    private void replaceText(String txt, int index) {
        int pos = errors.get(index).position;
        int len = errors.get(index).original.length();

        String olds = textlist.get(pos_in_list).getText();
        String news = olds.substring(0, pos) + txt + olds.substring(pos + len);
        textlist.get(pos_in_list).setText(news);

        int dlength = txt.length() - len;   // propagate the size change to the errors that follow
        for (int i = index + 1; i < errors.size(); i++)
            errors.get(i).position += dlength;
    }

    /* ===================== actions ===================== */

    private void onIgnore() {
        if (allBox.isSelected())
            ignored.add(current);
        findNextWord();
    }

    private void onReplace() {
        replaceText(replaceField.getText(), 0);
        count_changes++;
        if (allBox.isSelected())
            replaced.put(current, replaceField.getText());
        findNextWord();
    }

    private void onAddToDictionary() {
        checker.insertWord(current);
        findNextWord();
    }

    private void stop() {
        if (checker != null)
            checker.stop();
        if (isVisible()) {
            setVisible(false);
            dispose();
        }
        String msg = count_changes == 0
                ? __("No changes have been done")
                : __("Number of affected words: {0}", count_changes);
        JIDialog.info(jparent, msg, __("Speller changes"));
    }

    private static String describe(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = ex.getClass().getSimpleName();
            if (ex.getCause() != null)
                msg += ": " + ex.getCause().getMessage();
        }
        return msg;
    }

    /* ===================== UI ===================== */

    private void buildUI() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(buildSuggestions(), BorderLayout.CENTER);
        content.add(buildActions(), BorderLayout.SOUTH);

        setContentPane(content);
        setMinimumSize(new Dimension(460, 460));
        pack();
        setLocationRelativeTo(jparent);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        contextPane = new JTextPane();
        contextPane.setEditable(false);
        contextPane.setFocusable(false);
        contextPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        contextPane.setToolTipText(__("The context of the misspelled word"));
        contextPane.setAlignmentX(LEFT_ALIGNMENT);
        header.add(contextPane);
        header.add(Box.createVerticalStrut(12));

        JPanel wordRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        wordRow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel icon = new JLabel(Theme.loadIcon("spellcheck"));
        wordRow.add(icon);
        JPanel wordText = new JPanel();
        wordText.setLayout(new BoxLayout(wordText, BoxLayout.Y_AXIS));
        wordText.add(new JLabel(__("Not in dictionary")));
        wordLabel = new JLabel(" ");
        wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, wordLabel.getFont().getSize2D() + 3f));
        wordText.add(wordLabel);
        wordRow.add(wordText);
        header.add(wordRow);

        header.add(Box.createVerticalStrut(12));

        JPanel replaceRow = new JPanel(new BorderLayout(8, 0));
        replaceRow.setAlignmentX(LEFT_ALIGNMENT);
        replaceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        replaceRow.add(new JLabel(__("Replace with")), BorderLayout.WEST);
        replaceField = new JTextField();
        replaceField.setToolTipText(__("The word to change the misspelled word into"));
        replaceRow.add(replaceField, BorderLayout.CENTER);
        header.add(replaceRow);

        return header;
    }

    private JComponent buildSuggestions() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel(__("Suggestions")), BorderLayout.NORTH);

        sugList = new JList<>();
        sugList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sugList.setToolTipText(__("Suggested words to change the given word to"));
        sugList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                useSuggestedWord();
        });
        sugList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2)
                    onReplace();
            }
        });
        panel.add(new JScrollPane(sugList), BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildActions() {
        JPanel south = new JPanel(new BorderLayout(0, 10));

        allBox = new JCheckBox(__("Apply to all occurrences of this word"));
        south.add(allBox, BorderLayout.NORTH);

        JPanel bar = new JPanel(new BorderLayout());
        addButton = new JButton(__("Add to dictionary"));
        addButton.setToolTipText(__("Add this word to the speller's dictionary"));
        addButton.addActionListener(e -> onAddToDictionary());
        bar.add(addButton, BorderLayout.WEST);

        JButton ignoreButton = new JButton(__("Ignore"));
        ignoreButton.setToolTipText(__("Skip this word"));
        ignoreButton.addActionListener(e -> onIgnore());

        replaceButton = new JButton(__("Replace"));
        replaceButton.setToolTipText(__("Replace this word with the text above"));
        replaceButton.addActionListener(e -> onReplace());

        JButton doneButton = new JButton(__("Done"));
        doneButton.setToolTipText(__("Finish spell checking"));
        doneButton.addActionListener(e -> stop());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.add(ignoreButton);
        right.add(replaceButton);
        right.add(doneButton);
        bar.add(right, BorderLayout.EAST);

        south.add(bar, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(replaceButton);
        return south;
    }
}
