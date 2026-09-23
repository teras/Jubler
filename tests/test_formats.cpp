/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// Format layer tests: every format parses its fixtures and round-trips.
#include "TestSupport.h"

#include <QDir>
#include <QElapsedTimer>
#include <QFileInfo>

#include "core/formats/SubFormat.h"
#include "core/formats/TextSubFormat.h"
#include "core/media/MediaFile.h"
#include "core/options/Options.h"
#include "core/formats/SimpleStyledTextSubFormat.h"
#include "core/os/Charsets.h"
#include "core/os/FileCommunicator.h"
#include <QStringEncoder>
#include "core/subs/Subtitles.h"

static QString g_fixtures;
static void testEncodingDetection();
static void testTextFromAnySource();
static void testSaveLoadFile();
static void testAlignmentFixtures();
static void testFontScaling();
static void testCorpusAndRegressions();
static void testCharsets();
static void testW3CFamily();
static void testTextFormats();
static void testSrtInTheWild();

static std::unique_ptr<Subtitles> load(const QString &name, QString *fmtName = nullptr) {
    const QString path = g_fixtures + QLatin1Char('/') + name;
    SubFile sfile(path, SubFile::EXTENSION_GIVEN);
    const QString data = FileCommunicator::load(sfile);   // the application's path: decoding, line endings
    CHECK(!data.isEmpty(), qPrintable("fixture readable: " + name));
    auto subs = std::make_unique<Subtitles>(sfile);
    subs->populate(sfile, data, false);
    if (fmtName)
        *fmtName = sfile.getFormat() ? sfile.getFormat()->getName() : QString();
    return subs;
}

static QString render(Subtitles &subs, const QString &fmtName) {
    SubFormatPtr f = Availabilities::formats().findFromName(fmtName)->newInstance();
    f->updateFormat(subs.getSubFile());
    return static_cast<AbstractGenericTextSubFormat *>(f.get())->render(subs, nullptr);
}

static void testSubRip() {
    QString fmt;
    auto subs = load(QStringLiteral("simple.srt"), &fmt);
    CHECK_EQ(fmt, QStringLiteral("SubRip"), "simple.srt detected as SubRip");
    CHECK(subs->size() > 0, "simple.srt has entries");
    auto comp = load(QStringLiteral("comprehensive.srt"), &fmt);
    CHECK_EQ(fmt, QStringLiteral("SubRip"), "comprehensive.srt detected as SubRip");
    CHECK(comp->size() > 5, "comprehensive.srt has entries");

    // Hand-written cases.
    const QString src = QStringLiteral("1\n00:00:01,000 --> 00:00:02,500\nHello <i>world</i>\n\n2\n00:00:03,000 --> 00:00:04,000\n\n3\n00:00:05,000 --> 00:00:06,000\n<font color=\"#ff0000\">Red</font> plain\n\n");
    SubFile sf(QStringLiteral("/tmp/x.srt"), SubFile::EXTENSION_GIVEN);
    Subtitles s(sf);
    s.populate(sf, src, false);
    CHECK_EQ(s.size(), 3, "three entries incl. an empty one");
    CHECK_EQ(s.get(0)->getText(), QStringLiteral("Hello world"), "tags stripped");
    CHECK(s.get(0)->getStyleover(StyleType::ITALIC) != nullptr, "italic override recorded");
    CHECK_EQ(s.get(0)->getStyleover(StyleType::ITALIC)->dump(), QStringLiteral("{(true,6)}"), "italic from 6 to end");
    CHECK_EQ(s.get(1)->getText(), QString(QLatin1String("")), "empty cue text");
    CHECK_EQ(s.get(2)->getText(), QStringLiteral("Red plain"), "font tag stripped");
    CHECK_EQ(s.get(2)->getStyleover(StyleType::PRIMARY)->dump(), QStringLiteral("{(ffff0000,0)(ffffffff,3)}"), "colour run ends at </font>");
    const QString out = render(s, QStringLiteral("SubRip"));
    CHECK_EQ(out, QStringLiteral("1\n00:00:01,000 --> 00:00:02,500\nHello <i>world</i>\n\n2\n00:00:03,000 --> 00:00:04,000\n\n\n3\n00:00:05,000 --> 00:00:06,000\n<font color=\"#ff0000\">Red</font> plain\n"), "SubRip round trip");
}

static void testWebVTT() {
    QString fmt;
    auto subs = load(QStringLiteral("simple.vtt"), &fmt);
    CHECK_EQ(fmt, QStringLiteral("WebVTT"), "simple.vtt detected as WebVTT");
    CHECK(subs->size() > 0, "simple.vtt has entries");
    auto comp = load(QStringLiteral("comprehensive.vtt"), &fmt);
    CHECK_EQ(fmt, QStringLiteral("WebVTT"), "comprehensive.vtt detected");
    CHECK(comp->size() > 5, "comprehensive.vtt has entries");

    const QString src = QStringLiteral("WEBVTT - title\n\n1\n00:01.000 --> 00:02.000 line:10% align:end\nTop <b>bold</b><br/>second\n\n00:00:03.000 --> 00:00:04.000\n<v Speaker>Hi <c.red>red</c> there\n\n");
    SubFile sf(QStringLiteral("/tmp/x.vtt"), SubFile::EXTENSION_GIVEN);
    Subtitles s(sf);
    s.populate(sf, src, false);
    CHECK_EQ(s.size(), 2, "two cues");
    CHECK_EQ(s.get(0)->getText(), QStringLiteral("Top bold\nsecond"), "br converted");
    auto dir = s.get(0)->overValue(StyleType::DIRECTION, 0, 3);
    CHECK(dir && std::get<Direction>(*dir) == Direction::TOPRIGHT, "line:10% + align:end → TOPRIGHT override");
    CHECK_EQ(s.get(1)->getText(), QStringLiteral("Hi red there"), "voice and class tags stripped");
    CHECK_EQ(s.get(1)->getStyleover(StyleType::PRIMARY)->dump(), QStringLiteral("{(ffff0000,3)(ffffffff,6)}"), "class colour run");
    const QString out = render(s, QStringLiteral("WebVTT"));
    CHECK(out.startsWith(QStringLiteral("WEBVTT\n\n00:00:01.000 --> 00:00:02.000 line:10% align:end\nTop <b>bold</b>\nsecond\n\n")), "WebVTT round trip header/first cue");
    CHECK(out.contains(QStringLiteral("00:00:03.000 --> 00:00:04.000\nHi <c.red>red</c> there")) || out.contains(QStringLiteral("<font color=\"#ff0000\">red</font>")), "WebVTT colour written");
}


// ---- alignment / positioning fixtures (the Java basetextsubs tests) --------

static Direction effDir(const SubEntry &e) {
    const Direction base = e.getStyle() ? e.getStyle()->direction() : Direction::BOTTOM;
    if (const Styleover *o = e.getStyleover(StyleType::DIRECTION); o && o->size()) {
        Styleover c(*o);
        auto v = c.getValue(0, e.getText().length(), StyleValue(base), e.getText());
        if (v && std::holds_alternative<Direction>(*v)) return std::get<Direction>(*v);
    }
    return base;
}

static std::unique_ptr<Subtitles> reload(Subtitles &subs, const QString &fmtName, const QString &ext) {
    const QString out = render(subs, fmtName);
    SubFile sf(QStringLiteral("/tmp/roundtrip.") + ext, SubFile::EXTENSION_GIVEN);
    auto r = std::make_unique<Subtitles>(sf);
    r->populate(sf, out, false);
    return r;
}

static void checkDirs(const Subtitles &s, const QList<Direction> &expected, const char *what) {
    CHECK_EQ(s.size(), int(expected.size()), what);
    for (int i = 0; i < std::min(s.size(), int(expected.size())); ++i)
        CHECK_EQ(directionName(effDir(*s.get(i))), directionName(expected[i]), qPrintable(QStringLiteral("%1 entry %2 direction").arg(QString::fromUtf8(what)).arg(i)));
}

