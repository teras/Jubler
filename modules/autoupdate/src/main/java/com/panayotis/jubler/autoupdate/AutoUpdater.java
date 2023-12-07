
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
import com.panayotis.jubler.plugins.PluginCollection;
import com.panayotis.jubler.plugins.PluginItem;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

/**
 * @author teras
 */
public class AutoUpdater implements PluginCollection, PluginItem<Launcher> {

    static String newVersion;
    static String versionUrl;

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
            if (releases.isArray()) {
                VersionUrl latest = findReleases(releases.asArray());
                showMax(latest);
            }
        }).start();
    }

    @Override
    public Collection<PluginItem<?>> getPluginItems() {
        return Arrays.asList(this, new UIUpdater());
    }

    public String getCollectionName() {
        return "Auto update";
    }

    private VersionUrl findReleases(JsonArray releases) {
        Collection<VersionUrl> result = new TreeSet<>();
        releases.iterator().forEachRemaining(it -> {
            if (!it.isObject())
                return;
            JsonObject release = it.asObject();
            if (release.getBoolean("draft", true)
                    || release.getBoolean("prerelease", true))
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
            result.add(new VersionUrl(tag, url));
        });
        return result.isEmpty() ? null : result.iterator().next();
    }

    private void showMax(VersionUrl latest) {
//        VersionUrl now = new VersionUrl("5.6", "");
        VersionUrl now = new VersionUrl(JAbout.getCurrentVersion(), "");
        if (now.compareTo(latest) > 0)
            SwingUtilities.invokeLater(() -> syncInvoke(latest));
    }

    private static void syncInvoke(VersionUrl latest) {
        AutoUpdater.newVersion = latest.version;
        AutoUpdater.versionUrl = latest.url;
        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof JubFrame)
                ((JubFrame) frame).newVersionFound(latest.version, latest.url);
        }
    }
}
