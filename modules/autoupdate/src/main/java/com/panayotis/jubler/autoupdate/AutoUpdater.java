
/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.autoupdate;

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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.Launcher;
import com.panayotis.jubler.information.JAbout;
import com.panayotis.jubler.os.DEBUG;
import com.panayotis.jubler.os.SystemDependent;
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AutoUpdater implements PluginCollection, PluginItem<Launcher> {

    static List<VersionData> newerVersions;

    private final boolean skipDraft;
    private final boolean skipPreRelease;

    public AutoUpdater() {
        this(true, true);
    }

    public AutoUpdater(boolean skipDraft, boolean skipPreRelease) {
        this.skipDraft = skipDraft;
        this.skipPreRelease = skipPreRelease;
    }

    @Override
    public void execPlugin(Launcher caller) {
        new Thread(() -> {
            HttpURLConnection connection;
            try {
                URL url = new URL("https://api.github.com/repos/teras/jubler/releases");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
            } catch (Exception e) {
                DEBUG.debug(e);
                return;
            }
            JsonValue releases;
            try (Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                releases = Json.parse(in);
            } catch (Exception e) {
                DEBUG.debug(e);
                return;
            }
            if (releases.isArray())
                showVersions(findReleases(releases.asArray()));
        }).start();
    }

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Arrays.asList(this, new UIUpdater());
    }

    public String getCollectionName() {
        return "Auto update";
    }

    private List<VersionData> findReleases(JsonArray releases) {
        List<VersionData> result = new ArrayList<>();
        releases.iterator().forEachRemaining(it -> {
            if (!it.isObject())
                return;
            JsonObject release = it.asObject();
            if (skipDraft && release.getBoolean("draft", true))
                return;
            if (skipPreRelease && release.getBoolean("prerelease", true))
                return;
            String tag = release.getString("tag_name", null);
            if (tag == null) {
                DEBUG.debug("Release tag not found");
                return;
            }
            String url = release.getString("html_url", null);
            if (url == null) {
                DEBUG.debug("Release url not found");
                return;
            }
            String description = release.getString("body", null);
            if (description == null) {
                DEBUG.debug("Description not found");
                return;
            }

            if (hasAsset(release))
                result.add(new VersionData(tag, url, description));
        });
        return result;
    }

    private static boolean hasAsset(JsonObject release) {
        JsonValue assets = release.get("assets");
        if (assets == null || !assets.isArray()) {
            DEBUG.debug("Assets not found");
            return false;
        }
        String extension = SystemDependent.getAssetExtension();
        String tag = SystemDependent.getAssetTag();
        for (JsonValue it : assets.asArray()) {
            if (it.isObject()) {
                JsonObject asset = it.asObject();
                String name = asset.getString("name", "").toLowerCase();
                if (name.endsWith(extension) || name.contains(tag))
                    return true;
            }
        }
        return false;
    }

    private void showVersions(List<VersionData> releases) {
        VersionData current = new VersionData(JAbout.getCurrentVersion());
//        VersionData current = new VersionData("5.0.0");
        List<VersionData> newer = releases.stream().filter(it -> current.compareTo(it) > 0).collect(Collectors.toList());
        if (!newer.isEmpty())
            SwingUtilities.invokeLater(() -> syncInvoke(newer));
    }

    private static void syncInvoke(List<VersionData> latest) {
        AutoUpdater.newerVersions = latest;
        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof JubFrame)
                ((JubFrame) frame).setNewVersionCallback(AutoUpdater::showNewVersion);
        }
    }

    static void showNewVersion(JFrame parent) {
        new JUpdateInfo(parent).setVisible(true);
    }
}