static void testAlignmentFixtures() {
    using D = Direction;
    const QList<D> nine = {D::TOPLEFT, D::TOP, D::TOPRIGHT, D::LEFT, D::CENTER, D::RIGHT, D::BOTTOMLEFT, D::BOTTOM, D::BOTTOMRIGHT, D::BOTTOM};
    QString fmt;
    auto itt = load(QStringLiteral("all_directions_test.itt"), &fmt);
    CHECK_EQ(fmt, QStringLiteral("iTunes Timed Text"), "all_directions detected as ITT");
    checkDirs(*itt, nine, "all_directions load");
    const QStringList nineTexts{QStringLiteral("Top Left"), QStringLiteral("Top Center"), QStringLiteral("Top Right"), QStringLiteral("Center Left"), QStringLiteral("Center"), QStringLiteral("Center Right"),
                                QStringLiteral("Bottom Left"), QStringLiteral("Bottom Center (explicit)"), QStringLiteral("Bottom Right"), QStringLiteral("No region (uses default style = BOTTOM)")};
    for (int i = 0; i < nineTexts.size() && i < itt->size(); ++i) CHECK_EQ(itt->get(i)->getText(), nineTexts[i], "all_directions text");
    const QString saved = render(*itt, QStringLiteral("iTunes Timed Text"));
    for (const char *id : {"topleft", "top", "topright", "left", "center", "right", "bottomleft", "bottomright"}) {
        CHECK(saved.contains(QStringLiteral("xml:id=\"%1\"").arg(QLatin1String(id))), qPrintable(QStringLiteral("region %1 defined").arg(QLatin1String(id))));
        CHECK(saved.contains(QStringLiteral("region=\"%1\"").arg(QLatin1String(id))), qPrintable(QStringLiteral("region %1 referenced").arg(QLatin1String(id))));
    }
    CHECK(!saved.contains(QStringLiteral("xml:id=\"bottom\"")), "no bottom region");
    CHECK(!saved.contains(QStringLiteral("region=\"bottom\"")), "no bottom reference");
    CHECK(saved.contains(QStringLiteral("ttp:frameRate=\"24\"")), "ITT frame rate written from the file");
    auto ittR = reload(*itt, QStringLiteral("iTunes Timed Text"), QStringLiteral("itt"));
    checkDirs(*ittR, nine, "all_directions reload");
    for (int i = 0; i < nineTexts.size() && i < ittR->size(); ++i) CHECK_EQ(ittR->get(i)->getText(), nineTexts[i], "all_directions reload text");

    auto ass = load(QStringLiteral("ass_alignment_test.ass"), &fmt);
    CHECK_EQ(fmt, QStringLiteral("AdvancedSubStation"), "ass_alignment detected");
    const QList<D> six = {D::BOTTOM, D::TOPLEFT, D::TOP, D::TOPRIGHT, D::CENTER, D::TOPRIGHT};
    checkDirs(*ass, six, "ass_alignment load");
    const QString assOut = render(*ass, QStringLiteral("AdvancedSubStation"));
    for (const char *t : {"{\\an7}", "{\\an8}", "{\\an9}", "{\\an5}"})
        CHECK(assOut.contains(QLatin1String(t)), qPrintable(QStringLiteral("ASS output has %1").arg(QLatin1String(t))));
    CHECK_EQ(assOut.count(QStringLiteral("{\\an7}")), 1, "an7 once");
    checkDirs(*reload(*ass, QStringLiteral("AdvancedSubStation"), QStringLiteral("ass")), six, "ass_alignment reload");

    auto complex = load(QStringLiteral("ass_complex_alignment.ass"), &fmt);
    const QList<D> twenty = {D::BOTTOM, D::BOTTOMLEFT, D::BOTTOM, D::BOTTOMRIGHT, D::LEFT, D::CENTER, D::RIGHT, D::TOPLEFT, D::TOP, D::TOPRIGHT,
                             D::TOPRIGHT, D::TOPLEFT, D::TOP, D::BOTTOMLEFT, D::TOPLEFT, D::CENTER, D::TOPLEFT, D::BOTTOM, D::CENTER, D::TOPRIGHT};
    CHECK_EQ(complex->size(), 24, "ass_complex 24 entries");
    for (int i = 0; i < 20 && i < complex->size(); ++i)
        CHECK_EQ(directionName(effDir(*complex->get(i))), directionName(twenty[i]), qPrintable(QStringLiteral("ass_complex entry %1").arg(i)));
    const QString complexOut = render(*complex, QStringLiteral("AdvancedSubStation"));
    for (const char *t : {"{\\an1}", "{\\an3}", "{\\an4}", "{\\an5}", "{\\an6}", "{\\an7}", "{\\an8}", "{\\an9}"})
        CHECK(complexOut.contains(QLatin1String(t)), qPrintable(QStringLiteral("ass_complex output has %1").arg(QLatin1String(t))));
    for (const char *t : {"{\\a1}", "{\\a5}", "{\\a10}"})
        CHECK(!complexOut.contains(QLatin1String(t)), qPrintable(QStringLiteral("ass_complex output has no legacy %1").arg(QLatin1String(t))));
    for (const QString &line : complexOut.split(QLatin1Char('\n')))
        if (line.contains(QLatin1String("Bottom-center override (same as style)"))) CHECK(!line.contains(QLatin1String("{\\an2}")), "same-as-style override is not written");
    auto complexR = reload(*complex, QStringLiteral("AdvancedSubStation"), QStringLiteral("ass"));
    CHECK_EQ(complexR->size(), 24, "ass_complex reload count");
    for (int i = 0; i < 20 && i < complexR->size(); ++i)
        CHECK_EQ(directionName(effDir(*complexR->get(i))), directionName(twenty[i]), qPrintable(QStringLiteral("ass_complex reload entry %1").arg(i)));

    auto ittc = load(QStringLiteral("itt_complex_alignment.itt"), &fmt);
    checkDirs(*ittc, nine, "itt_complex load");
    const QString ittcOut = render(*ittc, QStringLiteral("iTunes Timed Text"));
    for (const char *id : {"topleft", "top", "topright", "left", "center", "right", "bottomleft", "bottomright"})
        CHECK(ittcOut.contains(QStringLiteral("region=\"%1\"").arg(QLatin1String(id))), qPrintable(QStringLiteral("itt_complex references %1").arg(QLatin1String(id))));
    for (const char *id : {"topleft", "top", "topright", "left", "center", "right", "bottomleft", "bottomright"})
        CHECK(ittcOut.contains(QStringLiteral("xml:id=\"%1\"").arg(QLatin1String(id))), qPrintable(QStringLiteral("itt_complex defines %1").arg(QLatin1String(id))));
    CHECK(!ittcOut.contains(QStringLiteral("xml:id=\"bottom\"")), "itt_complex no bottom region");
    checkDirs(*reload(*ittc, QStringLiteral("iTunes Timed Text"), QStringLiteral("itt")), nine, "itt_complex reload");

    auto extreme = load(QStringLiteral("ass_extreme_alignment.ass"), &fmt);
    CHECK_EQ(extreme->size(), 42, "ass_extreme 42 entries");
    const QList<D> ex13 = {D::BOTTOM, D::TOPLEFT, D::TOP, D::TOPRIGHT, D::LEFT, D::CENTER, D::RIGHT, D::BOTTOMLEFT, D::BOTTOMRIGHT, D::TOPLEFT, D::BOTTOMRIGHT, D::BOTTOMLEFT, D::TOP,
                           D::BOTTOM, D::TOPLEFT, D::CENTER, D::TOPRIGHT, D::RIGHT, D::CENTER, D::TOPLEFT, D::CENTER, D::TOPRIGHT, D::TOPLEFT};
    const QList<D> exRest = {D::TOPLEFT, D::BOTTOMLEFT, D::CENTER, D::TOPRIGHT, D::TOPRIGHT, D::TOP, D::RIGHT, D::TOPLEFT, D::BOTTOMLEFT, D::BOTTOMLEFT, D::RIGHT, D::TOPRIGHT,
                             D::BOTTOM, D::CENTER, D::TOPLEFT, D::BOTTOM, D::BOTTOMRIGHT, D::TOPLEFT, D::CENTER};
    const QList<D> exAll = ex13 + exRest;
    for (int i = 0; i < exAll.size() && i < extreme->size(); ++i)
        CHECK_EQ(directionName(effDir(*extreme->get(i))), directionName(exAll[i]), qPrintable(QStringLiteral("ass_extreme entry %1").arg(i)));
    const QStringList exStyles{QStringLiteral("Default"), QStringLiteral("TopLeft"), QStringLiteral("TopCenter"), QStringLiteral("TopRight"), QStringLiteral("MiddleLeft"), QStringLiteral("MiddleCenter"),
                               QStringLiteral("MiddleRight"), QStringLiteral("BottomLeft"), QStringLiteral("BottomRight"), QStringLiteral("Default"), QStringLiteral("TopLeft"), QStringLiteral("MiddleCenter"), QStringLiteral("BottomRight")};
    for (int i = 0; i < exStyles.size() && i < extreme->size(); ++i)
        CHECK_EQ(extreme->get(i)->getStyle()->getName(), exStyles[i], qPrintable(QStringLiteral("ass_extreme style %1").arg(i)));
    const QString extremeOut = render(*extreme, QStringLiteral("AdvancedSubStation"));
    for (const QString &line : extremeOut.split(QLatin1Char('\n'))) {
        if (line.contains(QLatin1String("Default style (2) \"overridden\" to same (2)"))) CHECK(!line.contains(QLatin1String("{\\an2}")), "extreme 13 no an2");
        if (line.contains(QLatin1String("TopLeft style (7) \"overridden\" to same (7)"))) CHECK(!line.contains(QLatin1String("{\\an7}")), "extreme 14 no an7");
        if (line.contains(QLatin1String("MiddleCenter style (5) \"overridden\" to same (5)"))) CHECK(!line.contains(QLatin1String("{\\an5}")), "extreme 15 no an5");
    }
    for (const char *t : {"{\\a1}", "{\\a5}", "{\\a10}"}) CHECK(!extremeOut.contains(QLatin1String(t)), "extreme no legacy tags");
    for (const char *st : {"Style: TopLeft,", "Style: MiddleCenter,", "Style: BottomRight,"}) CHECK(extremeOut.contains(QLatin1String(st)), "extreme style definitions kept");
    auto extremeR = reload(*extreme, QStringLiteral("AdvancedSubStation"), QStringLiteral("ass"));
    CHECK_EQ(extremeR->size(), 42, "ass_extreme reload count");
    for (int i = 0; i < extreme->size() && i < extremeR->size(); ++i) {
        CHECK_EQ(directionName(effDir(*extremeR->get(i))), directionName(effDir(*extreme->get(i))), qPrintable(QStringLiteral("ass_extreme reload entry %1").arg(i)));
        CHECK_EQ(extremeR->get(i)->getStyle()->getName(), extreme->get(i)->getStyle()->getName(), qPrintable(QStringLiteral("ass_extreme reload style %1").arg(i)));
    }

    auto ittx = load(QStringLiteral("itt_extreme_alignment.itt"), &fmt);
    CHECK_EQ(ittx->size(), 43, "itt_extreme 43 entries");
    const QList<D> ix = {D::BOTTOM, D::BOTTOM, D::TOPLEFT, D::TOP, D::TOPRIGHT, D::LEFT, D::CENTER, D::RIGHT, D::BOTTOMLEFT, D::BOTTOM, D::BOTTOMRIGHT, D::TOP, D::CENTER, D::TOPLEFT, D::TOPLEFT, D::TOPLEFT,
                         D::TOPLEFT, D::TOP, D::BOTTOMRIGHT, D::CENTER, D::BOTTOMRIGHT, D::TOPLEFT, D::TOPLEFT, D::TOPRIGHT, D::BOTTOM, D::TOP, D::CENTER, D::BOTTOMLEFT, D::TOPRIGHT, D::LEFT, D::CENTER};
    for (int i = 0; i < ix.size() && i < ittx->size(); ++i)
        CHECK_EQ(directionName(effDir(*ittx->get(i))), directionName(ix[i]), qPrintable(QStringLiteral("itt_extreme entry %1").arg(i)));
    const QString ittxOut = render(*ittx, QStringLiteral("iTunes Timed Text"));
    for (const char *id : {"topleft", "top", "topright", "left", "center", "right", "bottomleft", "bottomright"})
        CHECK(ittxOut.contains(QStringLiteral("xml:id=\"%1\"").arg(QLatin1String(id))), qPrintable(QStringLiteral("itt_extreme defines %1").arg(QLatin1String(id))));
    CHECK(!ittxOut.contains(QStringLiteral("xml:id=\"bottom\"")) && !ittxOut.contains(QStringLiteral("xml:id=\"default_bottom\"")), "itt_extreme no bottom/custom region names");
    auto ittxR = reload(*ittx, QStringLiteral("iTunes Timed Text"), QStringLiteral("itt"));
    CHECK_EQ(ittxR->size(), 43, "itt_extreme reload count");
    for (int i = 0; i < ittx->size() && i < ittxR->size(); ++i)
        CHECK_EQ(directionName(effDir(*ittxR->get(i))), directionName(effDir(*ittx->get(i))), qPrintable(QStringLiteral("itt_extreme reload entry %1").arg(i)));

    auto sbr = load(QStringLiteral("itt_style_based_regions.itt"), &fmt);
    checkDirs(*sbr, {D::TOPLEFT, D::BOTTOM, D::BOTTOM, D::CENTER, D::TOPLEFT}, "style-based regions load");
    const QString sbrOut = render(*sbr, QStringLiteral("iTunes Timed Text"));
    CHECK(sbrOut.contains(QStringLiteral("xml:id=\"topleft\"")) && sbrOut.contains(QStringLiteral("xml:id=\"center\"")) && !sbrOut.contains(QStringLiteral("xml:id=\"bottom\"")), "style-based regions written");
    checkDirs(*reload(*sbr, QStringLiteral("iTunes Timed Text"), QStringLiteral("itt")), {D::TOPLEFT, D::BOTTOM, D::BOTTOM, D::CENTER, D::TOPLEFT}, "style-based regions reload");

    auto pos = load(QStringLiteral("positioning_test.itt"), &fmt);
    CHECK(pos->size() >= 5, "positioning itt entries");
    CHECK_EQ(directionName(effDir(*pos->get(0))), QStringLiteral("TOP"), "positioning itt top");
    CHECK_EQ(directionName(effDir(*pos->get(1))), QStringLiteral("CENTER"), "positioning itt center");
    CHECK_EQ(directionName(effDir(*pos->get(2))), QStringLiteral("BOTTOM"), "positioning itt bottom");
    CHECK_EQ(directionName(effDir(*pos->get(3))), QStringLiteral("BOTTOM"), "positioning itt default");
    CHECK_EQ(pos->get(0)->getText(), QStringLiteral("Top positioned subtitle"), "positioning itt text 0");
    CHECK_EQ(pos->get(1)->getText(), QStringLiteral("Center positioned subtitle"), "positioning itt text 1");
    CHECK_EQ(pos->get(2)->getText(), QStringLiteral("Bottom positioned subtitle"), "positioning itt text 2");
    CHECK_EQ(pos->get(3)->getText(), QStringLiteral("Default position subtitle"), "positioning itt text 3");
    int topCount = 0;
    for (const SubEntryPtr &e : pos->entries()) if (effDir(*e) == D::TOP) ++topCount;
    CHECK_EQ(topCount, 2, "positioning itt exactly 2 TOP");
    auto posv = load(QStringLiteral("positioning_test.vtt"), &fmt);
    CHECK(posv->size() >= 6, "positioning vtt cues");
    CHECK_EQ(directionName(effDir(*posv->get(0))), QStringLiteral("TOP"), "vtt line:20% top");
    CHECK_EQ(directionName(effDir(*posv->get(4))), QStringLiteral("CENTER"), "vtt line:50% center");
    CHECK_EQ(directionName(effDir(*posv->get(5))), QStringLiteral("BOTTOM"), "vtt line:90% bottom");
    CHECK_EQ(directionName(effDir(*posv->get(1))), QStringLiteral("BOTTOM"), "vtt no setting bottom");
    CHECK_EQ(posv->get(0)->getText(), QStringLiteral("Top positioned subtitle"), "vtt text 0");
    CHECK_EQ(posv->get(4)->getText(), QStringLiteral("Center positioned subtitle"), "vtt text 4");
    CHECK_EQ(posv->get(5)->getText(), QStringLiteral("Near bottom subtitle"), "vtt text 5");
    CHECK_EQ(posv->get(1)->getText(), QStringLiteral("Bottom positioned subtitle"), "vtt text 1");
    topCount = 0;
    for (const SubEntryPtr &e : posv->entries()) if (effDir(*e) == D::TOP) ++topCount;
    CHECK_EQ(topCount, 2, "positioning vtt exactly 2 TOP");

    auto nested = load(QStringLiteral("nested_styles.itt"), &fmt);
    CHECK_EQ(nested->size(), 6, "nested_styles entries");
    CHECK_EQ(nested->get(0)->getText(), QStringLiteral("Italic with bold nested inside"), "nested text");
    CHECK_EQ(nested->get(0)->getStyleover(StyleType::BOLD)->dump(), QStringLiteral("{(true,12)(false,23)}"), "nested bold run");
    CHECK_EQ(nested->get(0)->getStyleover(StyleType::ITALIC)->dump(), QStringLiteral("{(true,0)}"), "nested italic run");
}

