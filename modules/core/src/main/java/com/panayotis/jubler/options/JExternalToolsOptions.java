/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.options;

import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.FileCommunicator;
import com.panayotis.jubler.os.JIDialog;
import com.panayotis.jubler.theme.Theme;
import com.panayotis.jubler.tools.ToolsManager;
import com.panayotis.jubler.tools.externals.Recipe;
import com.panayotis.jubler.tools.externals.RecipeCatalog;
import com.panayotis.jubler.tools.externals.RecipeResolver;
import com.panayotis.jubler.tools.externals.RecipeSecrets;
import com.panayotis.jubler.tools.externals.Recipes;
import com.panayotis.jubler.tools.externals.gui.JRecipeEditor;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.util.List;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Preferences tab for external-tool recipes: a list with Add / Remove / Edit and the
 * sharing actions Fetch (from GitHub) / Load / Save (single recipe ↔ file). Each recipe
 * is edited in a single-window {@link JRecipeEditor}. The dialog-level Export / Import /
 * Reset (in {@code JPreferences}) operate on all preferences and are untouched here.
 */
public class JExternalToolsOptions extends JPanel implements OptionsHolder {

    private final DefaultListModel<Recipe> model = new DefaultListModel<>();
    private final JList<Recipe> recipeList = new JList<>(model);
    private final JButton urlB = new JButton();
    private JButton removeB, editB;

