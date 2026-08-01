/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.languagetool;

import com.panayotis.jubler.os.SystemDependent;
import org.languagetool.Language;
import org.languagetool.Languages;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class LanguagePackageManager {
    
    private static final String LANGUAGETOOL_VERSION = "6.5";
    private static final String MAVEN_CENTRAL_BASE = "https://repo1.maven.org/maven2/org/languagetool/";
    
    private static Map<String, String> getKnownLanguages() {
        Map<String, String> knownLanguages = new LinkedHashMap<>();
        Set<String> processedCodes = new HashSet<>();
        
        for (Language lang : Languages.get()) {
            String code = lang.getShortCode();
            if (!processedCodes.contains(code)) {
                Language defaultVariant = lang.getDefaultLanguageVariant();
                if (defaultVariant != null && !defaultVariant.equals(lang)) {
                    knownLanguages.put(code, defaultVariant.getName());
                } else {
                    knownLanguages.put(code, lang.getName());
                }
                processedCodes.add(code);
            }
        }
        
        Map<String, String> fallbackNames = new LinkedHashMap<>();
        fallbackNames.put("ar", "Arabic");
        fallbackNames.put("ast", "Asturian");
        fallbackNames.put("be", "Belarusian");
        fallbackNames.put("br", "Breton");
        fallbackNames.put("ca", "Catalan");
        fallbackNames.put("crh", "Crimean Tatar");
        fallbackNames.put("da", "Danish");
        fallbackNames.put("de", "German");
        fallbackNames.put("el", "Greek");
        fallbackNames.put("en", "English");
        fallbackNames.put("eo", "Esperanto");
        fallbackNames.put("es", "Spanish");
        fallbackNames.put("fa", "Persian");
        fallbackNames.put("fr", "French");
        fallbackNames.put("ga", "Irish");
        fallbackNames.put("gl", "Galician");
        fallbackNames.put("is", "Icelandic");
        fallbackNames.put("it", "Italian");
        fallbackNames.put("ja", "Japanese");
        fallbackNames.put("km", "Khmer");
        fallbackNames.put("lt", "Lithuanian");
        fallbackNames.put("ml", "Malayalam");
        fallbackNames.put("nl", "Dutch");
        fallbackNames.put("pl", "Polish");
        fallbackNames.put("pt", "Portuguese");
        fallbackNames.put("ro", "Romanian");
        fallbackNames.put("ru", "Russian");
        fallbackNames.put("sk", "Slovak");
        fallbackNames.put("sl", "Slovenian");
        fallbackNames.put("sr", "Serbian");
        fallbackNames.put("sv", "Swedish");
        fallbackNames.put("ta", "Tamil");
        fallbackNames.put("tl", "Tagalog");
        fallbackNames.put("uk", "Ukrainian");
        fallbackNames.put("zh", "Chinese");
        
        for (Map.Entry<String, String> entry : fallbackNames.entrySet()) {
            if (!knownLanguages.containsKey(entry.getKey())) {
                knownLanguages.put(entry.getKey(), entry.getValue());
            }
        }
        
        return knownLanguages;
    }
    
    private static Set<String> fetchAvailableLanguageCodesFromMaven() {
        Set<String> languageCodes = new HashSet<>();
        
        try {
            URL url = new URL(MAVEN_CENTRAL_BASE);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("language-") && line.contains("/") && !line.contains("language-all") 
                                && !line.contains("language-de-DE-x-simple")) {
                            int start = line.indexOf("language-");
                            if (start != -1) {
                                int end = line.indexOf("/", start);
                                if (end != -1) {
                                    String langPackage = line.substring(start, end);
                                    String code = langPackage.substring("language-".length());
                                    if (code.length() >= 2 && code.length() <= 3) {
                                        languageCodes.add(code);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to fetch available languages from Maven Central: " + e.getMessage());
        }
        
        return languageCodes;
    }
    
    public static File getLanguagesDirectory() {
        // A trusted, app-owned directory (loaded silently at startup), separate from the gated
        // user "plugins" drop-in folder: these packs are fetched by us, not by the user.
        File dir = new File(SystemDependent.getAppSupportDirPath(), "langpacks");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    public static List<LanguagePackageInfo> getInstalledLanguages() {
        List<LanguagePackageInfo> installed = new ArrayList<>();
        Map<String, String> knownLanguages = getKnownLanguages();
        
        installed.add(new LanguagePackageInfo("en", knownLanguages.getOrDefault("en", "English") + " (built-in)", LANGUAGETOOL_VERSION));
        
        File langDir = getLanguagesDirectory();
        if (!langDir.exists()) {
            return installed;
        }
        
        File[] files = langDir.listFiles((dir, name) -> 
            name.startsWith("language-") && name.endsWith(".jar"));
        
        if (files != null) {
            for (File f : files) {
                String name = f.getName();
                String code = name.substring("language-".length(), name.lastIndexOf('-'));
                String languageName = knownLanguages.getOrDefault(code, code.toUpperCase());
                installed.add(new LanguagePackageInfo(code, languageName, LANGUAGETOOL_VERSION));
            }
        }
        
        return installed;
    }
    
    public static List<LanguagePackageInfo> getAvailableLanguages() {
        List<LanguagePackageInfo> available = new ArrayList<>();
        Set<String> installedCodes = new HashSet<>();
        
        for (LanguagePackageInfo installed : getInstalledLanguages()) {
            installedCodes.add(installed.getCode());
        }
        
        Set<String> availableCodes = fetchAvailableLanguageCodesFromMaven();
        Map<String, String> knownLanguages = getKnownLanguages();
        
        for (String code : availableCodes) {
            if (!installedCodes.contains(code)) {
                String languageName = knownLanguages.getOrDefault(code, code.toUpperCase());
                available.add(new LanguagePackageInfo(code, languageName, LANGUAGETOOL_VERSION));
            }
        }
        
        available.sort(Comparator.comparing(LanguagePackageInfo::getName));
        
        return available;
    }
    
    public static void downloadLanguage(LanguagePackageInfo language, DownloadProgressListener listener) throws IOException {
        File langDir = getLanguagesDirectory();
        File targetFile = new File(langDir, language.getFileName());
        
        if (targetFile.exists()) {
            throw new IOException("Language already installed: " + language.getName());
        }
        
        File tempFile = new File(langDir, language.getFileName() + ".tmp");
        List<File> downloadedDeps = new ArrayList<>();
        
        try {
            downloadFile(language.getDownloadUrl(), tempFile, listener);
            
            List<String> dependencies = fetchDependenciesFromPom(language);
            for (String depUrl : dependencies) {
                String fileName = depUrl.substring(depUrl.lastIndexOf('/') + 1);
                File depFile = new File(langDir, fileName);
                if (!depFile.exists()) {
                    File tempDepFile = new File(langDir, fileName + ".tmp");
                    downloadFile(depUrl, tempDepFile, listener);
                    downloadedDeps.add(tempDepFile);
                }
            }
            
            if (!tempFile.renameTo(targetFile)) {
                throw new IOException("Failed to finalize language installation");
            }
            
            for (File tempDep : downloadedDeps) {
                String finalName = tempDep.getName().substring(0, tempDep.getName().length() - 4);
                File finalDep = new File(langDir, finalName);
                if (!tempDep.renameTo(finalDep)) {
                    targetFile.delete();
                    throw new IOException("Failed to finalize dependency: " + finalName);
                }
            }
        } catch (IOException e) {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            for (File tempDep : downloadedDeps) {
                if (tempDep.exists()) {
                    tempDep.delete();
                }
            }
            throw e;
        }
    }
    
    private static List<String> fetchDependenciesFromPom(LanguagePackageInfo language) throws IOException {
        List<String> dependencies = new ArrayList<>();
        String pomUrl = String.format("%slanguage-%s/%s/language-%s-%s.pom",
                MAVEN_CENTRAL_BASE, language.getCode(), language.getVersion(), 
                language.getCode(), language.getVersion());
        
        URL url = new URL(pomUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        if (conn.getResponseCode() != 200) {
            return dependencies;
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(conn.getInputStream());
            doc.getDocumentElement().normalize();
            
            NodeList dependencyNodes = doc.getElementsByTagName("dependency");
            
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Element dep = (Element) dependencyNodes.item(i);
                
                Element parent = (Element) dep.getParentNode();
                if (parent.getTagName().equals("dependencyManagement")) {
                    continue;
                }
                
                String groupId = getElementText(dep, "groupId");
                String artifactId = getElementText(dep, "artifactId");
                String version = getElementText(dep, "version");
                String scope = getElementText(dep, "scope");
                
                if (groupId != null && artifactId != null) {
                    if (version == null) {
                        version = resolveVersion(groupId, artifactId);
                    }
                    
                    if (version != null) {
                        if (scope == null || "compile".equals(scope)) {
                            if (!"org.languagetool".equals(groupId) || 
                                (!artifactId.equals("languagetool-core") && 
                                 !artifactId.equals("hunspell-native-libs"))) {
                                String depUrl = String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
                                        groupId.replace('.', '/'), artifactId, 
                                        version, artifactId, version);
                                dependencies.add(depUrl);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to parse POM: " + e.getMessage(), e);
        }
        
        return dependencies;
    }
    
    private static String resolveVersion(String groupId, String artifactId) {
        Map<String, String> managedVersions = fetchManagedVersions(LANGUAGETOOL_VERSION);
        String version = managedVersions.get(groupId + ":" + artifactId);
        if (version != null) {
            return version;
        }
        
        try {
            String metadataUrl = String.format("https://repo1.maven.org/maven2/%s/%s/maven-metadata.xml",
                    groupId.replace('.', '/'), artifactId);
            
            URL url = new URL(metadataUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (conn.getResponseCode() != 200) {
                return null;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("<release>") && line.contains("</release>")) {
                        return line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                    }
                    if (line.startsWith("<latest>") && line.contains("</latest>")) {
                        return line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to resolve version for " + groupId + ":" + artifactId + ": " + e.getMessage());
        }
        return null;
    }
    
    private static Map<String, String> fetchManagedVersions(String languageToolVersion) {
        Map<String, String> versions = new HashMap<>();
        try {
            String parentPomUrl = String.format("%slanguagetool-parent/%s/languagetool-parent-%s.pom",
                    MAVEN_CENTRAL_BASE, languageToolVersion, languageToolVersion);
            
            URL url = new URL(parentPomUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            if (conn.getResponseCode() != 200) {
                return versions;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(conn.getInputStream());
            doc.getDocumentElement().normalize();
            
            Map<String, String> properties = new HashMap<>();
            NodeList propertiesNodes = doc.getElementsByTagName("properties");
            if (propertiesNodes.getLength() > 0) {
                Element propertiesElement = (Element) propertiesNodes.item(0);
                NodeList children = propertiesElement.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        Element elem = (Element) child;
                        String propName = elem.getTagName();
                        String propValue = elem.getTextContent();
                        properties.put("${" + propName + "}", propValue);
                    }
                }
            }
            
            NodeList depMgmtNodes = doc.getElementsByTagName("dependencyManagement");
            if (depMgmtNodes.getLength() > 0) {
                Element depMgmt = (Element) depMgmtNodes.item(0);
                NodeList dependencies = depMgmt.getElementsByTagName("dependency");
                
                for (int i = 0; i < dependencies.getLength(); i++) {
                    Element dep = (Element) dependencies.item(i);
                    String groupId = getElementText(dep, "groupId");
                    String artifactId = getElementText(dep, "artifactId");
                    String version = getElementText(dep, "version");
                    
                    if (groupId != null && artifactId != null && version != null) {
                        for (Map.Entry<String, String> prop : properties.entrySet()) {
                            version = version.replace(prop.getKey(), prop.getValue());
                        }
                        versions.put(groupId + ":" + artifactId, version);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch managed versions from parent POM: " + e.getMessage());
        }
        return versions;
    }
    
    private static String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }
    
    private static void downloadFile(String urlString, File targetFile, DownloadProgressListener listener) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to download " + urlString + ": HTTP " + responseCode);
        }
        
        long fileSize = conn.getContentLengthLong();
        
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(targetFile)) {
            
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                if (listener != null && listener.isCancelled()) {
                    targetFile.delete();
                    throw new IOException("Download cancelled");
                }
                
                out.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                
                if (listener != null) {
                    int progress = fileSize > 0 ? (int) ((downloaded * 100) / fileSize) : 0;
                    listener.onProgress(progress, downloaded, fileSize);
                }
            }
        } catch (IOException e) {
            if (targetFile.exists()) {
                targetFile.delete();
            }
            throw e;
        }
    }
    

    
    public static boolean deleteLanguage(LanguagePackageInfo language) {
        if ("en".equals(language.getCode())) {
            return false;
        }
        
        File langDir = getLanguagesDirectory();
        File langFile = new File(langDir, language.getFileName());
        
        return langFile.exists() && langFile.delete();
    }
    
    public interface DownloadProgressListener {
        void onProgress(int percent, long downloaded, long total);
        boolean isCancelled();
    }
}
