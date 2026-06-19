/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.eclipsesource.json.JsonObject;

import java.util.Collection;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * A typed parameter of a {@link Recipe}. The author defines it (key/label/type and
 * type-specific fields); the user fills it at run time (per-run) or once (persistent).
 *
 * <p>The {@code key} is referenced in the command template as {@code %<key>}. It must be
 * at least 2 characters so it can never collide with the single-character system
 * placeholders ({@code %i %j %a %v %o %x}).</p>
 */
public class RecipeParam {

    public enum Type {
        TEXTBOX, COMBOBOX, CHECKBOX, PATH, LANGUAGE, WINDOW, SECRET;

        public String getLabel() {
            switch (this) {
                case TEXTBOX:
                    return __("Text");
                case COMBOBOX:
                    return __("Dropdown");
                case CHECKBOX:
                    return __("Checkbox");
                case PATH:
                    return __("Path");
                case LANGUAGE:
                    return __("Language");
                case WINDOW:
                    return __("Window");
                case SECRET:
                    return __("Secret");
                default:
                    return name();
            }
        }

        /** A short explanation of what this input type does, shown in the Type info popup. */
        public String getDescription() {
            switch (this) {
                case TEXTBOX:
                    return __("A free-text field the user types into.");
                case COMBOBOX:
                    return __("A drop-down list of predefined choices.");
                case CHECKBOX:
                    return __("An on/off box that adds a fixed value to the command when checked.");
                case PATH:
                    return __("A file or folder picker with a Browse button.");
                case LANGUAGE:
                    return __("A language selector that emits the ISO language code.");
                case WINDOW:
                    return __("A drop-down of other open subtitle windows; that window's subtitles are saved to a temporary file and passed to the tool.");
                case SECRET:
                    return __("A password field; stored encrypted and excluded from shared recipes.");
                default:
                    return "";
            }
        }

        @Override
        public String toString() {
            return getLabel();
        }

        public static Type fromName(String name, Type deflt) {
            if (name != null)
                for (Type t : values())
                    if (t.name().equals(name))
                        return t;
            return deflt;
        }
    }

    private String key;
    private String label;
    private String help;
    private Type type;
    private boolean persistent;

    /* Type-specific fields (each used only by the relevant type) */
    private String defaultValue;    // TextBox, ComboBox, Path, Language
    private String choices;         // ComboBox ('|'-separated)
    private String formatter;       // any: e.g. "-m %VALUE"; skipped when value empty
    private boolean folder;         // Path
    private String checkedValue;    // CheckBox: emitted when checked

    public RecipeParam() {
        this("param", Type.TEXTBOX);
    }

    public RecipeParam(String key, Type type) {
        this.key = key;
        this.label = key;
        this.type = type;
        this.help = "";
        this.defaultValue = "";
        this.choices = "";
        this.formatter = "";
        this.checkedValue = "";
        this.persistent = false;
        this.folder = false;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label == null || label.isEmpty() ? key : label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public String getDefaultValue() {
        return defaultValue == null ? "" : defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getChoices() {
        return choices == null ? "" : choices;
    }

    public void setChoices(String choices) {
        this.choices = choices;
    }

    public String[] getChoiceList() {
        String c = getChoices().trim();
        return c.isEmpty() ? new String[0] : c.split("\\s*\\|\\s*");
    }

    public String getFormatter() {
        return formatter == null ? "" : formatter;
    }

    public void setFormatter(String formatter) {
        this.formatter = formatter;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public String getCheckedValue() {
        return checkedValue == null ? "" : checkedValue;
    }

    public void setCheckedValue(String checkedValue) {
        this.checkedValue = checkedValue;
    }

    public boolean isSecret() {
        return type == Type.SECRET;
    }

    /**
     * Apply this param's formatter to a resolved value. Returns an empty string when
     * the value is empty (so optional flags don't appear with no argument); otherwise
     * applies the {@code %VALUE} formatter, or the raw value if no formatter is set.
     */
    public String format(String value) {
        if (value == null || value.isEmpty())
            return "";
        String f = getFormatter();
        if (f.isEmpty())
            return value;
        return f.replace("%VALUE", value);
    }

    @Override
    public String toString() {
        return getLabel() + " (%" + key + ")";
    }

    /* ===================== Validation ===================== */

    /**
     * Validate a custom key. Returns an i18n error message, or {@code null} when valid.
     *
     * @param candidate the key to test
     * @param existing  the keys already in use by the recipe (excluding this param)
     */
    public static String validateKey(String candidate, Collection<String> existing) {
        if (candidate == null || candidate.length() < 2)
            return __("Key must be at least 2 characters (single-character keys are reserved).");
        if (!candidate.matches("[a-zA-Z][a-zA-Z0-9]*"))
            return __("Key must start with a letter and contain only letters and digits.");
        if (existing != null && existing.contains(candidate))
            return __("A parameter with this key already exists.");
        return null;
    }

    /* ===================== Serialization ===================== */

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.add("key", key);
        o.add("label", label == null ? "" : label);
        o.add("help", getHelp());
        o.add("type", type.name());
        o.add("persistent", persistent);
        if (!getDefaultValue().isEmpty())
            o.add("default", getDefaultValue());
        if (!getChoices().isEmpty())
            o.add("choices", getChoices());
        if (!getFormatter().isEmpty())
            o.add("formatter", getFormatter());
        if (folder)
            o.add("folder", true);
        if (!getCheckedValue().isEmpty())
            o.add("checkedValue", getCheckedValue());
        return o;
    }

    public static RecipeParam fromJson(JsonObject o) {
        RecipeParam p = new RecipeParam(o.getString("key", "param"),
                Type.fromName(o.getString("type", "TEXTBOX"), Type.TEXTBOX));
        p.setLabel(o.getString("label", ""));
        p.setHelp(o.getString("help", ""));
        p.setPersistent(o.getBoolean("persistent", false));
        p.setDefaultValue(o.getString("default", ""));
        p.setChoices(o.getString("choices", ""));
        p.setFormatter(o.getString("formatter", ""));
        p.setFolder(o.getBoolean("folder", false));
        p.setCheckedValue(o.getString("checkedValue", ""));
        return p;
    }
}
