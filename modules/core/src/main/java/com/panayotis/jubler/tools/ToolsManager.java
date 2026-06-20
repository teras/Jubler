/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.plugins.PluginContext;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.tools.ToolMenu.Location;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.tools.externals.Recipe;
import com.panayotis.jubler.tools.externals.RecipeParam;
import com.panayotis.jubler.tools.externals.RecipeResolver;
import com.panayotis.jubler.tools.externals.RecipeSecrets;
import com.panayotis.jubler.tools.externals.Recipes;
import com.panayotis.jubler.tools.externals.gui.JRecipeProgress;
import com.panayotis.jubler.tools.externals.gui.JRecipeRunDialog;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolsManager implements PluginContext {

    private static final EnumMap<Location, ArrayList<Tool>> tools = new EnumMap<Location, ArrayList<Tool>>(Location.class);
    private static RealTimeTool recoder, shifter;

    static {
        PluginManager.getManager().callPluginListeners(new ToolsManager());
    }

    private ToolsManager() {
    }

    public static void add(Tool tool) {
        if (tool != null && tool.menu != null)
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
        externalsM.removeAll();
        int i = 0;
        for (final Recipe recipe : Recipes.getList()) {
            JMenuItem menuItem = new JMenuItem(recipe.getName());
            menuItem.putClientProperty("recipe", recipe);
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    runRecipe(jubler, recipe);
                }
            });
            menuItem.setName("EXT" + (i++));
            externalsM.add(menuItem);
        }
        installAvailabilityListener(externalsM);
    }

    /* Evaluate availability on-show (not live): disable recipes whose executable is missing. */
    private static void installAvailabilityListener(final JMenu externalsM) {
        if (Boolean.TRUE.equals(externalsM.getClientProperty("recipeAvailListener")))
            return;
        externalsM.putClientProperty("recipeAvailListener", Boolean.TRUE);
        externalsM.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                for (Component c : externalsM.getMenuComponents())
                    if (c instanceof JMenuItem) {
                        Object r = ((JMenuItem) c).getClientProperty("recipe");
                        if (r instanceof Recipe)
                            c.setEnabled(RecipeResolver.isAvailable((Recipe) r));
                    }
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
    }

    private static void runRecipe(JubFrame jubler, Recipe recipe) {
        Map<String, String> values;
        List<SubEntry> scope = null;
        Map<String, JubFrame> windowSelections = java.util.Collections.emptyMap();
        boolean replaceInCurrent;
        if (JRecipeRunDialog.needsPrompt(recipe, jubler)) {
            JRecipeRunDialog dialog = new JRecipeRunDialog(jubler, jubler, recipe);
            if (!dialog.showRun())
                return;
            values = dialog.getValues();
            scope = dialog.getScope();
            windowSelections = dialog.getWindowSelections();
            replaceInCurrent = dialog.getReplaceInCurrent();
        } else {
            values = resolveDefaults(recipe);
            replaceInCurrent = JRecipeRunDialog.defaultReplaceInCurrent(jubler);
        }
        new JRecipeProgress(jubler, recipe.getName()).execute(jubler, recipe, values, scope, windowSelections, replaceInCurrent);
    }

    private static Map<String, String> resolveDefaults(Recipe recipe) {
        Map<String, String> values = new HashMap<>();
        for (RecipeParam p : recipe.getParams()) {
            if (p.isPersistent()) {
                String stored = recipe.getStoredValue(p.getKey());
                values.put(p.getKey(), stored == null ? p.getDefaultValue()
                        : (p.isSecret() ? RecipeSecrets.decrypt(stored) : stored));
            } else if (p.getType() == RecipeParam.Type.CHECKBOX) {
                values.put(p.getKey(), Boolean.parseBoolean(p.getDefaultValue()) ? p.getCheckedValue() : "");
            } else {
                values.put(p.getKey(), p.getDefaultValue());
            }
        }
        return values;
    }

    public static void updateExternals() {
        Recipes.load();
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
