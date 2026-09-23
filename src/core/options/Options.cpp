/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/options/Options.h"

#include <QFileInfo>
#include "core/os/Charsets.h"

#include "core/options/Prefs.h"
#include "core/subs/SubFile.h"

QString themeVariationName(ThemeVariation v) {
    switch (v) {
        case ThemeVariation::LIGHT: return QStringLiteral("LIGHT");
        case ThemeVariation::DARK: return QStringLiteral("DARK");
        default: return QStringLiteral("AUTO");
    }
}

ThemeVariation themeVariationFromName(const QString &name) {
    if (name == QLatin1String("LIGHT")) return ThemeVariation::LIGHT;
    if (name == QLatin1String("DARK")) return ThemeVariation::DARK;
    return ThemeVariation::AUTO;
}

namespace Options {

namespace {
const QString ERRORCOLOR_TAG = QStringLiteral("options.errorcolor");
const QString SPACECHARS_TAG = QStringLiteral("options.spaceaschars");
const QString NEWLINECHARS_TAG = QStringLiteral("options.newlineaschars");
const QString COMPACTSUBS_TAG = QStringLiteral("options.compactsubs");
const QString OTHERCHARS_TAG = QStringLiteral("options.otheraschars");
const QString MAXLINES_TAG = QStringLiteral("options.maxline");
const QString FILLPERCENT_TAG = QStringLiteral("options.fillpercent");
const QString MAXLINELENGTH_TAG = QStringLiteral("options.maxlinelength");
const QString MAXCPS_TAG = QStringLiteral("options.maxcps");
const QString MAXDURATION_TAG = QStringLiteral("options.maxduration");
const QString MINDURATION_TAG = QStringLiteral("options.minduration");
const QString SYSTEM_LASTFILE = QStringLiteral("system.lastfile");
const QString SCALING_TAG = QStringLiteral("ui.scaling.extra");
const QString OLD_SCALING_TAG = QStringLiteral("ui.scaling.factor");
const QString TIMESTAMP_TOOLTIPS_DISABLED = QStringLiteral("ui.tooltips.timestamp.disabled");
const QString USE_THEME_VARIATION = QStringLiteral("ui.theme.variation");
const QString LANGUAGE_TAG = QStringLiteral("ui.language");
const QString VIDEOPREVIEW_HARDWARE_TAG = QStringLiteral("videopreview.hardware");
const QString DEFAULT_ENCODING_8BIT_TAG = QStringLiteral("default.encoding.8bit");
const QString DEFAULT_ENCODING_CJK_TAG = QStringLiteral("default.encoding.cjk");

struct State {
    int errorColor = 1;
    bool spaceChars = false, newlineChars = false, compactSubs = true, otherChars = true;
    int maxLines = 2;
    float fillPercent = 50;
    int maxLineLength = 42;
    int maxCPS = 21;
    float maxDuration = 7, minDuration = 1;
    float scaling = 1.0f;
    bool timestampTooltipsDisabled = false;
    ThemeVariation themeVariation = ThemeVariation::AUTO;
    QString language = QStringLiteral("auto");
    bool videoPreviewHardware = false;
    QString defaultEncoding8bit = QStringLiteral("ISO-8859-1");
    QString defaultEncodingCjk;
    bool loaded = false;
};

State &st() {
    static State s;
    if (!s.loaded) {
        s.loaded = true;
        load();
    }
    return s;
}

// Up to 10.1 the preferences held "default.encoding1..3"; fold them into the
// 8-bit / CJK slots once.
void migrateDefaultEncoding() {
    if (!Prefs::getString(DEFAULT_ENCODING_8BIT_TAG, QString()).isNull())
        return;
    QString single, cjk;
    for (int i = 1; i <= 3; ++i) {
        const QString e = Prefs::getString(QStringLiteral("default.encoding%1").arg(i), QString());
        if (e.isNull() || isUnicodeCharset(e))
            continue;
        if (isSingleByteCharset(e)) {
            if (single.isNull()) single = e;
        } else if (cjk.isNull())
            cjk = e;
    }
    Prefs::set(DEFAULT_ENCODING_8BIT_TAG, single.isNull() ? QStringLiteral("ISO-8859-1") : single);
    if (!cjk.isNull())
        Prefs::set(DEFAULT_ENCODING_CJK_TAG, cjk);
}
}  // namespace

void load() {
    State &s = st();
    s.loaded = true;
    s.errorColor = Prefs::getInt(ERRORCOLOR_TAG, 1);
    s.spaceChars = Prefs::getBoolean(SPACECHARS_TAG, false);
    s.newlineChars = Prefs::getBoolean(NEWLINECHARS_TAG, false);
    s.compactSubs = Prefs::getBoolean(COMPACTSUBS_TAG, true);
    s.otherChars = Prefs::getBoolean(OTHERCHARS_TAG, true);
    s.maxLines = Prefs::getInt(MAXLINES_TAG, 2);
    s.fillPercent = Prefs::getFloat(FILLPERCENT_TAG, 50);
    s.maxLineLength = Prefs::getInt(MAXLINELENGTH_TAG, 42);
    s.maxCPS = Prefs::getInt(MAXCPS_TAG, 21);
    s.maxDuration = Prefs::getFloat(MAXDURATION_TAG, 7);
    s.minDuration = Prefs::getFloat(MINDURATION_TAG, 1);
    // The old total-scale key is abandoned (it double-scaled on systems that scale on their own).
    Prefs::remove(OLD_SCALING_TAG);
    s.scaling = Prefs::getFloat(SCALING_TAG, 1.0f);
    s.timestampTooltipsDisabled = Prefs::getBoolean(TIMESTAMP_TOOLTIPS_DISABLED, false);
    s.themeVariation = themeVariationFromName(Prefs::getString(USE_THEME_VARIATION, QStringLiteral("AUTO")));
    s.language = Prefs::getString(LANGUAGE_TAG, QStringLiteral("auto"));
    s.videoPreviewHardware = Prefs::getBoolean(VIDEOPREVIEW_HARDWARE_TAG, false);
    migrateDefaultEncoding();
    s.defaultEncoding8bit = Prefs::getString(DEFAULT_ENCODING_8BIT_TAG, QStringLiteral("ISO-8859-1"));
    if (!isSingleByteCharset(s.defaultEncoding8bit))  // the floor must always decode → single-byte
        s.defaultEncoding8bit = QStringLiteral("ISO-8859-1");
    s.defaultEncodingCjk = Prefs::getString(DEFAULT_ENCODING_CJK_TAG, QString());
}

bool isUnicodeCharset(const QString &name) {
    if (name.isNull()) return false;
    const QString u = name.toUpper();
    return u.startsWith(QLatin1String("UTF-")) || u.startsWith(QLatin1String("UTF8"))
        || u.startsWith(QLatin1String("UTF16")) || u.startsWith(QLatin1String("UTF32"))
        || u.startsWith(QLatin1String("X-UTF")) || u == QLatin1String("UTF");
}

bool isSingleByteCharset(const QString &name) {
    // Java: Charset.forName(name).newEncoder().maxBytesPerChar() == 1.
    return !isUnicodeCharset(name) && Charsets::isSingleByte(name);
}

#define OPT_SIMPLE(TYPE, GETTER, SETTER, FIELD, TAG) \
    TYPE GETTER() { return st().FIELD; }              \
    void SETTER(TYPE v) { Prefs::set(TAG, st().FIELD = v); }

OPT_SIMPLE(int, getErrorColor, setErrorColor, errorColor, ERRORCOLOR_TAG)
OPT_SIMPLE(bool, isSpaceChars, setSpaceChars, spaceChars, SPACECHARS_TAG)
OPT_SIMPLE(bool, isNewlineChars, setNewlineChars, newlineChars, NEWLINECHARS_TAG)
OPT_SIMPLE(bool, isCompactSubs, setCompactSubs, compactSubs, COMPACTSUBS_TAG)
OPT_SIMPLE(bool, isOtherChars, setOtherChars, otherChars, OTHERCHARS_TAG)
OPT_SIMPLE(int, getMaxLines, setMaxLines, maxLines, MAXLINES_TAG)
OPT_SIMPLE(int, getMaxLineLength, setMaxLineLength, maxLineLength, MAXLINELENGTH_TAG)
OPT_SIMPLE(int, getMaxCPS, setMaxCPS, maxCPS, MAXCPS_TAG)
OPT_SIMPLE(float, getMaxDuration, setMaxDuration, maxDuration, MAXDURATION_TAG)
OPT_SIMPLE(float, getMinDuration, setMinDuration, minDuration, MINDURATION_TAG)
OPT_SIMPLE(float, getScaling, setScaling, scaling, SCALING_TAG)
OPT_SIMPLE(bool, isTimestampTooltipsDisabled, setTimestampTooltipsDisabled, timestampTooltipsDisabled, TIMESTAMP_TOOLTIPS_DISABLED)
OPT_SIMPLE(bool, isVideoPreviewHardware, setVideoPreviewHardware, videoPreviewHardware, VIDEOPREVIEW_HARDWARE_TAG)

float getFillPercent() { return st().fillPercent; }
void setFillPercent(float value) {
    if (value < 0) value = 0;
    else if (value > 100) value = 100;
    Prefs::set(FILLPERCENT_TAG, st().fillPercent = value);
}

ThemeVariation getThemeVariation() { return st().themeVariation; }
void setThemeVariation(ThemeVariation v) {
    Prefs::set(USE_THEME_VARIATION, themeVariationName(v));
    st().themeVariation = v;
}

QString getLanguage() { return st().language; }
void setLanguage(const QString &lang) {
    Prefs::set(LANGUAGE_TAG, lang);
    st().language = lang;
}

QString getDefaultEncoding8bit() { return st().defaultEncoding8bit; }
QString getDefaultEncodingCjk() { return st().defaultEncodingCjk; }

void rememberEncoding(const QString &enc) {
    if (enc.isEmpty() || isUnicodeCharset(enc))
        return;
    if (isSingleByteCharset(enc)) {
        st().defaultEncoding8bit = enc;
        Prefs::set(DEFAULT_ENCODING_8BIT_TAG, enc);
    } else {
        st().defaultEncodingCjk = enc;
        Prefs::set(DEFAULT_ENCODING_CJK_TAG, enc);
    }
}

void saveFileList(const QList<SubFile> &recents) {
    int pos = recents.size();
    int counter = 0;
    while (pos > 0 && counter < MAX_RECENTS) {
        --pos;
        const SubFile &sfile = recents.at(pos);
        const QFileInfo f(sfile.getSaveFile());
        if (f.exists() && f.isFile()) {
            ++counter;
            Prefs::set(SYSTEM_LASTFILE + QString::number(counter), sfile.getPacked());
        }
    }
    while (counter < MAX_RECENTS)
        Prefs::remove(SYSTEM_LASTFILE + QString::number(++counter));
}

QList<SubFile> loadFileList() {
    QList<SubFile> files;
    for (int i = MAX_RECENTS; i > 0; --i) {
        SubFile sf;
        if (!SubFile::unpack(Prefs::getString(SYSTEM_LASTFILE + QString::number(i), QString()), sf))
            continue;
        const QFileInfo f(sf.getSaveFile());
        if (f.exists() && f.isReadable() && f.isFile())
            files.append(sf);
    }
    return files;
}

}  // namespace Options
