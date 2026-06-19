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
import com.panayotis.jubler.tools.externals.Recipes;
import com.panayotis.jubler.tools.externals.gui.JRecipeEditor;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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

    public JExternalToolsOptions() {
        setLayout(new BorderLayout(0, 6));

        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recipeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    editSelected();
            }
        });
        add(new JScrollPane(recipeList), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);
    }

    private JPanel buildButtons() {
        JPanel south = new JPanel(new BorderLayout());

        JPanel edit = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addB = new JButton("+ " + __("Add"));
        JButton removeB = new JButton("- " + __("Remove"));
        JButton editB = new JButton(__("Edit…"));
        addB.addActionListener(e -> addRecipe());
        removeB.addActionListener(e -> removeSelected());
        editB.addActionListener(e -> editSelected());
        edit.add(addB);
        edit.add(removeB);
        edit.add(editB);

        JPanel share = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton fetchB = new JButton(__("Fetch…"));
        JButton loadB = new JButton(__("Load…"));
        JButton saveB = new JButton(__("Save…"));
        fetchB.setToolTipText(__("Download shared recipes from the Jubler GitHub"));
        fetchB.addActionListener(e -> fetchFromGitHub());
        loadB.addActionListener(e -> loadFromFile());
        saveB.addActionListener(e -> saveToFile());
        share.add(fetchB);
        share.add(loadB);
        share.add(saveB);

        south.add(edit, BorderLayout.WEST);
        south.add(share, BorderLayout.EAST);
        return south;
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
        if (recipe != null && openEditor(recipe))
            recipeList.repaint();
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
            Recipe recipe = Recipes.loadFromFile(fc.getSelectedFile());
            Recipes.getList().add(recipe);
            model.addElement(recipe);
            recipeList.setSelectedValue(recipe, true);
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
        List<Recipe> fetched = RecipeCatalog.cached();
        if (fetched != null && !fetched.isEmpty())
            offerCatalog(fetched);
        new Thread(() -> {
            List<Recipe> fresh = RecipeCatalog.fetch();
            if (fresh != null && !fresh.isEmpty())
                SwingUtilities.invokeLater(() -> offerCatalog(fresh));
            else if (fetched == null)
                SwingUtilities.invokeLater(() ->
                        JIDialog.info(this, __("No shared recipes available (offline?)."), __("Fetch recipes")));
        }, "RecipeCatalog").start();
    }

    private void offerCatalog(List<Recipe> available) {
        JCatalogChooser chooser = new JCatalogChooser(SwingUtilities.getWindowAncestor(this), available);
        for (Recipe chosen : chooser.choose()) {
            Recipes.getList().add(chosen);
            model.addElement(chosen);
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
        return "Externals";
    }

    @Override
    public String getTabTooltip() {
        return "Configure external tools";
    }

    @Override
    public Icon getTabIcon() {
        return Theme.loadIcon("externals");
    }

    @Override
    public void changeProgram() {
    }
}
