/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.panayotis.jubler.plugins.Availabilities;
import com.panayotis.jubler.subs.loader.SubFormat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tool-agnostic recipe: an external command (or, in the future, an in-process
 * module) run against the project's subtitles/audio/video, with typed parameters and
 * a defined apply-back mode. Supersedes the old flat external-tool descriptor; the
 * registry ({@link Recipes}) reads the legacy prefs for backwards compatibility.
 */
public class Recipe {

    private static SubFormat defaultFormat;   // lazily resolved (avoids plugin scan at class load)

    private static SubFormat defaultFormat() {
        if (defaultFormat == null) {
            SubFormat srt = Availabilities.formats.findFromExtension("srt");
            if (srt == null) {
                ArrayList<SubFormat> formats = Availabilities.formats.getFormats();
                srt = formats.isEmpty() ? null : formats.get(0);
            }
            defaultFormat = srt;
        }
        return defaultFormat;
    }

    private String name;
    private String module;          // null => external command; non-null => in-process module (read-only)
    private String path;            // executable: bare name (PATH lookup) or absolute path
    private String command;         // template with %x %i %j %a %v %o and %<key> params
    private SubFormat format;       // wire format for %i/%j/%o
    private OutputMode outputMode;
    private String installInfo;     // shown when the executable is missing
    private final List<RecipeParam> params = new ArrayList<>();
    /* Stored values of persistent params (secret values held encrypted). */
    private final Map<String, String> values = new LinkedHashMap<>();

    public Recipe() {
        this("Recipe");
    }

    public Recipe(String name) {
        this.name = name;
        this.module = null;
        this.path = "";
        this.command = "%x --input %i --output %o";
        this.format = null;
        this.outputMode = OutputMode.REPLACE_NEW;
        this.installInfo = "";
    }

    public Recipe(String name, String path, String command, SubFormat format) {
        this(name);
        this.path = path == null ? "" : path;
        this.command = command;
        if (format != null)
            this.format = format;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public boolean isInProcess() {
        return module != null && !module.isEmpty();
    }

    public String getPath() {
        return path == null ? "" : path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCommand() {
        return command == null ? "" : command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public SubFormat getFormat() {
        return format == null ? defaultFormat() : format;
    }

    public void setFormat(SubFormat format) {
        this.format = format;
    }

    public OutputMode getOutputMode() {
        return outputMode;
    }

    public void setOutputMode(OutputMode outputMode) {
        this.outputMode = outputMode == null ? OutputMode.REPLACE_NEW : outputMode;
    }

    public String getInstallInfo() {
        return installInfo == null ? "" : installInfo;
    }

    public void setInstallInfo(String installInfo) {
        this.installInfo = installInfo;
    }

    public List<RecipeParam> getParams() {
        return params;
    }

    public void addParam(RecipeParam param) {
        params.add(param);
    }

    public void removeParam(RecipeParam param) {
        params.remove(param);
    }

    /** Stored value of a persistent param (raw: encrypted for secrets), or null. */
    public String getStoredValue(String key) {
        return values.get(key);
    }

    public boolean hasStoredValue(String key) {
        String v = values.get(key);
        return v != null && !v.isEmpty();
    }

    public void setStoredValue(String key, String value) {
        if (value == null || value.isEmpty())
            values.remove(key);
        else
            values.put(key, value);
    }

    /** Replace all of this recipe's state with a deep copy of another's (used to revert edits). */
    public void copyFrom(Recipe other) {
        Recipe clone = Recipe.fromJsonString(other.toJsonString(false));
        this.name = clone.name;
        this.module = clone.module;
        this.path = clone.path;
        this.command = clone.command;
        this.format = clone.format;
        this.outputMode = clone.outputMode;
        this.installInfo = clone.installInfo;
        this.params.clear();
        this.params.addAll(clone.params);
        this.values.clear();
        this.values.putAll(clone.values);
    }

    /** Keys already used by other params (for duplicate validation when editing one). */
    public Set<String> keysExcept(RecipeParam self) {
        Set<String> keys = new HashSet<>();
        for (RecipeParam p : params)
            if (p != self)
                keys.add(p.getKey());
        return keys;
    }

    @Override
    public String toString() {
        return name;
    }

    /* ===================== Serialization ===================== */

    /**
     * @param forSharing when true (export to a shared file/catalog), stored <b>secret</b>
     *                   values are omitted so a key is never leaked. Persisting to prefs
     *                   uses {@code false} so secrets survive (held encrypted).
     */
    public JsonObject toJson(boolean forSharing) {
        JsonObject o = new JsonObject();
        o.add("name", name);
        if (isInProcess())
            o.add("module", module);
        o.add("path", getPath());
        o.add("command", getCommand());
        o.add("format", getFormat() == null ? "" : getFormat().getName());
        o.add("output", getOutputMode().name());
        if (!getInstallInfo().isEmpty())
            o.add("install", getInstallInfo());
        JsonArray arr = Json.array();
        for (RecipeParam p : params)
            arr.add(p.toJson());
        o.add("params", arr);

        JsonObject vals = new JsonObject();
        for (RecipeParam p : params) {
            if (!p.isPersistent())
                continue;
            String v = values.get(p.getKey());
            if (v == null || v.isEmpty())
                continue;
            if (forSharing && p.isSecret())
                continue;   // never export a secret
            vals.add(p.getKey(), v);
        }
        if (!vals.isEmpty())
            o.add("values", vals);
        return o;
    }

    public String toJsonString(boolean forSharing) {
        return toJson(forSharing).toString();
    }

    public static Recipe fromJson(JsonObject o) {
        Recipe r = new Recipe(o.getString("name", "Recipe"));
        r.setModule(o.getString("module", null));
        r.setPath(o.getString("path", ""));
        r.setCommand(o.getString("command", "%x --input %i --output %o"));
        String fmtName = o.getString("format", null);
        if (fmtName != null && !fmtName.isEmpty()) {
            SubFormat fmt = Availabilities.formats.findFromName(fmtName);
            if (fmt != null)
                r.setFormat(fmt);
        }
        r.setOutputMode(OutputMode.fromName(o.getString("output", "REPLACE_NEW"), OutputMode.REPLACE_NEW));
        r.setInstallInfo(o.getString("install", ""));
        JsonValue arr = o.get("params");
        if (arr != null && arr.isArray())
            for (JsonValue v : arr.asArray())
                if (v.isObject())
                    r.addParam(RecipeParam.fromJson(v.asObject()));
        JsonValue vals = o.get("values");
        if (vals != null && vals.isObject())
            for (JsonObject.Member m : vals.asObject())
                r.setStoredValue(m.getName(), m.getValue().asString());
        return r;
    }

    public static Recipe fromJsonString(String json) {
        return fromJson(Json.parse(json).asObject());
    }
}
