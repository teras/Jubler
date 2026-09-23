/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>

class SubFile;

enum class ThemeVariation { AUTO, LIGHT, DARK };
QString themeVariationName(ThemeVariation v);
ThemeVariation themeVariationFromName(const QString &name);

// Application options with in-memory caching over Prefs, port of the Java
// `Options`. Every setter persists immediately. Keys are unchanged.
namespace Options {

constexpr int CURRENT_VERSION = 3;
constexpr int MAX_RECENTS = 10;

// (Re)load every cached value from Prefs. Called once at startup and after a
// preferences import/reset.
void load();

// --- Quality control ---------------------------------------------------------
int getErrorColor();          void setErrorColor(int mark);          // "options.errorcolor", default 1
bool isSpaceChars();          void setSpaceChars(bool);              // "options.spaceaschars", false
bool isNewlineChars();        void setNewlineChars(bool);            // "options.newlineaschars", false
bool isCompactSubs();         void setCompactSubs(bool);             // "options.compactsubs", true
bool isOtherChars();          void setOtherChars(bool);              // "options.otheraschars", true
int getMaxLines();            void setMaxLines(int);                 // "options.maxline", 2
float getFillPercent();       void setFillPercent(float);            // "options.fillpercent", 50 (clamped 0..100)
int getMaxLineLength();       void setMaxLineLength(int);            // "options.maxlinelength", 42
int getMaxCPS();              void setMaxCPS(int);                   // "options.maxcps", 21
float getMaxDuration();       void setMaxDuration(float);            // "options.maxduration", 7
float getMinDuration();       void setMinDuration(float);            // "options.minduration", 1

// --- UI ----------------------------------------------------------------------
float getScaling();           void setScaling(float);                // "ui.scaling.extra", 1.0
bool isTimestampTooltipsDisabled(); void setTimestampTooltipsDisabled(bool); // "ui.tooltips.timestamp.disabled"
ThemeVariation getThemeVariation(); void setThemeVariation(ThemeVariation); // "ui.theme.variation", AUTO
QString getLanguage();        void setLanguage(const QString &);     // "ui.language", "auto"

// --- Media -------------------------------------------------------------------
bool isVideoPreviewHardware(); void setVideoPreviewHardware(bool);   // "videopreview.hardware", false

// --- Encodings ---------------------------------------------------------------
bool isUnicodeCharset(const QString &name);
bool isSingleByteCharset(const QString &name);
// Default legacy encodings used by the auto-detector: an 8-bit single-byte
// charset ("default.encoding.8bit", ISO-8859-1) and an optional multi-byte
// one ("default.encoding.cjk").
QString getDefaultEncoding8bit();
QString getDefaultEncodingCjk();  // null when unset
// Remember a non-Unicode encoding the user chose, in the matching slot.
void rememberEncoding(const QString &enc);

// --- Recent files ("system.lastfile1".."10", packed SubFile) -----------------
void saveFileList(const QList<SubFile> &recents);   // most recent LAST, as the Java Stack
QList<SubFile> loadFileList();                       // most recent LAST

}  // namespace Options
