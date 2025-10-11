package com.panayotis.jubler.options;

public class LanguageOption {
    private final String code;
    private final String displayName;
    private final String iconName;

    public LanguageOption(String code, String displayName, String iconName) {
        this.code = code;
        this.displayName = displayName;
        this.iconName = iconName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconName() {
        return iconName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanguageOption that = (LanguageOption) o;
        return code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