static void testFontScaling() {
    auto make = [](int size, int playResY) {
        QString s = QStringLiteral("[Script Info]\nScriptType: v4.00+\n");
        if (playResY > 0) s += QStringLiteral("PlayResX: 1920\n");
        if (playResY > 0) s += QStringLiteral("PlayResY: %1\n").arg(playResY);
        s += QStringLiteral("\n[V4+ Styles]\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        s += QStringLiteral("Style: Default,Arial,%1,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2,0,2,30,30,30,1\n").arg(size);
        s += QStringLiteral("\n[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\nDialogue: 0,0:00:00.00,0:00:03.00,Default,,0,0,0,,Hello\n");
        return s;
    };
    auto loadText = [](const QString &text, const QString &ext) {
        SubFile sf(QStringLiteral("/tmp/scale.") + ext, SubFile::EXTENSION_GIVEN);
        auto subs = std::make_unique<Subtitles>(sf);
        subs->populate(sf, text, false);
        return subs;
    };
    CHECK_EQ(loadText(make(68, 1080), QStringLiteral("ass"))->getStyleList().get(0)->fontSize(), 24, "68@1080 → 24");
    CHECK_EQ(loadText(make(45, 720), QStringLiteral("ass"))->getStyleList().get(0)->fontSize(), 24, "45@720 → 24");
    CHECK_EQ(loadText(make(135, 2160), QStringLiteral("ass"))->getStyleList().get(0)->fontSize(), 24, "135@2160 → 24");
    CHECK_EQ(loadText(make(33, 0), QStringLiteral("ass"))->getStyleList().get(0)->fontSize(), 44, "33 without PlayRes → 44");
    // Only PlayResX: PlayResY follows at 4:3 (1920 → 1440), as renderers do.
    QString onlyX = make(33, 0);
    onlyX.replace(QStringLiteral("ScriptType: v4.00+\n"), QStringLiteral("ScriptType: v4.00+\nPlayResX: 1920\n"));
    auto onlyXSubs = loadText(onlyX, QStringLiteral("ass"));
    CHECK_EQ(onlyXSubs->getStyleList().get(0)->fontSize(), 9, "33 with PlayResX 1920 only → 33 at 1440");
    CHECK(render(*onlyXSubs, QStringLiteral("AdvancedSubStation")).contains(QStringLiteral("PlayResX: 1920\nPlayResY: 1440\n")), "PlayResY derived from PlayResX");
    QString only1280 = onlyX;
    only1280.replace(QStringLiteral("PlayResX: 1920"), QStringLiteral("PlayResX: 1280"));
    CHECK(render(*loadText(only1280, QStringLiteral("ass")), QStringLiteral("AdvancedSubStation")).contains(QStringLiteral("PlayResX: 1280\nPlayResY: 1024\n")), "PlayResX 1280 → PlayResY 1024");
    auto core = loadText(make(24, 384), QStringLiteral("ass"));
    class Media : public MediaFile {} media;
    auto vf = std::make_shared<VideoFile>(QStringLiteral("/tmp/none.mp4"));
    vf->setInformation(1920, 1080, 60, 25);
    media.setVideoFile(vf);
    SubFormatPtr f = Availabilities::formats().findFromName(QStringLiteral("AdvancedSubStation"))->newInstance();
    f->updateFormat(core->getSubFile());
    // A loaded SSA/ASS keeps its own canvas, whatever the video.
    const QString keptCanvas = static_cast<AbstractGenericTextSubFormat *>(f.get())->render(*core, &media);
    CHECK(keptCanvas.contains(QStringLiteral("PlayResX: 1920\nPlayResY: 384\n")) && keptCanvas.contains(QStringLiteral("Style: Default,Arial,24,")), "loaded canvas kept with a video");
    // A document without one (e.g. from SRT) takes the video's.
    Subtitles noCanvas(*core);
    noCanvas.setFormatData({});
    const QString withVideo = static_cast<AbstractGenericTextSubFormat *>(f.get())->render(noCanvas, &media);
    CHECK(withVideo.contains(QStringLiteral("PlayResY: 1080")) && withVideo.contains(QStringLiteral("Style: Default,Arial,68,")), "save with video scales up");
    CHECK_EQ(loadText(withVideo, QStringLiteral("ass"))->getStyleList().get(0)->fontSize(), 24, "roundtrip after save with video stable");
    // The SSA variant.
    QString ssaText = make(68, 1080);
    ssaText.replace(QStringLiteral("ScriptType: v4.00+"), QStringLiteral("ScriptType: v4.00")).replace(QStringLiteral("[V4+ Styles]"), QStringLiteral("[V4 Styles]"));
    ssaText.replace(QStringLiteral("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding"),
                    QStringLiteral("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding"));
    ssaText.replace(QStringLiteral("Style: Default,Arial,68,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2,0,2,30,30,30,1"), QStringLiteral("Style: Default,Arial,68,16777215,255,0,0,0,0,1,2,0,2,30,30,30,0,1"));
    ssaText.replace(QStringLiteral("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\nDialogue: 0,"), QStringLiteral("Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\nDialogue: Marked=0,"));
    auto ssa = loadText(ssaText, QStringLiteral("ssa"));
    CHECK_EQ(ssa->getSubFile().getFormat() ? ssa->getSubFile().getFormat()->getName() : QString(), QStringLiteral("SubStationAlpha"), "SSA detected");
    CHECK_EQ(ssa->getStyleList().get(0)->fontSize(), 24, "SSA 68@1080 → 24");
    SubFormatPtr fs = Availabilities::formats().findFromName(QStringLiteral("SubStationAlpha"))->newInstance();
    fs->updateFormat(ssa->getSubFile());
    const QString ssaSaved = static_cast<AbstractGenericTextSubFormat *>(fs.get())->render(*ssa, &media);
    CHECK(ssaSaved.contains(QStringLiteral("PlayResY: 1080")) && ssaSaved.contains(QStringLiteral("Style: Default,Arial,68,")), "SSA save with video scales up");
    CHECK_EQ(loadText(ssaSaved, QStringLiteral("ssa"))->getStyleList().get(0)->fontSize(), 24, "SSA roundtrip stable");
    const QString noVideo = static_cast<AbstractGenericTextSubFormat *>(f.get())->render(*core, nullptr);
    CHECK(noVideo.contains(QStringLiteral("PlayResY: 384")) && noVideo.contains(QStringLiteral("Style: Default,Arial,24,")), "save without video is verbatim");
    for (int h : {480, 720, 1080, 2160})
        for (int c = 8; c <= 80; ++c)
            CHECK_EQ(qRound(qRound(c * h / 384.0f) * 384.0f / h), c, "core size survives a video round trip");
}


static std::unique_ptr<Subtitles> loadString(const QString &text, const QString &ext) {
    SubFile sf(QStringLiteral("/tmp/parity.") + ext, SubFile::EXTENSION_GIVEN);
    auto subs = std::make_unique<Subtitles>(sf);
    subs->populate(sf, text, false);
    return subs;
}

// SSA/ASS: tags, header data and field layout survive load → save.
static void testSubStationParity() {
    const QString src = QStringLiteral(
        "[Script Info]\n; a comment\nTitle: T\nScriptType: v4.00+\nWrapStyle: 0\nScaledBorderAndShadow: yes\nYCbCr Matrix: TV.709\nCustom Key: keepme\nPlayResX: 1920\nPlayResY: 1080\n\n"
        "[Aegisub Project Garbage]\nAudio File: foo.wav\n\n"
        "[V4+ Styles]\nFormat: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n"
        "Style: Default,Arial,72,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,3,1.5,2,30,30,40,1\n"
        "Style: Deci,Arial,22.5,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100.5,99.5,0,0,1,3,1.5,2,30,30,40,0\n\n"
        "[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n"
        "Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,{comment in braces}third\n"
        "Comment: 0,0:00:02.00,0:00:04.00,Default,,0,0,0,,This is a comment line\n"
        "Dialogue: 0,0:00:03.00,0:00:04.00,Default,,0,0,0,,{\\i1}a{comment}b{\\clip(0,0,100,100)}c\n"
        "Dialogue: 0,0:00:05.00,0:00:06.00,Default,,0,0,0,,{\\fs40.5}d {\\b100}thin {\\b700}heavy {\\b1}bold\n"
        "Dialogue: 0,0:00:07.00,0:00:08.00,Default,,0,0,0,,hard\\hspace soft\\nbreak\\Nline \\{lit\\}\n"
        "Dialogue: 0,0:00:09.00,0:00:10.00,Default,,0,0,0,,{\\fs48}big{\\fs} back\n"
        "\n[Fonts]\nfontname: x.ttf\nM3<a@\n");
    const QString ass = QStringLiteral("AdvancedSubStation");
    auto subs = loadString(src, QStringLiteral("ass"));
    CHECK_EQ(subs->size(), 5, "Comment: is not an entry");
    const QString out = render(*subs, ass);
    // F13: brace comments are kept verbatim.
    CHECK_EQ(subs->get(0)->getText(), QStringLiteral("third"), "brace comment not in the text");
    CHECK(out.contains(QStringLiteral(",,{comment in braces}third\n")), "brace comment written back");
    // F14: \clip is not a colour.
    CHECK_EQ(subs->get(1)->getText(), QStringLiteral("abc"), "comment and clip stripped");
    CHECK(subs->get(1)->getStyleover(StyleType::PRIMARY) == nullptr, "\\clip is no colour");
    CHECK(out.contains(QStringLiteral(",,{\\i1}a{comment}b{\\clip(0,0,100,100)}c\n")), "inner comment and clip written back");
    // F15: decimal \fs, weights are not \b1.
    auto fs = subs->get(2)->overValue(StyleType::FONTSIZE, 0, 1);
    CHECK(fs && std::get<int>(*fs) == 14, "\\fs40.5 at 1080 → core 14");
    CHECK(out.contains(QStringLiteral("{\\fs40.5}d")), "\\fs40.5 written back as the file had it");
    auto thin = subs->get(2)->overValue(StyleType::BOLD, 3, 4);
    CHECK(thin && !std::get<bool>(*thin), "\\b100 is not bold");
    auto bold = subs->get(2)->overValue(StyleType::BOLD, subs->get(2)->getText().length() - 1, subs->get(2)->getText().length());
    CHECK(bold && std::get<bool>(*bold), "\\b1 is bold");
    const int h = subs->get(2)->getText().indexOf(QStringLiteral("heavy"));
    auto heavy = subs->get(2)->overValue(StyleType::BOLD, h, h + 1);
    CHECK(heavy && std::get<bool>(*heavy), "\\b700 is bold (as libass draws it)");
    const SubStylePtr deci = subs->getStyleList().get(subs->getStyleList().indexOfName(QStringLiteral("Deci")));
    CHECK_EQ(deci->fontSize(), 8, "Fontsize 22.5 at 1080 → core 8");
    CHECK_EQ(deci->integral(StyleType::XSCALE), 101, "ScaleX 100.5 rounded");
    CHECK(out.contains(QStringLiteral("Style: Deci,Arial,22.5,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100.5,99.5,")), "decimal style values written back");
    // F20: \h is a hard space, \n stays (WrapStyle 0), literal braces.
    CHECK_EQ(subs->get(3)->getText(), QStringLiteral("hard space soft\\nbreak\nline {lit}"), "\\h, \\n, \\N and \\{ \\} read");
    CHECK(out.contains(QStringLiteral(",,hard\\hspace soft\\nbreak\\Nline \\{lit\\}\n")), "\\h, \\n, \\N and braces written back");
    CHECK(out.contains(QStringLiteral(",,{\\fs48}big{\\fs72} back\n")), "a return to the style size is written exactly");
    // F16/F17: canvas, header keys, sections, comments and Encoding kept.
    CHECK(out.contains(QStringLiteral("PlayResX: 1920\nPlayResY: 1080\n")), "canvas kept");
    CHECK(out.contains(QStringLiteral("WrapStyle: 0\nScaledBorderAndShadow: yes\nYCbCr Matrix: TV.709\nCustom Key: keepme\n")), "Script Info keys kept");
    CHECK(out.contains(QStringLiteral("\n[Aegisub Project Garbage]\nAudio File: foo.wav\n\n[V4+ Styles]\n")), "unknown section before the styles kept");
    CHECK(out.contains(QStringLiteral("Style: Default,Arial,72,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,3,1.5,2,30,30,40,1\n")), "font size and Encoding kept");
    CHECK(out.contains(QStringLiteral("\nComment: 0,0:00:02.00,0:00:04.00,Default,,0,0,0,,This is a comment line\n")), "Comment: line kept");
    CHECK(out.endsWith(QStringLiteral("\n\n[Fonts]\nfontname: x.ttf\nM3<a@\n")), "[Fonts] kept at the end");
    CHECK_EQ(render(*loadString(out, QStringLiteral("ass")), ass), out, "ASS save is stable");
    // A changed size is computed again.
    subs->getStyleList().get(0)->set(StyleType::FONTSIZE, StyleValue(30));
    CHECK(render(*subs, ass).contains(QStringLiteral("Style: Default,Arial,84,")), "changed font size scaled");
    // F19: commas in Name/Effect, braces in text.
    subs->get(0)->setName(QStringLiteral("A, B"));
    subs->get(0)->setEffect(QStringLiteral("x,y"));
    subs->get(0)->setText(QStringLiteral("a {b} c"));
    const QString edited = render(*subs, ass);
    CHECK(edited.contains(QStringLiteral(",Default,A; B,0,0,0,x;y,{comment in braces}a \\{b\\} c\n")), "commas and braces escaped");
    auto editedR = loadString(edited, QStringLiteral("ass"));
    CHECK_EQ(editedR->get(0)->getText(), QStringLiteral("a {b} c"), "escaped braces read back");
    CHECK_EQ(editedR->get(0)->getEffect(), QStringLiteral("x;y"), "effect stays one field");
    // WrapStyle 2: \n is a hard break.
    QString wrap2 = src;
    wrap2.replace(QStringLiteral("WrapStyle: 0"), QStringLiteral("WrapStyle: 2"));
    CHECK_EQ(loadString(wrap2, QStringLiteral("ass"))->get(3)->getText(), QStringLiteral("hard space soft\nbreak\nline {lit}"), "\\n with WrapStyle 2");

    // F18: the Format: lines give the column order.
    const QString reordered = QStringLiteral(
        "[Script Info]\nScriptType: v4.00+\nPlayResX: 683\nPlayResY: 384\n\n"
        "[V4+ Styles]\nFormat: Name, Fontsize, Fontname, Alignment, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, MarginL, MarginR, MarginV, Encoding\n"
        "Style: Top,30,Verdana,8,&H000000FF,&H000000FF,&H00000000,&H00000000,-1,0,0,0,100,100,0,0,1,2,2,10,20,30,0\n\n"
        "[Events]\nFormat: Start, End, Style, Layer, Name, Effect, MarginL, MarginR, MarginV, Text\n"
        "Dialogue: 0:00:01.00,0:00:02.00,Top,3,Bob,Fx,1,2,3,Hello, world\n"
        "Comment: 0:00:03.00,0:00:04.00,Top,0,,,0,0,0,note\n");
    auto ro = loadString(reordered, QStringLiteral("ass"));
    CHECK_EQ(ro->size(), 1, "reordered events read");
    if (ro->size() == 1) {
        const SubEntry &e = *ro->get(0);
        CHECK_EQ(e.getText(), QStringLiteral("Hello, world"), "reordered text");
        CHECK_EQ(e.getLayer(), QStringLiteral("3"), "reordered layer");
        CHECK_EQ(e.getName(), QStringLiteral("Bob"), "reordered name");
        CHECK_EQ(e.getEffect(), QStringLiteral("Fx"), "reordered effect");
        CHECK_EQ(e.getMarginL() + e.getMarginR() + e.getMarginV(), QStringLiteral("123"), "reordered margins");
        CHECK_EQ(e.getStyle()->getName(), QStringLiteral("Top"), "reordered style reference");
        CHECK_EQ(e.getStyle()->fontName(), QStringLiteral("Verdana"), "reordered font name");
        CHECK_EQ(e.getStyle()->fontSize(), 30, "reordered font size");
        CHECK_EQ(directionName(e.getStyle()->direction()), QStringLiteral("TOP"), "reordered alignment");
        CHECK(e.getStyle()->flag(StyleType::BOLD), "reordered bold");
        CHECK_EQ(e.getStyle()->integral(StyleType::VERTICAL), 30, "reordered vertical margin");
    }
    const QString roOut = render(*ro, ass);
    CHECK(roOut.contains(QStringLiteral("Dialogue: 3,0:00:01.00,0:00:02.00,Top,Bob,1,2,3,Fx,Hello, world\n")), "written in the standard order");
    CHECK(roOut.contains(QStringLiteral("Comment: 0,0:00:03.00,0:00:04.00,Top,,0,0,0,,note\n")), "kept Comment: in the standard order");

    // F4: SSA reads {\an#} too.
    const QString ssa = QStringLiteral(
        "[Script Info]\nScriptType: v4.00\n\n[V4 Styles]\n"
        "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding\n"
        "Style: Default,Tahoma,24,16777215,65535,0,8421504,0,0,1,2,3,2,10,10,10,0,0\n\n"
        "[Events]\nFormat: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n"
        "Dialogue: Marked=0,0:00:01.00,0:00:02.00,Default,,0000,0000,0000,,{\\an8}top\n");
    auto ssaSubs = loadString(ssa, QStringLiteral("ssa"));
    CHECK_EQ(ssaSubs->size(), 1, "SSA with \\an loaded");
    if (ssaSubs->size() == 1)
        CHECK_EQ(directionName(effDir(*ssaSubs->get(0))), QStringLiteral("TOP"), "SSA {\\an8} → TOP");
    CHECK(render(*ssaSubs, QStringLiteral("SubStationAlpha")).contains(QStringLiteral(",,{\\a6}top\n")), "SSA writes {\\a6}");
    CHECK(render(*ssaSubs, QStringLiteral("SubStationAlpha")).contains(QStringLiteral("PlayResX: 384\nPlayResY: 288\n")), "SSA default canvas written");
}

static void testEncodingDetection() {
    SubFile sf(QStringLiteral("/tmp/enc.srt"), SubFile::EXTENSION_GIVEN);
    // UTF-8 with BOM
    QByteArray bom = QByteArray("\xEF\xBB\xBF") + QString::fromUtf8("1\n00:00:01,000 --> 00:00:02,000\nΓειά\n").toUtf8();
    QString t = FileCommunicator::detectAndDecode(sf, bom, false);
    CHECK_EQ(sf.getEncoding(), QStringLiteral("UTF-8"), "BOM → UTF-8");
    CHECK(t.startsWith(QStringLiteral("1\n")), "BOM stripped");
    // plain UTF-8
    t = FileCommunicator::detectAndDecode(sf, QString::fromUtf8("Γειά σου\r\nκόσμε").toUtf8(), false);
    CHECK_EQ(sf.getEncoding(), QStringLiteral("UTF-8"), "valid UTF-8 detected");
    CHECK_EQ(t, QStringLiteral("Γειά σου\nκόσμε\n\n"), "CRLF normalised, two trailing newlines");
    // Greek in ISO-8859-7 with the 8-bit default set
    Prefs::set(QStringLiteral("default.encoding.8bit"), QStringLiteral("ISO-8859-7"));
    Options::load();
    QStringEncoder enc("ISO-8859-7");
    const QByteArray greek = enc.encode(QString::fromUtf8("Γειά"));
    t = FileCommunicator::detectAndDecode(sf, greek, false);
    CHECK_EQ(sf.getEncoding(), QStringLiteral("ISO-8859-7"), "8-bit floor used");
    CHECK_EQ(t, QStringLiteral("Γειά\n\n"), "8-bit decoded");
    // UTF-16 LE with BOM
    QStringEncoder u16(QStringConverter::Utf16LE);
    QByteArray le = QByteArray("\xFF\xFE") + u16.encode(QStringLiteral("hi"));
    t = FileCommunicator::detectAndDecode(sf, le, false);
    CHECK_EQ(sf.getEncoding(), QStringLiteral("UTF-16"), "UTF-16 BOM");
    CHECK_EQ(t, QStringLiteral("hi\n\n"), "UTF-16 decoded");
    CHECK(FileCommunicator::detectAndDecode(sf, QByteArray(), false).isNull(), "unreadable → null");
    Prefs::set(QStringLiteral("default.encoding.8bit"), QStringLiteral("ISO-8859-1"));
    Options::load();
}

// Text that never goes through the decoder (a subtitle stream taken out of a
// video, a tool, a download) is prepared all the same: a CR left inside a cue
// is invisible in the editor but libass draws it in the preview.
static void testTextFromAnySource() {
    SubFile sf(QStringLiteral("/tmp/embedded.srt"), SubFile::EXTENSION_GIVEN);
    Subtitles subs(sf);
    subs.populate(sf, QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\nfirst line\r\nsecond line\n\n"), false);
    CHECK_EQ(subs.size(), 1, "the CRLF cue loads");
    CHECK_EQ(subs.elementAt(0)->getText(), QStringLiteral("first line\nsecond line"), "no CR reaches the subtitle");
    // No final newline at all: the last cue still closes.
    SubFile sf2(QStringLiteral("/tmp/embedded2.srt"), SubFile::EXTENSION_GIVEN);
    Subtitles bare(sf2);
    bare.populate(sf2, QStringLiteral("1\r\n00:00:01,000 --> 00:00:02,000\r\nonly line"), false);
    CHECK_EQ(bare.size(), 1, "a text without a trailing newline loads");
    CHECK_EQ(bare.elementAt(0)->getText(), QStringLiteral("only line"), "no CR in a single-line cue");
}

static void testSaveLoadFile() {
    SubFile sf(QStringLiteral("/tmp/jubler-qt-test-save.srt"), SubFile::EXTENSION_GIVEN);
    Subtitles subs(sf);
    subs.add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("Γειά σου")));
    sf.setEncoding(QStringLiteral("UTF-8"));
    CHECK(FileCommunicator::save(subs, sf, nullptr).isNull(), "save ok");
    SubFile back(QStringLiteral("/tmp/jubler-qt-test-save.srt"), SubFile::EXTENSION_GIVEN);
    const QString data = FileCommunicator::load(back);
    CHECK(data.contains(QStringLiteral("Γειά σου")), "reload has text");
    CHECK(FileCommunicator::loadRawBytes(back.getSaveFile()).contains("\r\n"), "text formats use CRLF");
    sf.setEncoding(QStringLiteral("ISO-8859-1"));
    const QString err = FileCommunicator::save(subs, sf, nullptr);
    CHECK(err.contains(QStringLiteral("not mappable")), "unmappable characters reported");
    CHECK(FileCommunicator::loadRawBytes(back.getSaveFile()).contains("\r\n"), "failed save leaves the original intact");
    QFile::remove(QStringLiteral("/tmp/jubler-qt-test-save.srt"));
}

