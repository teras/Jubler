/*
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

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.options.JExternalToolsOptions;
import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.tools.externals.ExternalTool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumMap;

/**
 * @author teras
 */
public class ToolsManager implements PluginContext {

    private static final EnumMap<Location, ArrayList<Tool>> tools = new EnumMap<Location, ArrayList<Tool>>(Location.class);
    private static RealTimeTool recoder, shifter;

    static {
        PluginManager.manager.callPluginListeners(new ToolsManager());
    }

    private ToolsManager() {
    }

    public static void add(Tool tool) {
        tools.computeIfAbsent(tool.menu.location, k -> new ArrayList<>()).add(tool);
    }

    public static void register(JubFrame current) {
        // Backup existing tools menu
        Component[] oldtools = current.ToolsM.getMenuComponents();
        current.ToolsM.removeAll();
        try {
            /* Populate tools menu */
            for (Tool tool : tools.get(Location.FILETOOL))
                addMenu(current, current.ToolsM, tool);
            current.ToolsM.add(new JSeparator());
            for (Tool tool : tools.get(Location.TIMETOOL))
                addMenu(current, current.ToolsM, tool);
            current.ToolsM.add(new JSeparator());
            for (Tool tool : tools.get(Location.CONTENTTOOL))
                addMenu(current, current.ToolsM, tool);
            current.ToolsM.add(new JSeparator());
            setFileToolsStatus(current, false);

            /* Populate edit menu */
            for (Tool tool : tools.get(Location.DELETE))
                addMenu(current, current.DeleteEM, tool);
            for (Tool tool : tools.get(Location.MARK))
                addMenu(current, current.MarkEM, tool);
            for (Tool tool : tools.get(Location.STYLE))
                addMenu(current, current.StyleEM, tool);
        } catch (NullPointerException ex) {
        }
        // Restore tools menu old entries
        for (Component comp : oldtools)
            current.ToolsM.add(comp);
        updateExternals(current);
    }

    private static void updateExternals(final JubFrame jubler) {
        JMenu externalsM = jubler.ExternalsM;
        for (Component menuItem : externalsM.getMenuComponents())
            externalsM.remove(menuItem);
        int i = 0;
        for (final ExternalTool tool : JExternalToolsOptions.getList()) {
            JMenuItem menuItem = new JMenuItem(tool.getName());
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tool.exec(jubler);
                }
            });
            menuItem.setName("EXT" + (i++));
            externalsM.add(menuItem);
        }
    }

    public static void updateExternals() {
        for (final JubFrame jubler : JubFrame.windows)
            updateExternals(jubler);
    }

    private static void addMenu(final JubFrame current, final JMenu ToolsM, final Tool tool) {
        JMenuItem item = new JMenuItem(tool.menu.text, tool.menu.key);
        if (tool.menu.key != 0)
            item.setAccelerator(KeyStroke.getKeyStroke(tool.menu.key, tool.menu.mask));
        item.setEnabled(false);
        item.setName(tool.menu.name);
        ToolsM.add(item);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tool.updateData(current);
                tool.execute(current);
            }
        });
    }

    /*
     * Join and Reparent are in the first block of menu, or else this code will break,
     * since it searches for the first separator item
     */
    public static void setFileToolsStatus(JubFrame current, boolean status) {
        JMenuItem Join = null;
        JMenuItem Reparent = null;
        for (Component item : current.ToolsM.getMenuComponents())
            if (item instanceof JMenuItem) {
                if ("TJO".equals(((JMenuItem) item).getName()))
                    Join = (JMenuItem) item;
                else if ("TPA".equals(((JMenuItem) item).getName()))
                    Reparent = (JMenuItem) item;
            } else
                break;
        if (Join != null)
            Join.setEnabled(status);
        if (Reparent != null)
            Reparent.setEnabled(status);
    }

    public static RealTimeTool getRecoder() {
        return recoder;
    }

    public static void setRecoder(RealTimeTool recoder) {
        ToolsManager.recoder = recoder;
    }

    public static RealTimeTool getShifter() {
        return shifter;
    }

    public static void setShifter(RealTimeTool shifter) {
        ToolsManager.shifter = shifter;
    }
}
