/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.autoupdate;

import com.panayotis.jubler.os.DEBUG;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionData implements Comparable<VersionData> {
    private static final Pattern pattern = Pattern.compile("^([\\d]+)\\.([\\d]+)\\.([\\d]+)(-[\\w]+)?$");
    public final String version;
    public final String url;
    public final String description;
    private final int[] parts;

    public VersionData(String version, String url, String description) {
        this.version = version.toLowerCase().startsWith("v") ? version.substring(1) : version;
        this.url = url;
        this.parts = getParts(this.version);
        this.description = description.trim();
    }

    public VersionData(String version) {
        this(version, null, "");
    }

    @Override
    public int compareTo(VersionData other) {
        for (int i = 0; i < parts.length; i++) {
            int diff = parts[i] - other.parts[i];
            if (diff != 0)
                return -diff;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionData)) return false;
        VersionData that = (VersionData) o;
        if (!version.equals(that.version)) return false;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        int result = version.hashCode();
        result = 31 * result + url.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "VersionUrl{" + version + '}';
    }

    private static int[] getParts(String version) {
        int[] result = new int[3];
        try {
            Matcher matcher = pattern.matcher(version);
            if (matcher.matches()) {
                for (int i = 0; i < result.length; i++) {
                    String part = matcher.group(i + 1);
                    if (part != null)
                        result[i] = Integer.parseInt(part);
                }
            }
        } catch (Exception e) {
            DEBUG.debug(e);
        }
        return result;
    }
}