// ---- the plain text formats and the SRT/VTT parity fixes ------------------

static QString readFixture(const QString &name) {
    QFile f(g_fixtures + QLatin1Char('/') + name);
    CHECK(f.open(QIODevice::ReadOnly), qPrintable("fixture readable: " + name));
    return QString::fromUtf8(f.readAll());
}

// Parse with a given format, as the application does after decoding (line
// ends normalised, a final blank line).
static std::unique_ptr<Subtitles> parseWith(const QString &fmtName, QString data, float fps = 25) {
    data.replace(QLatin1String("\r\n"), QLatin1String("\n"));
    if (!data.endsWith(QLatin1Char('\n'))) data += QLatin1Char('\n');
    data += QLatin1Char('\n');
    SubFormatPtr f = Availabilities::formats().findFromName(fmtName)->newInstance();
    SubFile sf(QStringLiteral("UTF-8"), fps, f, QStringLiteral("/tmp/x.") + f->getExtension(), SubFile::EXTENSION_GIVEN);
    f->updateFormat(sf);
    auto subs = f->parse(data, fps, sf.getSaveFile(), false);
    if (subs)
        subs->setSubFile(sf);
    return subs;
}

static QString dumpTimes(const Subtitles &s) {
    QStringList out;
    for (int i = 0; i < s.size(); ++i) {
        const SubEntry &e = *s.elementAt(i);
        out << QStringLiteral("%1 %2 [%3]").arg(e.getStartTime().getMillis()).arg(e.getFinishTime().getMillis()).arg(e.getText());
    }
    return out.join(QLatin1Char('\n'));
}

static void testTextFormats() {
    // Load + save of every plain text format, checked against what the Java
    // build writes for the same file (tests/fixtures/text/expected; where the
    // port fixes a Java bug the expected file says so below).
    struct C { const char *file; const char *fmt; const char *times; };
    const C cases[] = {
        {"mpl2.txt", "MPL2", "0 1200 [First at zero]\n1500 4000 [Two lines\nsecond line]\n5000 5000 [zero duration]\n6500 8000 [Ελληνικά 日本 😀]\n36000500 36002000 [ten hours [bracket] {brace}]"},
        {"microdvd.sub", "MicroDVD", "0 1240 [First at zero]\n1480 4000 [Two lines\nsecond line]\n5000 5000 [zero duration]\n6520 8040 [Ελληνικά 日本 😀]\n36000040 36002000 [ten hours {y:i}style code]"},
        {"subviewer.sub", "SubViewer", "0 1230 [First at zero]\n1500 3990 [Two lines\nsecond line]\n36000000 36001990 [ten hours Ελληνικά]"},
        {"subviewer2.sub", "SubViewer2", "0 1230 [First at zero]\n1500 3990 [Two lines\nsecond line]\n36000000 36001990 [ten hours Ελληνικά]"},
        // "00:00:05:25" at 25 fps is 6 s.
        {"spruce.stl", "Spruce", "0 1240 [First at zero]\n1480 3960 [Two lines\nsecond line]\n6000 6400 [frame equal to the fps]\n36000040 36001960 [ten hours Ελληνικά]"},
        // Frames read as frames (the Java read them as milliseconds, decision
        // 01 #24): the file is written back unchanged.
        {"textscript.txt", "TextScript", "0 1240 [First at zero]\n1480 3960 [Two lines\nsecond line]\n6000 6400 [frame equal to the fps]\n36000040 36001960 [ten hours Ελληνικά]"},
        // A '[' inside the text is text, and the cue at 0 keeps its timestamp
        // (the Java cut the text at the bracket and wrote no first stamp).
        {"quicktime.txt", "Quicktime", "0 1234 [First at zero [with a bracket]]\n1500 3999 [Two lines second line]\n5000 6000 [Ελληνικά 日本 😀]"},
        {"presegmented.txt", "PreSegmentedText", "0 2000 [First block\nsecond line]\n3000 5000 [Second block]\n6000 8000 [\nThird after two blanks\nlast line]"},
        // The artificial trailing empty lines are no entries (decision 01 #30).
        {"plaintext.txt", "PlainText", "0 2000 [First line]\n3000 5000 [Second line]\n6000 8000 []\n9000 11000 [Fourth line after a blank]"},
    };
    for (const C &c : cases) {
        const QString file = QStringLiteral("text/") + QLatin1String(c.file);
        auto subs = parseWith(QLatin1String(c.fmt), readFixture(file));
        CHECK(subs != nullptr, qPrintable(file + " parses"));
        if (!subs)
            continue;
        CHECK_EQ(dumpTimes(*subs), QString::fromUtf8(c.times), qPrintable(file + " parsed as the Java does"));
        CHECK_EQ(render(*subs, QLatin1String(c.fmt)), readFixture(QStringLiteral("text/expected/") + QLatin1String(c.file)),
                 qPrintable(file + " written as the Java does"));
        // The written file reads back the same.
        auto again = parseWith(QLatin1String(c.fmt), render(*subs, QLatin1String(c.fmt)));
        CHECK(again && dumpTimes(*again) == dumpTimes(*subs), qPrintable(file + " round trip"));
    }
    {
        auto sv = parseWith(QStringLiteral("SubViewer"), readFixture(QStringLiteral("text/subviewer.sub")));
        CHECK_EQ(sv->getAttribs().title, QStringLiteral("A title"), "SubViewer title");
        CHECK_EQ(sv->getAttribs().comments, QStringLiteral("line one\nline two"), "SubViewer comments");
    }
    // Detection of the files with a distinctive shape.
    for (const auto &[file, fmt] : {std::pair{"text/mpl2.txt", "MPL2"}, {"text/microdvd.sub", "MicroDVD"}, {"text/spruce.stl", "Spruce"},
                                    {"text/textscript.txt", "TextScript"}, {"text/quicktime.txt", "Quicktime"}}) {
        QString detected;
        load(QLatin1String(file), &detected);
        CHECK_EQ(detected, QLatin1String(fmt), qPrintable(QStringLiteral("%1 detected").arg(QLatin1String(file))));
    }

    // F30: a frame that rounds up to the frame rate carries into the seconds.
    CHECK_EQ(Time(3.990).getSecondsFrames(25), QStringLiteral("00:00:04:00"), "frame 25 at 25 fps written as the next second");
    CHECK_EQ(Time(59.999).getSecondsFrames(23.976f), QStringLiteral("00:01:00:00"), "carry into the minutes at 23.976 fps");
    CHECK_EQ(Time(3.960).getSecondsFrames(25), QStringLiteral("00:00:03:24"), "last frame of a second kept");
    {
        SubFile sf(QStringLiteral("/tmp/x.stl"), SubFile::EXTENSION_GIVEN);
        Subtitles s(sf);
        s.add(std::make_shared<SubEntry>(Time(3.990), Time(4.5), QStringLiteral("x")));
        CHECK_EQ(render(s, QStringLiteral("Spruce")), QStringLiteral("00:00:04:00 , 00:00:04:13 , x\n"), "Spruce never writes frame 25");
        CHECK_EQ(render(s, QStringLiteral("TextScript")), QStringLiteral("1 00;00;04;00 00;00;04;13 x\n"), "TextScript never writes frame 25");
    }

    // F31: {timeScale:N} counts the fraction in 1/N s; a no-break space is
    // not trimmed away (Java trim).
    {
        auto qt = parseWith(QStringLiteral("Quicktime"),
                            QStringLiteral("{QTtext}{font:Tahoma}\n{timeScale:30}\n{timeStamps:absolute}\n[00:00:01.15]\n") + QChar(0x00A0)
                                + QStringLiteral("half [a] second\n[00:00:02.00]\n"));
        CHECK(qt != nullptr, "Quicktime with a time scale parses");
        if (qt)
            CHECK_EQ(dumpTimes(*qt), QStringLiteral("1500 2000 [") + QChar(0x00A0) + QStringLiteral("half [a] second]"), "timeScale:30 honoured");
    }

    // F23: a separator line of spaces is a blank line.
    {
        auto srt = parseWith(QStringLiteral("SubRip"), QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\nfirst\n  \n2\n00:00:03,000 --> 00:00:04,000\nsecond\n"));
        CHECK_EQ(dumpTimes(*srt), QStringLiteral("1000 2000 [first]\n3000 4000 [second]"), "SRT whitespace separator");
        auto vtt = parseWith(QStringLiteral("WebVTT"), QStringLiteral("WEBVTT\n\n00:01.000 --> 00:02.000\nfirst\n \n00:03.000 --> 00:04.000\nsecond\n"));
        CHECK_EQ(dumpTimes(*vtt), QStringLiteral("1000 2000 [first]\n3000 4000 [second]"), "VTT whitespace separator");
        auto sv = parseWith(QStringLiteral("YouTube Subtitles"), QStringLiteral("0:00:01.000,0:00:02.000\nfirst\n\t\n0:00:03.000,0:00:04.000\nsecond\n"));
        CHECK_EQ(dumpTimes(*sv), QStringLiteral("1000 2000 [first]\n3000 4000 [second]"), "SBV whitespace separator");
    }

    // F24: tag names exact and case-insensitive.
    {
        auto srt = parseWith(QStringLiteral("SubRip"), QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\n<span>no</span> <B>bold</B> <br>x <I>it</I>\n"));
        const SubEntry &e = *srt->get(0);
        CHECK_EQ(e.getText(), QStringLiteral("no bold \nx it"), "unknown tags dropped, <br> a line break");
        CHECK(e.getStyleover(StyleType::STRIKETHROUGH) == nullptr, "<span> is not strike");
        CHECK_EQ(e.getStyleover(StyleType::BOLD)->dump(), QStringLiteral("{(true,3)(false,7)}"), "<B> is bold, <br> is not");
        CHECK_EQ(e.getStyleover(StyleType::ITALIC)->dump(), QStringLiteral("{(true,11)}"), "<I> is italic");
    }
    // F22: relative font sizes on the HTML scale, from the style size (24).
    {
        auto srt = parseWith(QStringLiteral("SubRip"), QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\n<font size=+2>big</font> <font size=\"-1\">small</font> <font size=30>abs</font>\n"));
        CHECK_EQ(srt->get(0)->getStyleover(StyleType::FONTSIZE)->dump(), QStringLiteral("{(36,0)(24,3)(20,4)(24,9)(30,10)}"), "relative font sizes");
    }
    // F21: closing tags with nothing open are not written.
    {
        const QString ass = QStringLiteral(
            "[Script Info]\nScriptType: v4.00+\nPlayResY: 384\n\n[V4+ Styles]\n"
            "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n"
            "Style: Default,Arial,24,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,20,20,20,0\n"
            "Style: Ital,Arial,24,&H00FFFFFF,&H00FFFFFF,&H00000000,&H00000000,-1,-1,0,0,100,100,0,0,1,2,0,2,20,20,20,0\n\n"
            "[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n"
            "Dialogue: 0,0:00:01.00,0:00:02.00,Ital,,0,0,0,,{\\i0}plain{\\b0} text\n");
        auto s = parseWith(QStringLiteral("AdvancedSubStation"), ass);
        const QString out = render(*s, QStringLiteral("SubRip"));
        CHECK_EQ(out, QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\n<b>plain</b> text\n"), "styled entry without stray closing tags");
    }

    // WebVTT: header lines, long hours, character references, classes on the
    // text without tags (F9, F24).
    {
        const QString src = QStringLiteral(
            "WEBVTT\nKind: captions\nLanguage: en\n\nSTYLE\n::cue(.red) { color: red }\n\nNOTE a note\n\nintro\n"
            "00:00:01.000 --> 00:00:02.000\n<b>Bold</b> then <c.loud.red>red</c> &amp; &lt;tag&gt;&nbsp;&#233;&#x1F600;&lrm;\n\n"
            "123:00:03.000 --> 123:00:04.000\n<v Bob>x</v>\n");
        SubFile sf(QStringLiteral("/tmp/x.vtt"), SubFile::EXTENSION_GIVEN);
        Subtitles s(sf);
        s.populate(sf, src + QLatin1Char('\n'), false);
        CHECK_EQ(s.getSubFile().getFormat() ? s.getSubFile().getFormat()->getName() : QString(), QStringLiteral("WebVTT"), "header lines accepted");
        CHECK_EQ(s.size(), 2, "two cues after the header blocks");
        if (s.size() == 2) {
            const QString text = QStringLiteral("Bold then red & <tag>") + QChar(0x00A0) + QChar(0x00E9) + QString::fromUcs4(U"\U0001F600", 1) + QChar(0x200E);
            CHECK_EQ(s.get(0)->getText(), text, "references decoded");
            CHECK_EQ(s.get(0)->getStyleover(StyleType::PRIMARY)->dump(), QStringLiteral("{(ffff0000,10)(ffffffff,13)}"), "class colour on the text without tags");
            CHECK_EQ(s.get(1)->getStartTime().getMillis(), Time::MAX_MILLI_TIME, "three-digit hours read (clamped to the maximum)");
            const QString out = render(s, QStringLiteral("WebVTT"));
            CHECK(out.contains(QStringLiteral("<b>Bold</b> then <c.red>red</c> &amp; &lt;tag&gt;")), qPrintable("references escaped on write: " + out));
        }
        // No <font> in WebVTT: colours with a class become classes, the rest is dropped.
        auto srt = parseWith(QStringLiteral("SubRip"), QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\n<font color=\"#00ff00\">g</font> <font color=\"#123456\" size=\"30\">o</font> <font face=\"Serif\">f</font>\n"));
        CHECK_EQ(render(*srt, QStringLiteral("WebVTT")), QStringLiteral("WEBVTT\n\n00:00:01.000 --> 00:00:02.000\n<c.lime>g</c> o f\n"), "no <font> written in WebVTT");
    }
    // F10: cue settings carried to SSA/ASS (margins, alignment, rotation) and back to WebVTT.
    {
        auto vtt = parseWith(QStringLiteral("WebVTT"), QStringLiteral(
            "WEBVTT\n\n00:01.000 --> 00:02.000 position:10% align:start size:30%\nleft box\n\n"
            "00:03.000 --> 00:04.000 size:80%\ncentred box\n\n"
            "00:05.000 --> 00:06.000 line:0\ntop line\n\n"
            "00:07.000 --> 00:08.000 line:-2\nsecond from the bottom\n\n"
            "00:09.000 --> 00:10.000 line:15%\ntop percent\n\n"
            "00:11.000 --> 00:12.000 vertical:rl\nvertical\n"));
        CHECK_EQ(vtt->size(), 6, "cue settings cues");
        const QString ass = render(*vtt, QStringLiteral("AdvancedSubStation"));
        CHECK(ass.contains(QStringLiteral("Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0068,0410,0000,,{\\an1}left box")), qPrintable("position/size as margins: " + ass));
        CHECK(ass.contains(QStringLiteral(",Default,,0068,0068,0000,,centred box")), "size as margins");
        CHECK(ass.contains(QStringLiteral(",Default,,0000,0000,0000,,{\\an8}top line")), "line 0 at the top");
        CHECK(ass.contains(QStringLiteral(",Default,,0000,0000,0019,,second from the bottom")), "line -2 one line above the bottom");
        CHECK(ass.contains(QStringLiteral(",Default,,0000,0000,0058,,{\\an8}top percent")), "line 15% as the top margin");
        CHECK(ass.contains(QStringLiteral("{\\frz90}{\\an6}vertical")), "vertical text as a rotation");
        const QString back = render(*vtt, QStringLiteral("WebVTT"));
        CHECK(back.contains(QStringLiteral("00:00:01.000 --> 00:00:02.000 align:start position:10% size:30%\n")), qPrintable("position/size written back: " + back));
        CHECK(back.contains(QStringLiteral("00:00:03.000 --> 00:00:04.000 position:50% size:80%\n")), "size written back");
        CHECK(back.contains(QStringLiteral("00:00:11.000 --> 00:00:12.000 vertical:rl\n")), "vertical written back");
        auto again = parseWith(QStringLiteral("WebVTT"), back);
        CHECK_EQ(render(*again, QStringLiteral("AdvancedSubStation")), ass, "cue settings survive a WebVTT round trip");
    }
}

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    testInitPrefs();
    g_fixtures = argc > 1 ? QString::fromLocal8Bit(argv[1]) : QStringLiteral("tests/fixtures");
    testSubRip();
    testWebVTT();
    testAlignmentFixtures();
    testFontScaling();
    testSubStationParity();
    testEncodingDetection();
    testTextFromAnySource();
    testSaveLoadFile();
    testCorpusAndRegressions();
    testCharsets();
    testW3CFamily();
    testTextFormats();
    testSrtInTheWild();
    return testFinish("test_formats");
}