    public JExternalToolsOptions() {
        setLayout(new BorderLayout(0, 6));

        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recipeList.setCellRenderer(new AvailabilityRenderer());
        recipeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    editSelected();
            }
        });
        recipeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updateButtons();
        });
        add(new JScrollPane(recipeList), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
        updateButtons();
    }

    private JPanel buildButtons() {
        JPanel south = new JPanel(new BorderLayout());

        JPanel edit = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        JButton addB = iconButton("plus", __("Add"));
        removeB = iconButton("minus", __("Remove"));
        editB = iconButton("edit", __("Edit…"));
        // Globe: opens the selected recipe's web page; keeps its native 4:3 aspect, disabled when there is none.
        ImageIcon globe = Theme.loadIcon("flag-global");
        if (globe != null)
            urlB.setIcon(new ImageIcon(globe.getImage().getScaledInstance(-1, 28, Image.SCALE_SMOOTH)));
        else
            urlB.setText("🌐");
        urlB.setToolTipText(__("Open the recipe's web page"));
        urlB.setMargin(new Insets(4, 8, 4, 8));
        urlB.setEnabled(false);
        addB.addActionListener(e -> addRecipe());
        removeB.addActionListener(e -> removeSelected());
        editB.addActionListener(e -> editSelected());
        urlB.addActionListener(e -> openSelectedUrl());
        edit.add(addB);
        edit.add(removeB);
        edit.add(urlB);
        edit.add(editB);

        JPanel share = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton pinB = new JButton(__("Change PIN…"));
        JButton fetchB = new JButton(__("Fetch from GitHub…"));
        JButton loadB = new JButton(__("Load…"));
        JButton saveB = new JButton(__("Save…"));
        pinB.setToolTipText(__("Change the PIN that protects secret values"));
        fetchB.setToolTipText(__("Download shared recipes from the Jubler GitHub"));
        pinB.addActionListener(e -> RecipeSecrets.changePin(this));
        fetchB.addActionListener(e -> fetchFromGitHub());
        loadB.addActionListener(e -> loadFromFile());
        saveB.addActionListener(e -> saveToFile());
        share.add(pinB);
        share.add(fetchB);
        share.add(loadB);
        share.add(saveB);

        south.add(edit, BorderLayout.WEST);
        south.add(share, BorderLayout.EAST);
        return south;
    }

    private static JButton iconButton(String icon, String tooltip) {
        JButton b = new JButton();
        ImageIcon base = Theme.loadIcon(icon);
        if (base != null)
            b.setIcon(new ImageIcon(base.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH)));
        else
            b.setText(tooltip);
        b.setToolTipText(tooltip);
        b.setMargin(new Insets(4, 8, 4, 8));
        return b;
    }

    /**
     * Paints recipes whose tool cannot be found in red, with a ⚠ note naming the missing
     * executable, so the user sees at a glance which recipes can't run. The check is a cheap
     * filesystem lookup ({@link RecipeResolver#isAvailable}), fine to run per row.
     */
    private static class AvailabilityRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Recipe) {
                Recipe r = (Recipe) value;
                if (!RecipeResolver.isAvailable(r)) {
                    setText(r.getName() + "   ⚠");
                    if (!isSelected)
                        setForeground(Color.RED);
                }
            }
            return this;
        }
    }

    /* ===================== actions ===================== */

    private void addRecipe() {
        Recipe recipe = new Recipe(__("New recipe"));
        Recipes.getList().add(recipe);
        model.addElement(recipe);
        recipeList.setSelectedValue(recipe, true);
        if (!openEditor(recipe)) {
            // user cancelled a brand-new recipe -> drop it
            Recipes.getList().remove(recipe);
            model.removeElement(recipe);
        }
    }

    private void editSelected() {
        Recipe recipe = recipeList.getSelectedValue();
        if (recipe != null && openEditor(recipe)) {
            recipeList.repaint();
            updateButtons();   // the URL may have changed in the editor
        }
    }

    /** Remove/Edit need a selection; the globe additionally needs the recipe to have a URL. */
    private void updateButtons() {
        Recipe sel = recipeList.getSelectedValue();
        removeB.setEnabled(sel != null);
        editB.setEnabled(sel != null);
        urlB.setEnabled(sel != null && !sel.getUrl().isEmpty());
    }

    /** Open the selected recipe's web page in the default browser. */
    private void openSelectedUrl() {
        Recipe sel = recipeList.getSelectedValue();
        if (sel == null || sel.getUrl().isEmpty())
            return;
        try {
            Desktop.getDesktop().browse(URI.create(sel.getUrl()));
        } catch (Exception e) {
            DEBUG.debug(e);
        }
    }

    private boolean openEditor(Recipe recipe) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        return new JRecipeEditor(owner, recipe).showEditor();
    }

    private void removeSelected() {
        int idx = recipeList.getSelectedIndex();
        if (idx < 0)
            return;
        Recipes.getList().remove(model.get(idx));
        model.remove(idx);
        if (model.getSize() > 0)
            recipeList.setSelectedIndex(Math.min(idx, model.getSize() - 1));
    }

    private void loadFromFile() {
        JFileChooser fc = recipeChooser(__("Load recipe"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try {
            List<Recipe> loaded = Recipes.loadFromFile(fc.getSelectedFile());
            for (Recipe recipe : loaded) {
                Recipes.getList().add(recipe);
                model.addElement(recipe);
            }
            if (!loaded.isEmpty())
                recipeList.setSelectedValue(loaded.get(loaded.size() - 1), true);
        } catch (Exception e) {
            DEBUG.debug(e);
            JIDialog.error(this, __("Could not load recipe: {0}", e.getMessage()), __("Error"));
        }
    }

    private void saveToFile() {
        Recipe recipe = recipeList.getSelectedValue();
        if (recipe == null) {
            JIDialog.info(this, __("Select a recipe to save."), __("Save recipe"));
            return;
        }
        JFileChooser fc = recipeChooser(__("Save recipe"));
        fc.setSelectedFile(new File(sanitize(recipe.getName()) + ".json"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        try {
            Recipes.saveToFile(recipe, withJsonExtension(fc.getSelectedFile()));
        } catch (Exception e) {
            DEBUG.debug(e);
            JIDialog.error(this, __("Could not save recipe: {0}", e.getMessage()), __("Error"));
        }
    }

    private void fetchFromGitHub() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        List<Recipe> cached = RecipeCatalog.cached();
        if (cached != null && !cached.isEmpty()) {
            // Show the cached catalog at once, then refresh the same dialog in place.
            JCatalogChooser chooser = new JCatalogChooser(owner, cached);
            new Thread(() -> {
                List<Recipe> fresh = RecipeCatalog.fetch();
                if (fresh != null && !fresh.isEmpty())
                    SwingUtilities.invokeLater(() -> chooser.setRecipes(fresh));
            }, "RecipeCatalog").start();
            importChosen(chooser.choose());
        } else {
            // Nothing cached: fetch first, then show a single dialog (or report offline).
            new Thread(() -> {
                List<Recipe> fresh = RecipeCatalog.fetch();
                SwingUtilities.invokeLater(() -> {
                    if (fresh != null && !fresh.isEmpty())
                        importChosen(new JCatalogChooser(owner, fresh).choose());
                    else
                        JIDialog.info(this, __("No shared recipes available (offline?)."), __("Fetch recipes"));
                });
            }, "RecipeCatalog").start();
        }
    }

    private void importChosen(List<Recipe> chosen) {
        for (Recipe r : chosen) {
            Recipes.getList().add(r);
            model.addElement(r);
        }
        recipeList.repaint();
    }

    private JFileChooser recipeChooser(String title) {
        JFileChooser fc = new JFileChooser(FileCommunicator.getDefaultDirPath());
        fc.setDialogTitle(title);
        fc.setFileFilter(new FileNameExtensionFilter(__("Recipe files") + " (*.json)", "json"));
        return fc;
    }

    private static File withJsonExtension(File f) {
        return f.getName().toLowerCase().endsWith(".json") ? f : new File(f.getParentFile(), f.getName() + ".json");
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9-_]+", "_");
    }

    /* ===================== OptionsHolder ===================== */

    @Override
    public void loadPreferences() {
        Recipes.load();
        rebuildModel();
    }

    @Override
    public void savePreferences() {
        Recipes.save();
        ToolsManager.updateExternals();
    }

    private void rebuildModel() {
        model.clear();
        for (Recipe r : Recipes.getList())
            model.addElement(r);
    }

    @Override
    public JPanel getTabPanel() {
        return this;
    }

    @Override
    public String getTabName() {
        return __("Externals");
    }

    @Override
    public String getTabTooltip() {
        return __("Configure external tools");
    }

    @Override
    public Icon getTabIcon() {
        return Theme.loadIcon("externals");
    }

    @Override
    public void changeProgram() {
    }
}
