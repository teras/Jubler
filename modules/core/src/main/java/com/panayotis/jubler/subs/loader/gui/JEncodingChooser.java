/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.gui;

import static com.panayotis.jubler.i18n.I18N.__;

import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.theme.Theme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * Reusable encoding picker: a charset combo plus a "presets" button that opens a region-grouped
 * menu of the common encodings. This is the single home of the control that used to live inside the
 * load/save dialogs (as ~900 lines of generated NetBeans code); it is now built data-driven from the
 * category tables below. Region and sub-menu labels are wrapped in the translation call with string
 * literals inline so the i18n extractor still sees them.
 */
public class JEncodingChooser extends JPanel {

    private static final String[] AVAILABLE = Charset.availableCharsets().keySet().toArray(new String[0]);

    private final JComboBox<String> combo = new JComboBox<>(AVAILABLE);
    private final JPopupMenu presets = new JPopupMenu();
    private Consumer<String> listener;
    private boolean updating;

    public JEncodingChooser() {
        super(new BorderLayout());
        buildPresets();

        JButton presetsButton = new JButton(Theme.loadIcon("encs"));
        presetsButton.setToolTipText(__("Use predefined encodings"));
        SystemDependent.setCommandButtonStyle(presetsButton, "only");
        presetsButton.addActionListener(e -> presets.show(presetsButton, 0, presetsButton.getHeight()));

        combo.addActionListener(e -> {
            if (updating || listener == null)
                return;
            Object sel = combo.getSelectedItem();
            if (sel != null)
                listener.accept(sel.toString());
        });

        add(combo, BorderLayout.CENTER);
        add(presetsButton, BorderLayout.EAST);
    }

    /** Notified with the charset name whenever the user changes the selection. */
    public void setChangeListener(Consumer<String> listener) {
        this.listener = listener;
    }

    public String getEncoding() {
        Object sel = combo.getSelectedItem();
        return sel == null ? null : sel.toString();
    }

    /** Show the given charset without firing the change listener. */
    public void setEncoding(String enc) {
        updating = true;
        selectInCombo(enc);
        updating = false;
    }

    private void selectInCombo(String enc) {
        combo.setSelectedItem(enc);
        Object sel = combo.getSelectedItem();
        if (sel == null || !sel.toString().equals(enc))
            combo.setSelectedItem("US-ASCII");
    }

    /* Spec → real charset name. Most Mac* charsets (plus Johab and windows-874) are registered under
     * an "x-" alias; the rest use the spec verbatim (which may already be an "x-..." canonical name). */
    private static String toCharset(String name) {
        if (name.startsWith("Mac") || name.equals("Johab") || name.endsWith("874"))
            return "x-" + name;
        return name;
    }

    /* What the user sees: the real charset name without the internal "x-" alias prefix, capitalized. */
    private static String displayLabel(String charset) {
        String s = charset.startsWith("x-") ? charset.substring(2) : charset;
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void buildPresets() {
        JMenu unicode = new JMenu(__("Unicode"));
        addItems(unicode, "UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE", "UTF-32", "UTF-32LE", "UTF-32BE");
        presets.add(unicode);
        presets.addSeparator();

        JMenu ew = new JMenu(__("West European"));
        ew.add(sub(__("Western"), "ISO-8859-1", "ISO-8859-15", "windows-1252", "IBM850", "MacRoman"));
        ew.add(sub(__("Greek"), "ISO-8859-7", "windows-1253", "MacGreek"));
        ew.add(sub(__("Icelandic"), "MacIceland"));
        ew.add(sub(__("South European"), "ISO-8859-3"));
        presets.add(ew);

        JMenu ee = new JMenu(__("East European"));
        ee.add(sub(__("Baltic"), "ISO-8859-4", "ISO-8859-13", "windows-1257"));
        ee.add(sub(__("Central European"), "ISO-8859-2", "windows-1250", "IBM852", "MacCentralEurope"));
        ee.add(sub(__("Croatian"), "MacCroatian"));
        ee.add(sub(__("Cyrillic"), "ISO-8859-5", "windows-1251", "IBM855", "IBM866", "MacCyrillic", "MacUkraine", "KOI8-R", "KOI8-U"));
        ee.add(sub(__("Romanian"), "ISO-8859-16", "MacRomania"));
        presets.add(ee);

        JMenu ae = new JMenu(__("East Asian"));
        ae.add(sub(__("Chinese Simplified"), "ISO-2022-CN", "GB2312", "GBK", "GB18030"));
        ae.add(sub(__("Chinese Traditional"), "Big5", "Big5-HKSCS", "EUC-TW"));
        ae.add(sub(__("Japanese"), "ISO-2022-JP", "EUC-JP", "Shift_JIS", "windows-31j"));
        ae.add(sub(__("Korean"), "ISO-2022-KR", "EUC-KR", "x-windows-949", "Johab"));
        presets.add(ae);

        JMenu as = new JMenu(__("SE and SW Asian"));
        as.add(sub(__("Thai"), "ISO-8859-11", "windows-874", "TIS-620", "MacThai"));
        as.add(sub(__("Turkish"), "ISO-8859-9", "windows-1254", "IBM857", "MacTurkish"));
        as.add(sub(__("Vietnamese"), "windows-1258"));
        presets.add(as);

        JMenu me = new JMenu(__("Middle Eastern"));
        me.add(sub(__("Arabic"), "ISO-8859-6", "windows-1256", "IBM864", "MacArabic"));
        me.add(sub(__("Hebrew"), "ISO-8859-8", "windows-1255", "IBM862", "MacHebrew"));
        presets.add(me);
    }

    private JMenu sub(String label, String... specs) {
        JMenu m = new JMenu(label);
        addItems(m, specs);
        return m;
    }

    private void addItems(JMenu menu, String... specs) {
        for (String spec : specs) {
            final String charset = toCharset(spec);
            JMenuItem item = new JMenuItem(displayLabel(charset));
            item.addActionListener(e -> selectInCombo(charset));
            menu.add(item);
        }
    }
}