// Every charset of the Java preset menu is available, with the Java bytes.
static void testCharsets() {
    const char *presets[] = {"UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE", "UTF-32", "UTF-32LE", "UTF-32BE", "ISO-8859-1", "ISO-8859-15",
                             "windows-1252", "IBM850", "macintosh", "ISO-8859-7", "windows-1253", "x-mac-greek", "x-MacIceland", "ISO-8859-3",
                             "ISO-8859-4", "ISO-8859-13", "windows-1257", "ISO-8859-2", "windows-1250", "IBM852", "x-mac-centraleurroman",
                             "x-MacCroatian", "ISO-8859-5", "windows-1251", "IBM855", "IBM866", "x-mac-cyrillic", "x-MacUkraine", "KOI8-R",
                             "KOI8-U", "ISO-8859-16", "x-MacRomania", "ISO-2022-CN", "GB2312", "GBK", "GB18030", "Big5", "Big5-HKSCS",
                             "EUC-TW", "ISO-2022-JP", "EUC-JP", "Shift_JIS", "windows-31j", "ISO-2022-KR", "EUC-KR", "windows-949",
                             "x-Johab", "ISO-8859-11", "windows-874", "TIS-620", "x-MacThai", "ISO-8859-9", "windows-1254", "IBM857",
                             "x-mac-turkish", "windows-1258", "ISO-8859-6", "windows-1256", "IBM864", "x-MacArabic", "ISO-8859-8",
                             "windows-1255", "IBM862", "x-MacHebrew"};
    for (const char *p : presets)
        CHECK(Charsets::isSupported(QLatin1String(p)), qPrintable(QStringLiteral("preset charset available: %1").arg(QLatin1String(p))));
    struct R { const char *cs; const char *text; const char *hex; };
    const R refs[] = {{"x-MacIceland", "Ðþ", "DCDF"}, {"ISO-8859-16", "ȘțĂ", "AAFEC3"}, {"x-Johab", "한글", "D0658B69"},
                      {"x-MacHebrew", "שלום", "F9ECE5ED"}, {"x-MacThai", "ภาษา", "C0D2C9D2"}, {"x-MS950-HKSCS-XP", "香港", "ADBBB4E4"},
                      {"X-UTF-32BE-BOM", "A", "0000FEFF00000041"}};
    for (const R &r : refs) {
        Charsets::EncodeStatus st;
        const QByteArray bytes = Charsets::encode(QString::fromUtf8(r.text), QLatin1String(r.cs), &st);
        CHECK_EQ(QString::fromLatin1(bytes.toHex().toUpper()), QLatin1String(r.hex), r.cs);
        CHECK(st == Charsets::EncodeStatus::Ok, r.cs);
        bool err = true;
        CHECK_EQ(Charsets::decode(bytes, QLatin1String(r.cs), &err), QString::fromUtf8(r.text), r.cs);
        CHECK(!err, r.cs);
    }
    Charsets::EncodeStatus st;
    Charsets::encode(QStringLiteral("Ș"), QStringLiteral("x-MacRomania"), &st);
    CHECK(st == Charsets::EncodeStatus::Unmappable, "unmappable reported");
    bool err = false;
    CHECK_EQ(Charsets::decode(QByteArray("\xD0", 1), QStringLiteral("x-Johab"), &err), QString(QChar::ReplacementCharacter), "incomplete Johab byte");
    CHECK(err, "malformed reported");
    CHECK(Charsets::isSingleByte(QStringLiteral("x-MacIceland")) && !Charsets::isSingleByte(QStringLiteral("x-Johab")), "single byte");
    CHECK(Charsets::availableNames().contains(QStringLiteral("x-Johab")), "built-in listed");
    // Hunspell's spellings of charset names.
    CHECK(Charsets::isSupported(QStringLiteral("ISO8859-16")) && Charsets::isSupported(QStringLiteral("microsoft-cp1251")), "Hunspell charset names");
    // Java's UTF semantics: UTF-32 without BOM, BOM-less input big-endian.
    CHECK_EQ(QString::fromLatin1(Charsets::encode(QStringLiteral("A"), QStringLiteral("UTF-32")).toHex()), QStringLiteral("00000041"), "UTF-32 has no BOM");
    CHECK_EQ(QString::fromLatin1(Charsets::encode(QStringLiteral("A"), QStringLiteral("UTF-16")).toHex()), QStringLiteral("feff0041"), "UTF-16 has a BOM");
    CHECK_EQ(Charsets::decode(QByteArray("\x00\x41", 2), QStringLiteral("UTF-16")), QStringLiteral("A"), "BOM-less UTF-16 is big-endian");
    CHECK_EQ(Charsets::decode(QByteArray("\xFF\xFE\x41\x00", 4), QStringLiteral("UTF-16")), QStringLiteral("A"), "UTF-16 LE BOM honoured");
    err = false;
    CHECK_EQ(Charsets::decode(QByteArray("\x00\x00\x00\x41", 4), QStringLiteral("UTF-32"), &err), QStringLiteral("A"), "BOM-less UTF-32 is big-endian");
    Charsets::decode(QByteArray("\x41\x00\x00\x00", 4), QStringLiteral("UTF-32"), &err);
    CHECK(err, "an impossible UTF-32 value is an error");
}

