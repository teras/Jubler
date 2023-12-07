package com.panayotis.jubler.autoupdate;

public class VersionUrl implements Comparable<VersionUrl> {
    public final String version;
    public final String url;
    private final int[] parts;

    public VersionUrl(String version, String url) {
        this.version = version.toLowerCase().startsWith("v") ? version.substring(1) : version;
        this.url = url;
        this.parts = getParts(this.version);
    }

    @Override
    public int compareTo(VersionUrl other) {
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
        if (!(o instanceof VersionUrl)) return false;
        VersionUrl that = (VersionUrl) o;
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
        String[] parts = version.split("\\.");
        for (int i = 0; i < result.length && i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException ignored) {
                // keep zero
            }
        }
        return result;
    }
}
