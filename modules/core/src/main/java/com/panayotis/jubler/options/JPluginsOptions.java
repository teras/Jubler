/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.os.PluginRegistry;
import com.panayotis.jubler.os.PluginRegistry.PluginInfo;
import com.panayotis.jubler.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Preferences page that lists the drop-in plugin jars found in the user plugins directory and lets the user
 * enable or disable each one. A disabled plugin is never placed on the classpath, so its code does not run; a
 * change therefore only takes effect after a restart, which is stated on the page and confirmed on save. The
 * page is added to the preferences dialog only when at least one plugin was discovered (see {@code JPreferences}).
 */
public class JPluginsOptions extends JPanel implements OptionsHolder {

    private final Map<String, JCheckBox> boxes = new LinkedHashMap<>();
    private Set<String> loadedEnabled = new LinkedHashSet<>();

    /** Cap the plugin list height so the preferences dialog never grows unbounded; the rest scrolls. */
    private static final int MAX_LIST_HEIGHT = 260;

    public JPluginsOptions() {
        setLayout(new BorderLayout());

        JLabel intro = new JLabel(__("Enable only plugins you trust. Changes apply after you restart Jubler."));
        intro.setBorder(BorderFactory.createEmptyBorder(10, 10, 12, 10));
        add(intro, BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridy = 0;

        for (PluginInfo p : PluginRegistry.getPlugins()) {
            JCheckBox box = new JCheckBox(p.name);
            boxes.put(p.key, box);
            c.insets = new Insets(6, 10, 0, 10);
            rows.add(box, c);
            c.gridy++;
            if (p.description != null && !p.description.isEmpty()) {
                JLabel desc = new JLabel(p.description);
                desc.setForeground(UIManager.getColor("Label.disabledForeground"));
                desc.setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 0));
                c.insets = new Insets(0, 10, 6, 10);
                rows.add(desc, c);
                c.gridy++;
            }
        }
        // Push the rows to the top when the viewport is taller than the content.
        c.weighty = 1.0;
        rows.add(Box.createVerticalGlue(), c);

        JScrollPane scroll = new JScrollPane(rows,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        Dimension pref = rows.getPreferredSize();
        if (pref.height > MAX_LIST_HEIGHT)
            scroll.setPreferredSize(new Dimension(pref.width + 24, MAX_LIST_HEIGHT));
        add(scroll, BorderLayout.CENTER);

        loadPreferences();
    }

    @Override
    public void loadPreferences() {
        loadedEnabled = new LinkedHashSet<>();
        for (Map.Entry<String, JCheckBox> e : boxes.entrySet()) {
            boolean on = PluginRegistry.isEnabled(e.getKey());
            e.getValue().setSelected(on);
            if (on)
                loadedEnabled.add(e.getKey());
        }
    }

    @Override
    public void savePreferences() {
        Set<String> selected = new LinkedHashSet<>();
        for (Map.Entry<String, JCheckBox> e : boxes.entrySet())
            if (e.getValue().isSelected())
                selected.add(e.getKey());
        boolean changed = !selected.equals(loadedEnabled);
        PluginRegistry.setEnabledKeys(selected);
        loadedEnabled = selected;
        if (changed)
            JOptionPane.showMessageDialog(this,
                    __("Please exit Jubler and restart it to apply the changes."));
    }

    @Override
    public JPanel getTabPanel() {
        return this;
    }

    @Override
    public String getTabName() {
        return __("Plugins");
    }

    @Override
    public String getTabTooltip() {
        return __("Enable or disable installed plugins");
    }

    @Override
    public Icon getTabIcon() {
        return Theme.loadIcon("plugins");
    }

    @Override
    public void changeProgram() {
    }
}