static void testCorpusAndRegressions() {
    // Every fixture of the corpus loads with the expected format and count.
    struct C { const char *file; const char *fmt; int n; };
    const C cases[] = {{"simple.srt", "SubRip", 17}, {"simple.vtt", "WebVTT", 17}, {"simple.ass", "AdvancedSubStation", 17},
                       {"simple.ssa", "SubStationAlpha", 17}, {"simple.ttml", "TTML", 17}, {"simple.dfxp", "DFXP", 17}, {"simple.itt", "iTunes Timed Text", 17},
                       {"comprehensive.srt", "SubRip", 31}, {"comprehensive.vtt", "WebVTT", 31}, {"comprehensive.ass", "AdvancedSubStation", 31},
                       {"comprehensive.ssa", "SubStationAlpha", 31}, {"comprehensive.ttml", "TTML", 31}, {"comprehensive.dfxp", "DFXP", 31},
                       {"comprehensive.itt", "iTunes Timed Text", 31}};
    for (const C &c : cases) {
        QString fmt;
        auto subs = load(QLatin1String(c.file), &fmt);
        CHECK_EQ(fmt, QLatin1String(c.fmt), c.file);
        CHECK_EQ(subs->size(), c.n, c.file);
        // JUBLER_REGEN_FIXTURES=<dir>: write the corpus through the port's writers
        // there, for review before replacing the checked-in fixtures (decision 01 #5).
        if (const QString regen = qEnvironmentVariable("JUBLER_REGEN_FIXTURES"); !regen.isEmpty()) {
            QDir().mkpath(regen);
            SubFormatPtr f = Availabilities::formats().findFromName(QLatin1String(c.fmt))->newInstance();
            f->updateFormat(subs->getSubFile());
            SubFormat::SaveError err;
            CHECK(f->produce(*subs, regen + QLatin1Char('/') + QLatin1String(c.file), nullptr, err), c.file);   // as a real save
        }
        // Round trip through the format's own writer.
        auto again = reload(*subs, QLatin1String(c.fmt), QFileInfo(QLatin1String(c.file)).suffix());
        CHECK_EQ(again->size(), c.n, qPrintable(QStringLiteral("%1 round trip count").arg(QLatin1String(c.file))));
        for (int i = 0; i < c.n && i < again->size(); ++i) {
            CHECK_EQ(again->get(i)->getText(), subs->get(i)->getText(), qPrintable(QStringLiteral("%1 round trip text %2").arg(QLatin1String(c.file)).arg(i)));
            // The inline formatting survives the round trip as well.
            for (const StyleType::Id id : {StyleType::BOLD, StyleType::ITALIC, StyleType::UNDERLINE, StyleType::STRIKETHROUGH, StyleType::PRIMARY,
                                           StyleType::FONTSIZE, StyleType::FONTNAME}) {
                // WebVTT has no <font>: sizes and faces are not written there.
                if (QLatin1String(c.fmt) == QLatin1String("WebVTT") && (id == StyleType::FONTSIZE || id == StyleType::FONTNAME))
                    continue;
                const Styleover *a = subs->get(i)->getStyleover(id), *b = again->get(i)->getStyleover(id);
                auto dump = [](const Styleover *o) { const QString d = o ? o->dump() : QString(); return d == QLatin1String("{}") ? QString() : d; };
                CHECK_EQ(dump(b), dump(a),
                         qPrintable(QStringLiteral("%1 round trip %2 of entry %3").arg(QLatin1String(c.file), StyleType::name(id)).arg(i)));
            }
        }
    }
    // WebVTT written by the Java build: merged "<ibu>" tags read as i+b+u.
    {
        SubFile sf(QStringLiteral("/tmp/j.vtt"), SubFile::EXTENSION_GIVEN);
        Subtitles s(sf);
        s.populate(sf, QStringLiteral("WEBVTT\n\n00:00:01.000 --> 00:00:02.000\n<ibu>combined</i/b/u> x\n\n"), false);
        CHECK_EQ(s.get(0)->getText(), QStringLiteral("combined x"), "merged tags stripped");
        CHECK(s.get(0)->getStyleover(StyleType::BOLD) && s.get(0)->getStyleover(StyleType::UNDERLINE), "merged tags keep bold and underline");
    }
    // Crossing tags from independent style runs are made to nest.
    CHECK_EQ(SimpleStyledTextSubFormat::repairNesting(QStringLiteral("<b><font color=\"#ff0000\">Bold red.</b></font>")),
             QStringLiteral("<b><font color=\"#ff0000\">Bold red.</font></b>"), "crossing b/font nested");
    CHECK_EQ(SimpleStyledTextSubFormat::repairNesting(QStringLiteral("<i>a<b>b</i>c</b>")), QStringLiteral("<i>a<b>b</b></i><b>c</b>"), "crossing i/b split");
    CHECK_EQ(SimpleStyledTextSubFormat::repairNesting(QStringLiteral("<ibu>x</i/b/u> y</b>")), QStringLiteral("<ibu>x</i/b/u> y"), "compact tags kept, a closing tag with nothing open dropped");
    {
        auto subs = load(QStringLiteral("simple.srt"));
        const QString out = render(*subs, QStringLiteral("SubRip"));
        CHECK(!out.contains(QStringLiteral("</b></font>")) && !out.contains(QStringLiteral("</i></font>")), "SubRip output nests font runs");
    }
    // Font tags after other tags keep their positions (Java lost them).
    {
        SubFile sf(QStringLiteral("/tmp/f.srt"), SubFile::EXTENSION_GIVEN);
        Subtitles s(sf);
        s.populate(sf, QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\n<i>it</i> <font size=\"27\">sz</font> x <strong>b</strong>\n\n"), false);
        CHECK_EQ(s.get(0)->getText(), QStringLiteral("it sz x b"), "font/strong stripped");
        CHECK(s.get(0)->getStyleover(StyleType::FONTSIZE) != nullptr, "font size override kept");
        CHECK_EQ(s.get(0)->getStyleover(StyleType::FONTSIZE)->dump(), QStringLiteral("{(27,3)(24,5)}"), "font size run in stripped coordinates");
        CHECK_EQ(s.get(0)->getStyleover(StyleType::BOLD)->dump(), QStringLiteral("{(true,8)}"), "strong → bold");
    }
    // UTF-16 round trip with BOM.
    {
        SubFile sf(QStringLiteral("/tmp/jubler-qt-u16.srt"), SubFile::EXTENSION_GIVEN);
        Subtitles subs(sf);
        subs.add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("Γειά")));
        sf.setEncoding(QStringLiteral("UTF-16"));
        CHECK(FileCommunicator::save(subs, sf, nullptr).isNull(), "UTF-16 save ok");
        const QByteArray raw = FileCommunicator::loadRawBytes(sf.getSaveFile());
        CHECK(raw.startsWith("\xFE\xFF"), "UTF-16 written with a big-endian BOM");
        SubFile back(sf.getSaveFile(), SubFile::EXTENSION_GIVEN);
        const QString data = FileCommunicator::load(back);
        CHECK_EQ(back.getEncoding(), QStringLiteral("UTF-16"), "UTF-16 detected on reload");
        CHECK(data.contains(QStringLiteral("Γειά")), "UTF-16 text round trip");
        QFile::remove(sf.getSaveFile());
        sf.setEncoding(QStringLiteral("NO-SUCH-CHARSET"));
        CHECK(FileCommunicator::save(subs, sf, nullptr).contains(QStringLiteral("not supported")), "unknown charset reported");
    }
    // Relaxed 8-bit decode replaces undefined bytes instead of failing.
    {
        Prefs::set(QStringLiteral("default.encoding.8bit"), QStringLiteral("ISO-8859-7"));
        Options::load();
        SubFile sf(QStringLiteral("/tmp/x.srt"), SubFile::EXTENSION_GIVEN);
        QByteArray bytes("ab\xAE" "cd");
        const QString t = FileCommunicator::detectAndDecode(sf, bytes, false);
        CHECK(!t.isNull() && t.startsWith(QStringLiteral("ab")) && t.contains(QStringLiteral("cd")), "undefined byte replaced under the 8-bit floor");
        CHECK(t.contains(QChar(0xFFFD)), "replacement is U+FFFD");
        // Relaxed multi-byte: malformed input fails (the Java REPORTed it).
        CHECK(FileCommunicator::decodeFrom(QByteArray("ab\xC3(cd"), QStringLiteral("UTF-8"), false).isNull(), "malformed UTF-8 fails relaxed");
        // A BOM-declared UTF-8 file with malformed bytes falls through to the 8-bit floor.
        SubFile bomf(QStringLiteral("/tmp/x.srt"), SubFile::EXTENSION_GIVEN);
        FileCommunicator::detectAndDecode(bomf, QByteArray("\xEF\xBB\xBF" "ab\xC3(cd"), false);
        CHECK_EQ(bomf.getEncoding(), QStringLiteral("ISO-8859-7"), "malformed BOM UTF-8 → 8-bit floor");
        Prefs::set(QStringLiteral("default.encoding.8bit"), QStringLiteral("ISO-8859-1"));
        Options::load();
    }
    // TTML span style reference only adds true flags.
    {
        SubFile sf(QStringLiteral("/tmp/x.ttml"), SubFile::EXTENSION_GIVEN);
        Subtitles s(sf);
        s.populate(sf, QStringLiteral("<tt xmlns=\"http://www.w3.org/ns/ttml\" xmlns:tts=\"http://www.w3.org/ns/ttml#styling\"><head><styling>"
                                      "<style xml:id=\"italic\" tts:fontStyle=\"italic\"/><style xml:id=\"boldOnly\" tts:fontWeight=\"bold\"/></styling></head>"
                                      "<body><div><p begin=\"00:00:01.000\" end=\"00:00:02.000\" style=\"italic\">a <span style=\"boldOnly\">x</span> b</p></div></body></tt>"), false);
        CHECK_EQ(s.size(), 1, "ttml span test loaded");
        CHECK(s.get(0)->getStyleover(StyleType::ITALIC) == nullptr, "span style reference does not inject false italic");
        CHECK(s.get(0)->getStyleover(StyleType::BOLD) != nullptr, "span style reference adds bold");
    }
    // YouTube multi-line cues.
    {
        SubFile sf(QStringLiteral("/tmp/x.sbv"), SubFile::EXTENSION_GIVEN);
        Subtitles s(sf);
        s.populate(sf, QStringLiteral("0:00:01.000,0:00:02.000\nTwo\nlines\n\n0:00:03.000,0:00:04.000\nOne\n\n"), false);
        CHECK_EQ(s.size(), 2, "sbv cues");
        CHECK_EQ(s.get(0)->getText(), QStringLiteral("Two\nlines"), "sbv multi-line cue");
    }
}

// ---- TTML / DFXP / ITT: Java parity and the standard TTML features ---------

static std::unique_ptr<Subtitles> loadW3C(const QString &ext, const QString &text, float fps = -1) {
    SubFile sf(QStringLiteral("/tmp/w3c.") + ext, SubFile::EXTENSION_GIVEN);
    if (fps > 0) sf.setFPS(fps);
    auto subs = std::make_unique<Subtitles>(sf);
    subs->populate(sf, text, false);
    return subs;
}

static QString ttmlDoc(const QString &body, const QString &head = QString(), const QString &rootAttrs = QString()) {
    return QStringLiteral("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tt xmlns=\"http://www.w3.org/ns/ttml\" xmlns:tts=\"http://www.w3.org/ns/ttml#styling\" "
                          "xmlns:ttp=\"http://www.w3.org/ns/ttml#parameter\" xmlns:ttm=\"http://www.w3.org/ns/ttml#metadata\" xml:lang=\"en\" ")
        + rootAttrs + QStringLiteral(">\n<head>") + head + QStringLiteral("</head>\n<body>") + body + QStringLiteral("</body>\n</tt>\n");
}

static QString dumpOf(const SubEntry &e, StyleType::Id id) {
    const Styleover *o = e.getStyleover(id);
    return o ? o->dump() : QString();
}

