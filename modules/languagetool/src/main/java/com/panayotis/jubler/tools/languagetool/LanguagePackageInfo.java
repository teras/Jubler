/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.languagetool;

public class LanguagePackageInfo {
    private final String code;
    private final String name;
    private final String version;
    
    public LanguagePackageInfo(String code, String name, String version) {
        this.code = code;
        this.name = name;
        this.version = version;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDownloadUrl() {
        return String.format("https://repo1.maven.org/maven2/org/languagetool/language-%s/%s/language-%s-%s.jar",
                code, version, code, version);
    }
    
    public String getFileName() {
        return String.format("language-%s-%s.jar", code, version);
    }
    
    @Override
    public String toString() {
        return name;
    }
}
