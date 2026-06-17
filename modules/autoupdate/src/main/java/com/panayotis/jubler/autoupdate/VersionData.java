/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.autoupdate;

import com.panayotis.jubler.os.DEBUG;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionData implements Comparable<VersionData> {
    private static final Pattern pattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:[-.]([a-z]+)\\.?(\\d+)?)?$");
    public final String version;
    public final String url;
    public final String description;
    private final int[] parts;
    private final String suffixLabel;   // null = final release (no pre-release suffix)
    private final int suffixNum;        // numeric part of the suffix (0 when absent)

    public VersionData(String version, String url, String description) {
        String v = version.toLowerCase().startsWith("v") ? version.substring(1) : version;
        this.version = v;
        this.url = url;
        this.description = description.trim();
        int[] p = new int[3];
        String label = null;
        int num = 0;
        try {
            Matcher matcher = pattern.matcher(v.toLowerCase());
            if (matcher.matches()) {
                for (int i = 0; i < p.length; i++)
                    p[i] = Integer.parseInt(matcher.group(i + 1));
                label = matcher.group(4);   // null when there is no suffix
                if (matcher.group(5) != null)
                    num = Integer.parseInt(matcher.group(5));
            }
        } catch (Exception e) {
            DEBUG.debug(e);
        }
        this.parts = p;
        this.suffixLabel = label;
        this.suffixNum = num;
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
        // Same major.minor.patch: a final release outranks any pre-release suffix,
        // otherwise compare the suffix label alphabetically (alpha < beta < gamma < rc ...)
        // and then its number naturally (alpha2 < alpha10).
        if (suffixLabel == null && other.suffixLabel == null) return 0;
        if (suffixLabel == null) return -1;          // this is final -> other is not newer
        if (other.suffixLabel == null) return 1;     // other is final -> other is newer
        int labelDiff = suffixLabel.compareTo(other.suffixLabel);
        if (labelDiff != 0)
            return -labelDiff;
        return other.suffixNum - suffixNum;
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
}