static void testW3CFamily() {
    using D = Direction;
    // F1: ITT without ttp:frameRate counts frames at 30 fps, not the document FPS.
    {
        auto s = loadW3C(QStringLiteral("itt"), ttmlDoc(QStringLiteral("<div><p begin=\"00:00:01:15\" end=\"00:00:02:00\">x</p></div>"), QString(),
                                                        QStringLiteral("ttp:timeBase=\"smpte\"")), 25);
        CHECK_EQ(s->size(), 1, "ITT without frame rate loaded");
        CHECK_EQ(s->get(0)->getStartTime().getMillis(), 1500, "ITT frames at 30 fps without ttp:frameRate");
    }
    // F2: whitespace-only text between spans is kept; spaces collapse across nodes.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div><p begin=\"1s\" end=\"2s\"><span>a</span> <span>b</span></p>"
                                                                        "<p begin=\"3s\" end=\"4s\">\n  <span>x</span>\n  <span> y </span>\n  <br/>\n  <span>z</span>\n</p></div>")));
        CHECK_EQ(s->size(), 2, "whitespace entries");
        CHECK_EQ(s->get(0)->getText(), QStringLiteral("a b"), "space between spans kept");
        CHECK_EQ(s->get(1)->getText(), QStringLiteral("x y\nz"), "spaces collapse across nodes, trimmed around breaks");
    }
    // F3: ITT at fractional FPS: frames of the effective rate labelled at the nominal rate.
    {
        SubFile sf(QStringLiteral("/tmp/f3.itt"), SubFile::EXTENSION_GIVEN);
        sf.setFPS(23.976f);
        Subtitles subs(sf);
        subs.add(std::make_shared<SubEntry>(Time::fromMillis(1001), Time::fromMillis(60060), QStringLiteral("x")));
        const QString out = render(subs, QStringLiteral("iTunes Timed Text"));
        CHECK(out.contains(QStringLiteral("ttp:frameRate=\"24\"")) && out.contains(QStringLiteral("ttp:frameRateMultiplier=\"1000 1001\"")), "23.976 written as 24 x 1000/1001");
        CHECK(out.contains(QStringLiteral("begin=\"00:00:01:00\"")), "1001 ms at 23.976 is frame 24 = 00:00:01:00");
        CHECK(out.contains(QStringLiteral("end=\"00:01:00:00\"")), "60060 ms at 23.976 is frame 1440 = 00:01:00:00");
        auto back = loadW3C(QStringLiteral("itt"), out);
        CHECK_EQ(back->get(0)->getStartTime().getMillis(), 1001, "23.976 ITT reload start");
        CHECK_EQ(back->get(0)->getFinishTime().getMillis(), 60060, "23.976 ITT reload end");
        sf.setFPS(12.5f);
        Subtitles half(sf);
        half.add(std::make_shared<SubEntry>(Time::fromMillis(2000), Time::fromMillis(3000), QStringLiteral("x")));
        const QString out12 = render(half, QStringLiteral("iTunes Timed Text"));
        CHECK(out12.contains(QStringLiteral("ttp:frameRate=\"13\"")) && out12.contains(QStringLiteral("ttp:frameRateMultiplier=\"25 26\"")), "12.5 fps: nominal 13 x 25/26");
        CHECK_EQ(loadW3C(QStringLiteral("itt"), out12)->get(0)->getStartTime().getMillis(), 2000, "12.5 fps round trip");
        sf.setFPS(25);
        Subtitles pal(sf);
        pal.add(std::make_shared<SubEntry>(Time::fromMillis(999), Time::fromMillis(2000), QStringLiteral("x")));
        const QString out25 = render(pal, QStringLiteral("iTunes Timed Text"));
        CHECK(out25.contains(QStringLiteral("begin=\"00:00:01:00\"")) && !out25.contains(QStringLiteral("frameRateMultiplier")), "999 ms at 25 fps rounds up to the next second");
    }
    // F12: the TTAF 2006/04 and 2006/10 namespaces.
    for (const char *ns : {"http://www.w3.org/2006/10/ttaf1", "http://www.w3.org/2006/04/ttaf1"}) {
        const QString n = QLatin1String(ns);
        auto s = loadW3C(QStringLiteral("dfxp"), QStringLiteral("<tt xmlns=\"%1\" xmlns:tts=\"%1#styling\" xmlns:ttp=\"%1#parameter\"><head><styling>"
                                                                "<style id=\"s\" tts:color=\"red\"/></styling></head><body><div>"
                                                                "<p begin=\"1s\" end=\"2s\" style=\"s\">a <span tts:fontStyle=\"italic\">b</span></p></div></body></tt>").arg(n));
        CHECK_EQ(s->size(), 1, qPrintable(n));
        CHECK_EQ((long long)s->get(0)->getStyle()->color(StyleType::PRIMARY).rgb(), (long long)0xff0000, qPrintable(n + QStringLiteral(" style colour")));
        CHECK_EQ(dumpOf(*s->get(0), StyleType::ITALIC), QStringLiteral("{(true,2)}"), qPrintable(n + QStringLiteral(" span italic")));
    }
    // F25/F26: "default" is the document Default; case-insensitive alignment and RGB(); the last duplicate id wins.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div><p begin=\"1s\" end=\"2s\" style=\"s\" region=\"r\">a</p>"
                                                                        "<p begin=\"3s\" end=\"4s\"><span tts:color=\"RGB(0, 0, 255)\">b</span></p></div>"),
                                                         QStringLiteral("<styling><style xml:id=\"default\" tts:color=\"yellow\"/><style xml:id=\"s\" tts:fontStyle=\"italic\"/>"
                                                                        "<style xml:id=\"s\" tts:fontWeight=\"bold\"/></styling>"
                                                                        "<layout><region xml:id=\"r\" tts:displayAlign=\"BEFORE\" tts:textAlign=\"Left\"/></layout>")));
        CHECK_EQ(s->getStyleList().size(), 2, "default merged into Default, s once");
        CHECK_EQ((long long)s->getStyleList().get(0)->color(StyleType::PRIMARY).rgb(), (long long)0xffff00, "default style -> Default");
        const SubStylePtr st = s->get(0)->getStyle();
        CHECK(st->flag(StyleType::BOLD) && !st->flag(StyleType::ITALIC), "last duplicate style definition wins");
        CHECK_EQ(directionName(effDir(*s->get(0))), directionName(D::TOPLEFT), "BEFORE/Left case-insensitive");
        CHECK_EQ(dumpOf(*s->get(1), StyleType::PRIMARY), QStringLiteral("{(ff0000ff,0)}"), "RGB() colour");
    }
    // F27: written regions stay within the screen and keep the margins.
    {
        SubFile sf(QStringLiteral("/tmp/f27.ttml"), SubFile::EXTENSION_GIVEN);
        Subtitles subs(sf);
        auto e = std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("right"));
        e->setStyle(subs.getStyleList().get(0));
        e->setOverStyle(StyleType::DIRECTION, StyleValue(D::RIGHT), 0, 5);
        e->setOverStyle(StyleType::LEFTMARGIN, StyleValue(10), 0, 5);
        e->setOverStyle(StyleType::RIGHTMARGIN, StyleValue(30), 0, 5);
        subs.add(e);
        const QString out = render(subs, QStringLiteral("TTML"));
        CHECK(out.contains(QStringLiteral("tts:origin=\"10% 50%\" tts:extent=\"60% 15%\"")), "region between the margins");
        auto back = loadW3C(QStringLiteral("ttml"), out);
        CHECK_EQ(directionName(effDir(*back->get(0))), directionName(D::RIGHT), "right region reload");
        CHECK_EQ(std::get<int>(*back->get(0)->overValue(StyleType::RIGHTMARGIN, 0, 5)), 30, "right margin reload");
        CHECK_EQ(std::get<int>(*back->get(0)->overValue(StyleType::LEFTMARGIN, 0, 5)), 10, "left margin reload");
    }
    // F29: timing — ticks, dur, nested offsets, clipping, dropNTSC.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div><p begin=\"10000000t\" end=\"25000000t\">t</p><p begin=\"1s\" dur=\"2s\">d</p></div>"
                                                                        "<div begin=\"10s\" end=\"11.5s\"><p begin=\"1s\" end=\"2s\">n</p><p>w</p></div>"),
                                                         QString(), QStringLiteral("ttp:tickRate=\"10000000\"")));
        CHECK_EQ(s->size(), 4, "timed entries");
        CHECK_EQ(s->get(0)->getStartTime().getMillis(), 1000, "tick begin");
        CHECK_EQ(s->get(0)->getFinishTime().getMillis(), 2500, "tick end");
        CHECK_EQ(s->get(1)->getFinishTime().getMillis(), 3000, "begin + dur");
        CHECK_EQ(s->get(2)->getStartTime().getMillis(), 11000, "div begin offsets its paragraphs");
        CHECK_EQ(s->get(2)->getFinishTime().getMillis(), 11500, "clipped to the div end");
        CHECK_EQ(s->get(3)->getStartTime().getMillis(), 10000, "untimed paragraph takes the div timing");
        auto drop = loadW3C(QStringLiteral("itt"), ttmlDoc(QStringLiteral("<div><p begin=\"00:01:00:02\" end=\"00:01:01:00\">x</p></div>"), QString(),
                                                           QStringLiteral("ttp:timeBase=\"smpte\" ttp:frameRate=\"30\" ttp:frameRateMultiplier=\"1000 1001\" ttp:dropMode=\"dropNTSC\"")));
        CHECK_EQ(drop->get(0)->getStartTime().getMillis(), 60060, "dropNTSC from ttp:dropMode");
    }
    // F29: styles — chaining, lists, tts on containers, textAlign in styles, sizes, colours, decorations.
    {
        const QString head = QStringLiteral("<styling><style xml:id=\"a\" tts:fontStyle=\"italic\"/><style xml:id=\"b\" style=\"a\" tts:fontWeight=\"bold\"/>"
                                            "<style xml:id=\"c\" tts:color=\"lime\"/><style xml:id=\"left\" tts:textAlign=\"left\"/>"
                                            "<style xml:id=\"top\" tts:displayAlign=\"before\"/>"
                                            "<style xml:id=\"box\" tts:backgroundColor=\"#00000080\" tts:textOutline=\"black 2px\" tts:opacity=\"0.5\" tts:textDecoration=\"lineThrough\"/>"
                                            "</styling>");
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div tts:color=\"navy\">"
                                                                        "<p begin=\"1s\" end=\"2s\" style=\"b\">chain</p>"
                                                                        "<p begin=\"2s\" end=\"3s\" style=\"a c\">list</p>"
                                                                        "<p begin=\"3s\" end=\"4s\" tts:fontStyle=\"italic\">inherit</p>"
                                                                        "<p begin=\"4s\" end=\"5s\" style=\"left\">left</p>"
                                                                        "<p begin=\"5s\" end=\"6s\" style=\"top\" tts:textAlign=\"right\">topright</p>"
                                                                        "<p begin=\"6s\" end=\"7s\" style=\"box\">box</p>"
                                                                        "<p begin=\"7s\" end=\"8s\"><span tts:fontSize=\"1.5em\">em</span><span tts:color=\"#ff000080\">aa</span><span tts:color=\"green\">g</span></p>"
                                                                        "</div>"), head));
        CHECK_EQ(s->size(), 7, "style entries");
        const SubStylePtr b = s->get(0)->getStyle();
        CHECK(b->getName() == QLatin1String("b") && b->flag(StyleType::BOLD) && b->flag(StyleType::ITALIC), "chained style");
        CHECK_EQ(s->get(1)->getStyle()->getName(), QStringLiteral("a"), "style list: first style");
        CHECK_EQ(dumpOf(*s->get(1), StyleType::PRIMARY), QStringLiteral("{(ff00ff00,0)}"), "style list: second style's colour");
        CHECK_EQ(dumpOf(*s->get(0), StyleType::PRIMARY), QStringLiteral("{(ff000080,0)}"), "div colour inherited");
        CHECK_EQ(dumpOf(*s->get(2), StyleType::ITALIC), QStringLiteral("{(true,0)}"), "tts on p");
        CHECK_EQ(directionName(s->get(3)->getStyle()->direction()), directionName(D::BOTTOMLEFT), "textAlign in a style");
        CHECK_EQ(directionName(effDir(*s->get(3))), directionName(D::BOTTOMLEFT), "no override for the style's own alignment");
        CHECK_EQ(directionName(effDir(*s->get(4))), directionName(D::TOPRIGHT), "region-less placement from style + p");
        const SubStylePtr box = s->get(5)->getStyle();
        CHECK_EQ(box->integral(StyleType::BORDERSTYLE), 1, "background -> opaque box");
        CHECK_EQ((long long)box->color(StyleType::OUTLINE).argb(), (long long)0x80000000, "box colour with alpha");
        CHECK_EQ(box->real(StyleType::BORDERSIZE), 2.0f, "outline thickness");
        CHECK_EQ(box->color(StyleType::PRIMARY).alpha(), 128, "opacity");
        CHECK(box->flag(StyleType::STRIKETHROUGH), "lineThrough");
        CHECK_EQ(std::get<int>(*s->get(6)->overValue(StyleType::FONTSIZE, 0, 1)), 24, "1.5em = 24");
        CHECK_EQ((long long)std::get<AlphaColor>(*s->get(6)->overValue(StyleType::PRIMARY, 2, 3)).argb(), (long long)0x80ff0000, "#rrggbbaa");
        CHECK_EQ((long long)std::get<AlphaColor>(*s->get(6)->overValue(StyleType::PRIMARY, 4, 5)).argb(), (long long)0xff008000, "CSS green");
        // lineThrough survives the TTML writer.
        auto back = reload(*s, QStringLiteral("TTML"), QStringLiteral("ttml"));
        CHECK(back->get(5)->getStyle()->flag(StyleType::STRIKETHROUGH), "lineThrough round trip");
    }
    // F29: xml:space, cell sizes, pixel regions, metadata, xml:lang.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div><p begin=\"1s\" end=\"2s\" xml:space=\"preserve\">a  b\nc</p>"
                                                                        "<p begin=\"2s\" end=\"3s\" region=\"px\" tts:fontSize=\"1c\">cell</p></div>"),
                                                         QStringLiteral("<metadata><ttm:title>Movie</ttm:title><ttm:desc>About</ttm:desc><ttm:copyright>Studio</ttm:copyright></metadata>"
                                                                        "<layout><region xml:id=\"px\" tts:origin=\"100px 400px\" tts:extent=\"600px 100px\"/></layout>"),
                                                         QStringLiteral("tts:extent=\"1000px 500px\" ttp:cellResolution=\"40 20\"")));
        CHECK_EQ(s->size(), 2, "space/cell entries");
        CHECK_EQ(s->get(0)->getText(), QStringLiteral("a  b\nc"), "xml:space preserve");
        CHECK_EQ(std::get<int>(*s->get(1)->overValue(StyleType::FONTSIZE, 0, 1)), 25, "1c = 500px / 20 rows");
        CHECK_EQ(directionName(effDir(*s->get(1))), directionName(D::BOTTOM), "pixel region at the bottom");
        CHECK_EQ(std::get<int>(*s->get(1)->overValue(StyleType::LEFTMARGIN, 0, 1)), 10, "pixel region left margin");
        CHECK_EQ(std::get<int>(*s->get(1)->overValue(StyleType::RIGHTMARGIN, 0, 1)), 30, "pixel region right margin");
        CHECK_EQ(s->getAttribs().title, QStringLiteral("Movie"), "ttm:title");
        CHECK_EQ(s->getAttribs().comments, QStringLiteral("About"), "ttm:desc");
        CHECK_EQ(s->getAttribs().author, QStringLiteral("Studio"), "ttm:copyright");
        CHECK(render(*s, QStringLiteral("TTML")).contains(QStringLiteral("<ttm:title>Movie</ttm:title>")), "title written back");
        auto own = loadW3C(QStringLiteral("ttml"), render(*s, QStringLiteral("TTML")).replace(QStringLiteral("Movie"), QStringLiteral("TTML Document")));
        CHECK_EQ(own->getAttribs().title, QString(), "the generated title is not a document title");
    }
    // The document's xml:lang is kept and written back; without one, the format's default.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div><p begin=\"1s\" end=\"2s\" xml:lang=\"de\">a</p></div>"))
                                                     .replace(QStringLiteral("xml:lang=\"en\""), QStringLiteral("xml:lang=\"el-GR\"")));
        CHECK_EQ(s->getFormatData().value(QStringLiteral("ttml.lang")), QStringLiteral("el-GR"), "xml:lang of <tt> kept");
        CHECK(render(*s, QStringLiteral("TTML")).contains(QStringLiteral("<tt xml:lang=\"el-GR\"")), "xml:lang written back (TTML)");
        CHECK(render(*s, QStringLiteral("iTunes Timed Text")).contains(QStringLiteral("<tt xml:lang=\"el-GR\"")), "xml:lang written back (ITT)");
        CHECK(render(*s, QStringLiteral("DFXP")).contains(QStringLiteral("<tt xml:lang=\"el-GR\"")), "xml:lang written back (DFXP)");
        SubFile sf(QStringLiteral("/tmp/lang.itt"), SubFile::EXTENSION_GIVEN);
        Subtitles fresh(sf);
        fresh.add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("x")));
        CHECK(render(fresh, QStringLiteral("iTunes Timed Text")).contains(QStringLiteral("<tt xml:lang=\"en-US\"")), "ITT default language");
        CHECK(render(fresh, QStringLiteral("TTML")).contains(QStringLiteral("<tt xml:lang=\"en\"")), "TTML default language");
    }
    // tts:writingMode: vertical text is a whole-entry ±90 rotation; the region's
    // displayAlign places the column, its textAlign the text; written back as a vertical region.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div><p begin=\"1s\" end=\"2s\" region=\"v\">rl</p>"
                                                                        "<p begin=\"2s\" end=\"3s\" region=\"l\">lr</p>"
                                                                        "<p begin=\"3s\" end=\"4s\" region=\"h\">h</p></div>"),
                                                         QStringLiteral("<layout><region xml:id=\"v\" tts:writingMode=\"tb\" tts:displayAlign=\"before\" tts:textAlign=\"start\"/>"
                                                                        "<region xml:id=\"l\" tts:writingMode=\"tblr\" tts:displayAlign=\"before\" tts:textAlign=\"center\"/>"
                                                                        "<region xml:id=\"h\" tts:writingMode=\"lrtb\"/></layout>")));
        CHECK_EQ(s->size(), 3, "writing mode entries");
        CHECK_EQ(dumpOf(*s->get(0), StyleType::ANGLE), QStringLiteral("{(90.0,0)}"), "tb(rl) -> angle 90");
        CHECK_EQ(directionName(effDir(*s->get(0))), directionName(D::TOPRIGHT), "tbrl before/start: top right");
        CHECK_EQ(dumpOf(*s->get(1), StyleType::ANGLE), QStringLiteral("{(-90.0,0)}"), "tblr -> angle -90");
        CHECK_EQ(directionName(effDir(*s->get(1))), directionName(D::LEFT), "tblr before/center: left");
        CHECK(s->get(2)->getStyleover(StyleType::ANGLE) == nullptr, "lrtb: no rotation");
        for (const QString &fmt : {QStringLiteral("TTML"), QStringLiteral("DFXP"), QStringLiteral("iTunes Timed Text")}) {
            const QString out = render(*s, fmt);
            CHECK(out.contains(QStringLiteral("xml:id=\"topright_tbrl\"")) && out.contains(QStringLiteral("tts:writingMode=\"tbrl\"")), qPrintable(fmt + QStringLiteral(" tbrl region")));
            CHECK(out.contains(QStringLiteral("tts:writingMode=\"tblr\"")), qPrintable(fmt + QStringLiteral(" tblr region")));
            const QString ext = fmt == QLatin1String("TTML") ? QStringLiteral("ttml") : fmt == QLatin1String("DFXP") ? QStringLiteral("dfxp") : QStringLiteral("itt");
            auto back = loadW3C(ext, out);
            CHECK_EQ(back->size(), 3, qPrintable(fmt + QStringLiteral(" vertical reload")));
            CHECK_EQ(dumpOf(*back->get(0), StyleType::ANGLE), QStringLiteral("{(90.0,0)}"), qPrintable(fmt + QStringLiteral(" tbrl round trip")));
            CHECK_EQ(directionName(effDir(*back->get(0))), directionName(D::TOPRIGHT), qPrintable(fmt + QStringLiteral(" tbrl placement round trip")));
            CHECK_EQ(dumpOf(*back->get(1), StyleType::ANGLE), QStringLiteral("{(-90.0,0)}"), qPrintable(fmt + QStringLiteral(" tblr round trip")));
            CHECK_EQ(directionName(effDir(*back->get(1))), directionName(D::LEFT), qPrintable(fmt + QStringLiteral(" tblr placement round trip")));
            CHECK(back->get(2)->getStyleover(StyleType::ANGLE) == nullptr, qPrintable(fmt + QStringLiteral(" horizontal stays horizontal")));
        }
        // A vertical entry at the bottom also gets a region.
        SubFile sf(QStringLiteral("/tmp/v.ttml"), SubFile::EXTENSION_GIVEN);
        Subtitles subs(sf);
        auto e = std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("v"));
        e->setStyle(subs.getStyleList().get(0));
        e->setOverStyle(StyleType::ANGLE, StyleValue(90.0f), 0, 1);
        subs.add(e);
        auto back = reload(subs, QStringLiteral("TTML"), QStringLiteral("ttml"));
        CHECK_EQ(dumpOf(*back->get(0), StyleType::ANGLE), QStringLiteral("{(90.0,0)}"), "bottom vertical round trip");
        CHECK_EQ(directionName(effDir(*back->get(0))), directionName(D::BOTTOM), "bottom vertical placement");
        CHECK_EQ(std::get<int>(back->get(0)->overValue(StyleType::LEFTMARGIN, 0, 1).value_or(StyleValue(20))), 20, "bottom vertical margins");
    }
    // Span timing: the paragraph splits at every span boundary; untimed text shows throughout.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div><p begin=\"1s\" end=\"5s\">Who <span begin=\"1s\" end=\"2s\" tts:fontStyle=\"italic\">is</span>"
                                                                        " <span begin=\"2s\">there</span></p>"
                                                                        "<p begin=\"6s\" end=\"7s\"><span begin=\"0s\" end=\"1s\">all</span></p>"
                                                                        "<p begin=\"8s\" end=\"9s\"><span begin=\"5s\">never</span>plain</p></div>")));
        CHECK_EQ(s->size(), 5, "timed spans split the paragraph");
        auto at = [&](int i) { return QStringLiteral("%1-%2 %3").arg(s->get(i)->getStartTime().getMillis()).arg(s->get(i)->getFinishTime().getMillis()).arg(s->get(i)->getText()); };
        CHECK_EQ(at(0), QStringLiteral("1000-2000 Who"), "before the spans");
        CHECK_EQ(at(1), QStringLiteral("2000-3000 Who is"), "first span shown");
        CHECK_EQ(dumpOf(*s->get(1), StyleType::ITALIC), QStringLiteral("{(true,4)}"), "span styling kept in its interval");
        CHECK_EQ(at(2), QStringLiteral("3000-5000 Who there"), "second span until the paragraph end");
        CHECK_EQ(at(3), QStringLiteral("6000-7000 all"), "span covering the paragraph");
        CHECK_EQ(at(4), QStringLiteral("8000-9000 plain"), "span after the paragraph end not shown");
    }
    // tts:backgroundColor / textOutline / opacity set directly on content elements.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div tts:backgroundColor=\"black\"><p begin=\"1s\" end=\"2s\">boxed</p></div>"
                                                                        "<div><p begin=\"2s\" end=\"3s\" tts:textOutline=\"red 3px\" tts:opacity=\"0.5\">outlined</p>"
                                                                        "<p begin=\"3s\" end=\"4s\">a <span tts:textOutline=\"blue 1px\" tts:backgroundColor=\"yellow\">b</span></p>"
                                                                        "<p begin=\"4s\" end=\"5s\"><span tts:backgroundColor=\"#00ff0080\">all</span></p>"
                                                                        "<p begin=\"5s\" end=\"6s\" region=\"r\">region</p></div>"),
                                                         QStringLiteral("<layout><region xml:id=\"r\" tts:backgroundColor=\"black\"/></layout>")));
        CHECK_EQ(s->size(), 5, "direct tts entries");
        CHECK_EQ(dumpOf(*s->get(0), StyleType::BORDERSTYLE), QStringLiteral("{(1,0)}"), "div background -> box");
        CHECK_EQ(dumpOf(*s->get(0), StyleType::OUTLINE), QStringLiteral("{(ff000000,0)}"), "div background -> box colour");
        CHECK_EQ(dumpOf(*s->get(1), StyleType::OUTLINE), QStringLiteral("{(ffff0000,0)}"), "p textOutline colour");
        CHECK_EQ(dumpOf(*s->get(1), StyleType::BORDERSIZE), QStringLiteral("{(3.0,0)}"), "p textOutline thickness");
        CHECK_EQ(s->get(1)->getStyleover(StyleType::BORDERSTYLE) == nullptr, true, "outline is not a box");
        CHECK_EQ((long long)std::get<AlphaColor>(*s->get(1)->overValue(StyleType::PRIMARY, 0, 1)).alpha(), 128LL, "p opacity");
        CHECK_EQ(dumpOf(*s->get(2), StyleType::OUTLINE), QStringLiteral("{(ff0000ff,2)}"), "span outline colour on its characters");
        CHECK(s->get(2)->getStyleover(StyleType::BORDERSTYLE) == nullptr && s->get(2)->getStyleover(StyleType::BORDERSIZE) == nullptr,
              "partial span: no whole-entry box or thickness");
        CHECK_EQ(dumpOf(*s->get(3), StyleType::BORDERSTYLE), QStringLiteral("{(1,0)}"), "whole-text span background -> box");
        CHECK_EQ(dumpOf(*s->get(3), StyleType::OUTLINE), QStringLiteral("{(8000ff00,0)}"), "whole-text span box colour");
        CHECK(s->get(4)->getStyleover(StyleType::BORDERSTYLE) == nullptr, "region background is not a text box");
        // F38: box and outline overrides written back on the p and spans.
        for (const QString &fmt : {QStringLiteral("TTML"), QStringLiteral("DFXP"), QStringLiteral("iTunes Timed Text")}) {
            const QString ext = fmt == QLatin1String("TTML") ? QStringLiteral("ttml") : fmt == QLatin1String("DFXP") ? QStringLiteral("dfxp") : QStringLiteral("itt");
            auto back = loadW3C(ext, render(*s, fmt));
            CHECK_EQ(back->size(), 5, qPrintable(fmt + QStringLiteral(" box/outline reload")));
            for (int i = 0; i < 4; ++i)
                for (StyleType::Id t : {StyleType::BORDERSTYLE, StyleType::BORDERSIZE, StyleType::OUTLINE})
                    CHECK_EQ(dumpOf(*back->get(i), t), dumpOf(*s->get(i), t), qPrintable(fmt + QStringLiteral(" box/outline round trip %1/%2").arg(i).arg(int(t))));
        }
    }
    // F38: box and outline of styles written back; a box removed by an entry.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div><p begin=\"1s\" end=\"2s\" style=\"box\">a</p>"
                                                                        "<p begin=\"2s\" end=\"3s\" style=\"ol\">b <span tts:textOutline=\"lime 3px\">c</span></p>"
                                                                        "<p begin=\"3s\" end=\"4s\" style=\"box\" tts:backgroundColor=\"transparent\" tts:textOutline=\"none\">d</p>"
                                                                        "<p begin=\"4s\" end=\"5s\" style=\"box\">e <span tts:backgroundColor=\"red\">f</span></p></div>"),
                                                         QStringLiteral("<styling><style xml:id=\"box\" tts:backgroundColor=\"#00000080\" tts:textOutline=\"black 2px\"/>"
                                                                        "<style xml:id=\"ol\" tts:textOutline=\"red 3px\"/></styling>")));
        CHECK_EQ(s->size(), 4, "box style entries");
        CHECK_EQ(dumpOf(*s->get(2), StyleType::BORDERSTYLE), QStringLiteral("{(0,0)}"), "transparent background removes the box");
        CHECK_EQ(dumpOf(*s->get(2), StyleType::BORDERSIZE), QStringLiteral("{(0.0,0)}"), "textOutline none removes the outline");
        CHECK_EQ(dumpOf(*s->get(3), StyleType::OUTLINE), QStringLiteral("{(ffff0000,2)}"), "span background inside a box: box colour");
        const QString out = render(*s, QStringLiteral("TTML"));
        CHECK(out.contains(QStringLiteral("tts:backgroundColor=\"#00000080\" tts:textOutline=\"#00000080 2px\"")), "box style written");
        CHECK(out.contains(QStringLiteral("tts:textOutline=\"#ff0000 3px\"")), "outline style written");
        auto back = loadW3C(QStringLiteral("ttml"), out);
        const SubStylePtr box = back->getStyleList().getStyleByName(QStringLiteral("box"));
        CHECK(box && box->integral(StyleType::BORDERSTYLE) == 1, "box style round trip");
        CHECK(box && box->color(StyleType::OUTLINE).argb() == 0x80000000u, "box colour round trip");
        CHECK(box && box->real(StyleType::BORDERSIZE) == 2.0f, "box style thickness round trip");
        const SubStylePtr ol = back->getStyleList().getStyleByName(QStringLiteral("ol"));
        CHECK(ol && ol->integral(StyleType::BORDERSTYLE) == 0 && ol->real(StyleType::BORDERSIZE) == 3.0f && ol->color(StyleType::OUTLINE).rgb() == 0xff0000u,
              "outline style round trip");
        for (int i = 0; i < 4; ++i)
            for (StyleType::Id t : {StyleType::BORDERSTYLE, StyleType::BORDERSIZE, StyleType::OUTLINE})
                CHECK_EQ(dumpOf(*back->get(i), t), dumpOf(*s->get(i), t), qPrintable(QStringLiteral("style box/outline override round trip %1/%2").arg(i).arg(int(t))));
        // A plain document gains no box or outline.
        SubFile sf(QStringLiteral("/tmp/plain.ttml"), SubFile::EXTENSION_GIVEN);
        Subtitles plain(sf);
        plain.add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("x")));
        const QString plainOut = render(plain, QStringLiteral("TTML"));
        CHECK(!plainOut.contains(QStringLiteral("backgroundColor")) && !plainOut.contains(QStringLiteral("textOutline")), "no box/outline for defaults");
    }
    // F39: timeContainer="seq": each child begins after the previous one ends.
    {
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(QStringLiteral("<div begin=\"10s\" timeContainer=\"seq\"><p dur=\"1s\">a</p><p begin=\"0.5s\" dur=\"2s\">b</p>"
                                                                        "<p end=\"1s\">c</p><p>zero</p><p dur=\"1s\">d</p></div>"
                                                                        "<div timeContainer=\"seq\"><p begin=\"30s\" dur=\"1s\">x</p><div><p>unbounded</p></div><p dur=\"1s\">never</p></div>"
                                                                        "<div><p begin=\"40s\" end=\"50s\" timeContainer=\"seq\">hidden <span dur=\"1s\">one</span><span dur=\"2s\">two</span></p>"
                                                                        "<p begin=\"60s\"><span dur=\"1s\">p1</span><span begin=\"1s\" dur=\"2s\">p2</span></p></div>")));
        QStringList got;
        for (int i = 0; i < s->size(); ++i)
            got.append(QStringLiteral("%1-%2 %3").arg(s->get(i)->getStartTime().getMillis()).arg(s->get(i)->getFinishTime().getMillis()).arg(s->get(i)->getText()));
        CHECK_EQ(got.join(QLatin1String(" | ")),
                 QStringLiteral("10000-11000 a | 11500-13500 b | 13500-14500 c | 14500-15500 d | 30000-31000 x | 40000-41000 one | 41000-43000 two"
                                " | 60000-61000 p1 | 61000-63000 p2"),
                 "seq timing, zero-length text leaves, indefinite siblings, par end of timed children");
    }
    // F39: a long sequence stays fast.
    {
        QString body = QStringLiteral("<div timeContainer=\"seq\">");
        for (int i = 0; i < 1500; ++i) body += QStringLiteral("<p dur=\"1s\">%1</p>").arg(i);
        body += QStringLiteral("</div>");
        QElapsedTimer timer;
        timer.start();
        auto s = loadW3C(QStringLiteral("ttml"), ttmlDoc(body));
        CHECK_EQ(s->size(), 1500, "long sequence loaded");
        CHECK_EQ(s->get(1499)->getStartTime().getMillis(), 1499000, "long sequence last begin");
        CHECK(timer.elapsed() < 5000, "long sequence load time");
    }
}

// SRT files in the wild: "{\\anN}" placement and "<br>" line breaks.
static void testSrtInTheWild() {
    auto subs = loadString(QStringLiteral("1\n00:00:01,000 --> 00:00:02,000\n{\\an8}line<br>two\n\n2\n00:00:03,000 --> 00:00:04,000\nplain\n\n"), QStringLiteral("srt"));
    CHECK(subs && subs->size() == 2, "two cues");
    if (!subs || subs->size() != 2) return;
    CHECK_EQ(subs->get(0)->getText(), QStringLiteral("line\ntwo"), "<br> is a line break, {\\an8} not text");
    CHECK(effDir(*subs->get(0)) == Direction::TOP, "{\\an8} is top");
    CHECK(effDir(*subs->get(1)) == Direction::BOTTOM, "no tag: bottom");
    const QString out = render(*subs, QStringLiteral("SubRip"));
    CHECK(out.contains(QStringLiteral("{\\an8}line\ntwo")), "placement written back");
    CHECK(!out.contains(QStringLiteral("{\\an2}")), "bottom writes no tag");
}
