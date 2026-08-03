/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.media.VideoFile;
import com.panayotis.jubler.media.preview.decoders.PreviewProviderRegistry;
import com.panayotis.jubler.media.preview.decoders.SubtitleStreamInfo;
import com.panayotis.jubler.os.SystemDependent;
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

    /**
     * The single owner of Tools-menu item availability. Re-evaluate each registered tool for
     * {@code frame} and enable/disable its menu item accordingly. Items are located by the name carried
     * from {@link ToolMenu#name}. A tool item is enabled only when the frame actually holds a document
     * <em>and</em> the tool reports itself {@link Tool#isAvailable(JubFrame) available} — so before any
     * document is loaded every item stays disabled, exactly as when items are first created. Cheap and
     * idempotent; called when a document is enabled, on every media change, and whenever windows open or
     * close (window-count is what governs Join/Reparent).
     */
    public static void updateToolsAvailability(JubFrame frame) {
        boolean hasDocument = frame.getSubtitles() != null;
        for (ArrayList<Tool> group : tools.values())
            for (Tool tool : group) {
                JMenuItem item = findMenuItem(frame, tool.menu == null ? null : tool.menu.name);
                if (item != null)
                    item.setEnabled(hasDocument && tool.isAvailable(frame));
            }
    }

    /*
     * Only the Tools menu is scanned: that is where availability-driven items live (our media tool,
     * Join/Reparent). The Edit sub-menus (Delete/Mark/Style) manage their own item state, so they are
     * deliberately left untouched to preserve existing behaviour.
     */
    private static JMenuItem findMenuItem(JubFrame frame, String name) {
        if (name == null || frame.ToolsM == null)
            return null;
        for (Component c : frame.ToolsM.getMenuComponents())
            if (c instanceof JMenuItem && name.equals(c.getName()))
                return (JMenuItem) c;
        return null;
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
            updateToolsAvailability(current);

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
        if (SystemDependent.isFlatpak()) {
            // External tools (recipes) can't run in the sandbox — parked for v1; hide the whole menu.
            externalsM.setVisible(false);
            return;
        }
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
        if (!JRecipeRunDialog.needsPrompt(recipe, jubler)) {
            Map<String, String> values = resolveDefaults(recipe);
            new JRecipeProgress(jubler, recipe.getName()).execute(jubler, recipe, values, null,
                    java.util.Collections.emptyMap(), JRecipeRunDialog.defaultReplaceInCurrent(jubler));
            return;
        }
        // A subtitle-stream picker needs the attached video's streams. Parsing the media can take a
        // moment, so probe off the EDT (with a wait cursor) and only then open the populated dialog —
        // never block the UI thread building it.
        VideoFile vf = usesVideoSubtitle(recipe) && jubler.getMediaFile() != null
                ? jubler.getMediaFile().getVideoFile() : null;
        if (vf == null) {
            promptAndExecute(jubler, recipe, java.util.Collections.<SubtitleStreamInfo>emptyList());
            return;
        }
        final VideoFile fvf = vf;
        jubler.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            final List<SubtitleStreamInfo> streams = PreviewProviderRegistry.probeSubtitleStreams(fvf);
            SwingUtilities.invokeLater(() -> {
                jubler.setCursor(Cursor.getDefaultCursor());
                promptAndExecute(jubler, recipe, streams);
            });
        }, "recipe-subtitle-probe").start();
    }

    private static boolean usesVideoSubtitle(Recipe recipe) {
        for (RecipeParam p : recipe.getParams())
            if (p.getType() == RecipeParam.Type.VIDEO_SUBTITLE)
                return true;
        return false;
    }

    /** Show the (populated) run dialog and, if accepted, launch the recipe. Runs on the EDT. */
    private static void promptAndExecute(JubFrame jubler, Recipe recipe, List<SubtitleStreamInfo> streams) {
        JRecipeRunDialog dialog = new JRecipeRunDialog(jubler, jubler, recipe, streams);
        if (!dialog.showRun())
            return;
        new JRecipeProgress(jubler, recipe.getName()).execute(jubler, recipe, dialog.getValues(),
                dialog.getScope(), dialog.getWindowSelections(), dialog.getReplaceInCurrent());
    }

    private static Map<String, String> resolveDefaults(Recipe recipe) {
        Map<String, String> values = new HashMap<>();
        for (RecipeParam p : recipe.getParams()) {
            if (p.isSecret() && !p.getDefaultValue().isEmpty()) {
                values.put(p.getKey(), RecipeSecrets.decrypt(p.getDefaultValue()));
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
