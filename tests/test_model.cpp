/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// Model layer tests: Time, styles, inline style overrides, entries, document.
#include "TestSupport.h"

#include "core/style/AlphaColor.h"
#include "core/style/StyleType.h"
#include "core/style/Styleover.h"
#include "core/style/SubStyle.h"
#include "core/subs/SubEntry.h"
#include "core/subs/Subtitles.h"
#include "core/options/Options.h"
#include "core/style/WebSafeFonts.h"
#include <QFontDatabase>
#include <QGuiApplication>
#include "core/time/Time.h"
#include "core/util/JavaCompat.h"
#include "core/os/FileCommunicator.h"
#include <QFile>

// Java's Float.toString: the float's own shortest digits (the Cps column showed "2.7999999").
static void testFloatToString() {
    CHECK_EQ(jc::floatToString(int(2.8f * 10) / 10.0f), QStringLiteral("2.8"), "2.8f");
    CHECK_EQ(jc::floatToString(25.0f), QStringLiteral("25.0"), "whole number keeps .0");
    CHECK_EQ(jc::floatToString(23.976f), QStringLiteral("23.976"), "fps");
}

static void testTime() {
    Time t(QStringLiteral("01"), QStringLiteral("02"), QStringLiteral("03"), QStringLiteral("500"));
    CHECK(t.isValid(), "valid time");
    CHECK_EQ(t.getMillis(), ((1 * 60 + 2) * 60 + 3) * 1000 + 500, "h:m:s.mmm value");
    CHECK_EQ(t.toString(), QStringLiteral("01:02:03,500"), "toString uses comma");
    CHECK_EQ(t.getSeconds(QLatin1Char('.')), QStringLiteral("01:02:03.500"), "dot separator");
    CHECK_EQ(t.getRoundSeconds(), QStringLiteral("01:02:03"), "round seconds");

    Time pad(QStringLiteral("0"), QStringLiteral("0"), QStringLiteral("1"), QStringLiteral("5"));
    CHECK_EQ(pad.getMillis(), 1500, "fraction '5' is padded to 500 ms");
    Time pad2(QStringLiteral("0"), QStringLiteral("0"), QStringLiteral("1"), QStringLiteral("05"));
    CHECK_EQ(pad2.getMillis(), 1050, "fraction '05' is 50 ms");
    Time lit;
    lit.setTimeLiteral(QStringLiteral("0"), QStringLiteral("0"), QStringLiteral("1"), QStringLiteral("5"));
    CHECK_EQ(lit.getMillis(), 1005, "literal fraction is not padded");

    Time bad(QStringLiteral("x"), QStringLiteral("0"), QStringLiteral("0"), QStringLiteral("0"));
    CHECK(!bad.isValid(), "garbage → invalid");
    bad.addTime(5);
    CHECK(!bad.isValid(), "arithmetic on invalid is a no-op");

    Time frames(QStringLiteral("0"), QStringLiteral("0"), QStringLiteral("1"), QStringLiteral("12"), 25.0f);
    CHECK_EQ(frames.getMillis(), 1480, "12 frames at 25 fps = 480 ms");
    CHECK_EQ(frames.getSecondsFrames(25.0f), QStringLiteral("00:00:01:12"), "seconds:frames");
    CHECK_EQ(frames.getFrames(25.0f), QStringLiteral("37"), "absolute frames");

    Time fromFrame(QStringLiteral("250"), 25.0f);
    CHECK_EQ(fromFrame.getMillis(), 10000, "frame text at fps");

    Time clamp(-5.0);
    CHECK_EQ(clamp.getMillis(), 0, "negative clamps to 0");
    Time big(1e9);
    CHECK_EQ(big.getMillis(), Time::MAX_MILLI_TIME, "clamps to 24h");

    Time r(1.0004999);
    CHECK_EQ(r.getMillis(), 1000, "rounding down");
    Time r2(1.0005);
    CHECK_EQ(r2.getMillis(), 1001, "rounding half up");

    Time a(10.0), b(4.5);
    CHECK_EQ(a.difference(b).getMillis(), 5500, "difference");
    CHECK_EQ(b.difference(a).getMillis(), 0, "negative difference clamps to 0");
    a.recodeTime(5.0, 2.0);
    CHECK_EQ(a.getMillis(), 15000, "recode around centre");
}

static void testAlphaColor() {
    AlphaColor c(QColor(255, 0, 128), 180);
    CHECK_EQ(c.toString(), QStringLiteral("b4ff0080"), "AARRGGBB text");
    AlphaColor p(QStringLiteral("b4ff0080"));
    CHECK(p == c, "parse round trip");
    AlphaColor six(QStringLiteral("ff0080"));
    CHECK_EQ(six.alpha(), 0, "6-digit form has alpha 0");
    CHECK_EQ(six.red(), 255, "6-digit red");
    AlphaColor lowpad(QColor(0, 0, 1), 5);
    CHECK_EQ(lowpad.toString(), QStringLiteral("05000001"), "zero padded");
    CHECK_EQ(c.mixed(QColor(0, 0, 0), 255).red(), 255, "mixed opaque");
    CHECK_EQ(c.mixed(QColor(0, 0, 0), 0).red(), 0, "mixed transparent");
}

static void testStyleType() {
    CHECK(std::get<bool>(StyleType::init(StyleType::BOLD, QStringLiteral("1"))), "flag 1");
    CHECK(!std::get<bool>(StyleType::init(StyleType::BOLD, QStringLiteral("0"))), "flag 0");
    CHECK(std::get<bool>(StyleType::init(StyleType::BOLD, QStringLiteral("true"))), "flag true");
    CHECK(std::get<bool>(StyleType::init(StyleType::BOLD, QStringLiteral("-1"))), "flag -1 (ASS) is true");
    CHECK(!std::get<bool>(StyleType::init(StyleType::BOLD, QStringLiteral("yes"))), "flag garbage is false");
    CHECK_EQ(std::get<int>(StyleType::init(StyleType::FONTSIZE, QStringLiteral("x"))), 24, "bad int → default");
    CHECK_EQ(std::get<float>(StyleType::init(StyleType::SHADOWSIZE, QStringLiteral("1.5"))), 1.5f, "real parse");
    CHECK_EQ(StyleType::get(StyleType::SHADOWSIZE, StyleValue(2.0f)), QStringLiteral("2"), "real 2.0 → 2");
    CHECK_EQ(StyleType::get(StyleType::SHADOWSIZE, StyleValue(2.5f)), QStringLiteral("2.5"), "real 2.5");
    CHECK_EQ(styleValueToString(StyleValue(2.0f)), QStringLiteral("2.0"), "toString keeps .0");
    CHECK(std::get<Direction>(StyleType::init(StyleType::DIRECTION, QStringLiteral("TOPLEFT"))) == Direction::TOPLEFT, "direction");
    CHECK_EQ(std::get<int>(StyleType::init(StyleType::FONTSIZE, StyleValue(3.7f))), 3, "real → int truncation");
}

static void testSubStyle() {
    SubStyle s(QStringLiteral("Default"));
    const QString packed = s.getValues();
    CHECK_EQ(packed.count(QLatin1Char('|')), 20, "21 fields packed");
    CHECK(packed.startsWith(QStringLiteral("Arial|24|false|false|false|false|ffffffff|ffffff00|b4000000|b4404040|0|0.0|2.0|20|20|20|0.0|0.0|100|100|BOTTOM")), "packed default values");
    SubStyle t(QStringLiteral("Other"));
    t.set(StyleType::BOLD, StyleValue(true));
    t.set(StyleType::FONTSIZE, QStringLiteral("30"));
    SubStyle u(QStringLiteral("U"));
    u.setValues(t.getValues());
    CHECK(u.flag(StyleType::BOLD), "packed round trip bold");
    CHECK_EQ(u.fontSize(), 30, "packed round trip size");
    CHECK_EQ(u.getName(), QStringLiteral("U"), "setValues(text) keeps the name");
    u.setValues(QStringLiteral("garbage"));
    CHECK_EQ(u.fontSize(), 30, "malformed packed text ignored");

    SubStyleList list;
    CHECK_EQ(list.size(), 1, "fresh list has Default");
    CHECK(list.get(0)->isDefault(), "index 0 is default");
    auto a = std::make_shared<SubStyle>(QStringLiteral("Style"));
    list.add(a);
    auto b = std::make_shared<SubStyle>(QStringLiteral("Style"));
    list.add(b);
    b->setName(QStringLiteral("Style"), list);
    CHECK_EQ(b->getName(), QStringLiteral("Style2"), "unique name suffix (Java: existing counts as 1)");
    auto c = std::make_shared<SubStyle>(QStringLiteral("Style"));
    list.add(c);
    c->setName(QStringLiteral("Style"), list);
    CHECK_EQ(c->getName(), QStringLiteral("Style3"), "unique name bump");
    a->setName(QStringLiteral("Style"), list);
    CHECK_EQ(a->getName(), QStringLiteral("Style"), "own name stays");
    CHECK_EQ(list.findStyleIndex(QStringLiteral("Style3")), 3, "find by name");
    CHECK_EQ(list.findStyleIndex(QStringLiteral("nope")), 0, "missing → 0");

    SubStyleList copy(list);
    CHECK_EQ(copy.size(), 4, "deep copy size");
    CHECK(copy.get(1).get() != list.get(1).get(), "deep copy objects");
    CHECK_EQ(copy.get(3)->getName(), QStringLiteral("Style3"), "deep copy names");
}

static void testStyleover() {
    const QString txt = QStringLiteral("hello world");
    Styleover so(Styleover::Character, StyleType::BOLD);
    StyleValue basic(false);
    so.addEvent(StyleValue(true), 6, 11, basic, txt);
    CHECK_EQ(so.dump(), QStringLiteral("{(true,6)(false,11)}"), "suffix leaves a trailing end event (Java)");
    so.cleanupEvents(basic, txt);
    CHECK_EQ(so.size(), 1, "cleanup drops the event at the text end");
    CHECK_EQ(so.get(0).prev.position, 6, "bold starts at 6");
    auto v = so.getValue(6, 11, basic, txt);
    CHECK(v && std::get<bool>(*v), "value over the bold range");
    v = so.getValue(0, 5, basic, txt);
    CHECK(v && !std::get<bool>(*v), "value over the plain range");
    v = so.getValue(3, 8, basic, txt);
    CHECK(!v, "mixed range → nullopt");

    // Bold in the middle: two change points.
    Styleover mid(Styleover::Character, StyleType::BOLD);
    mid.addEvent(StyleValue(true), 2, 4, basic, txt);
    CHECK_EQ(mid.dump(), QStringLiteral("{(true,2)(false,4)}"), "middle range dump");
    // Extend by inserting text inside.
    mid.insertText(3, 2);
    CHECK_EQ(mid.dump(), QStringLiteral("{(true,2)(false,6)}"), "insert shifts later points");
    mid.removeText(1, 3, txt.length() + 2 - 3, basic, QStringLiteral("hlo world"));
    CHECK_EQ(mid.dump(), QStringLiteral("{(true,1)(false,3)}"), "remove collapses into the gap");

    // Applying the basic value removes the override.
    Styleover clear(Styleover::Character, StyleType::BOLD);
    clear.addEvent(StyleValue(true), 0, 11, basic, txt);
    CHECK_EQ(clear.dump(), QStringLiteral("{(true,0)(false,11)}"), "whole text bold");
    clear.addEvent(StyleValue(false), 0, 11, basic, txt);
    clear.cleanupEvents(basic, txt);
    CHECK_EQ(clear.size(), 0, "back to basic clears everything");

    // Full kind ignores the range.
    Styleover full(Styleover::Full, StyleType::DIRECTION);
    full.addEvent(StyleValue(Direction::TOP), 3, 5, StyleValue(Direction::BOTTOM), txt);
    CHECK_EQ(full.dump(), QStringLiteral("{(TOP,0)}"), "full override sits at 0");

    // Runs.
    int runs = 0;
    mid.forEachRun(basic, 9, [&](int, int, const StyleValue &) { ++runs; });
    CHECK_EQ(runs, 3, "three runs: plain, bold, plain");
}

static void testSubEntry() {
    SubEntry e(1.0, 3.0, QStringLiteral("Hello, world!\nSecond line here"));
    CHECK_EQ(e.getDurationTime().getMillis(), 2000, "duration");
    SubMetrics m = e.getMetrics();
    CHECK_EQ(m.lines, 2, "lines");
    CHECK_EQ(m.length, 26, "length counts non-space chars (other chars on)");
    CHECK_EQ(m.linelength, 14, "longest line");
    CHECK_EQ(m.fillpercent, 85, "fill percent 12/14");
    CHECK_EQ(SubEntry(1.0, 3.0, QString()).getMetrics().fillpercent, 100, "empty text: fill 100 (no imbalance)");
    CHECK_EQ(SubEntry(1.0, 3.0, QStringLiteral("Hello\n")).getMetrics().fillpercent, 0, "an empty second line is imbalance");
    CHECK(std::abs(m.cps - 13.0f) < 0.01f, "cps");
    CHECK_EQ(e.getData(7, 7), QStringLiteral("13.0"), "cps column");
    CHECK_EQ(e.getData(7, 6), QStringLiteral("780"), "cpm column");
    CHECK_EQ(e.getData(7, 8), QStringLiteral("Hello, world!|Second line here"), "text column");
    CHECK_EQ(e.getData(7, 0), QStringLiteral("8"), "row number column");
    CHECK_EQ(e.getData(7, 5), QStringLiteral("?Default"), "no style → ?Default");

    SubEntry zero(1.0, 1.0, QStringLiteral("x"));
    CHECK_EQ(zero.getData(0, 7), QStringLiteral("∞"), "zero duration cps");
    CHECK_EQ(zero.getData(0, 6), QStringLiteral("∞"), "zero duration cpm");
    CHECK(zero.isDurationSmall(), "small duration");

    e.updateQuality();
    CHECK_EQ(e.getMark(), 1, "compact rule: 26 chars on 2 lines would fit on one → error");
    SubEntry ok(1.0, 3.0, QStringLiteral("Hello, world!"));
    ok.updateQuality();
    CHECK_EQ(ok.getMark(), 0, "passes quality");
    SubEntry bad(1.0, 1.2, QStringLiteral("This is a very long line that exceeds forty two characters for sure"));
    bad.updateQuality();
    CHECK_EQ(bad.getMark(), 1, "fails quality → error colour");

    CHECK_EQ(e.getTextLineCount(), 2, "line count");
    CHECK_EQ(e.getFirstTextLine(), QStringLiteral("Hello, world!"), "first line");
    CHECK_EQ(e.getLastTextLine(), QStringLiteral("Second line here"), "last line");
    CHECK_EQ(e.getTextWithoutLineBreak(), QStringLiteral("Hello, world! Second line here"), "join lines");
    CHECK(e.removeLineBreak(0), "remove line break");
    CHECK_EQ(e.getText(), QStringLiteral("Hello, world! Second line here"), "text after removing break");
    SubEntry hy(0.0, 1.0, QStringLiteral("hyphen-\nated"));
    hy.removeLineBreak(0);
    CHECK_EQ(hy.getText(), QStringLiteral("hyphen-ated"), "hyphenated join has no space");
    CHECK_EQ(SubEntry::wordCount(QStringLiteral("a b  c")), 3, "word count");
    CHECK_EQ(SubEntry::wordCount(QString(QLatin1String(""))), 1, "empty word count is 1 (Java split)");

    SubEntry sp(0.0, 2.0, QStringLiteral("one two three four"));
    SubEntry second = sp.splitRecord();
    CHECK_EQ(sp.getText(), QStringLiteral("one two"), "split first half");
    CHECK_EQ(second.getText(), QStringLiteral("three four"), "split second half");
    CHECK_EQ(sp.getFinishTime().getMillis(), 1000, "split time first");
    CHECK_EQ(second.getStartTime().getMillis(), 1001, "split time second");
    sp.mergeRecord(second);
    CHECK_EQ(sp.getText(), QStringLiteral("one two\nthree four"), "merge text as a line");
    CHECK_EQ(sp.getStartTime().getMillis(), 0, "merge start");
    CHECK_EQ(sp.getFinishTime().getMillis(), 2000, "merge finish");

    // Inline overrides follow text edits and copies.
    auto style = std::make_shared<SubStyle>(QStringLiteral("Default"));
    SubEntry st(0.0, 1.0, QStringLiteral("abcdef"));
    st.setStyle(style);
    st.setOverStyle(StyleType::ITALIC, StyleValue(true), 2, 4);
    CHECK(st.hasStyleovers(), "override recorded");
    CHECK_EQ(st.getStyleover(StyleType::ITALIC)->dump(), QStringLiteral("{(true,2)(false,4)}"), "italic run");
    SubEntry copy(st);
    CHECK_EQ(copy.getStyleover(StyleType::ITALIC)->dump(), QStringLiteral("{(true,2)(false,4)}"), "copied run");
    st.setText(QStringLiteral("abc"));
    CHECK_EQ(st.getStyleover(StyleType::ITALIC)->dump(), QStringLiteral("{(true,2)}"), "truncation drops points past the end");
    st.setText(QStringLiteral("a"));
    CHECK(!st.hasStyleovers(), "all points gone → no overrides");
    auto val = copy.overValue(StyleType::ITALIC, 2, 4);
    CHECK(val && std::get<bool>(*val), "override value lookup");
}

static void testSubtitles() {
    Subtitles subs;
    auto e1 = std::make_shared<SubEntry>(5.0, 6.0, QStringLiteral("b"));
    auto e2 = std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("a"));
    auto e3 = std::make_shared<SubEntry>(9.0, 10.0, QStringLiteral("c"));
    subs.add(e1);
    subs.add(e2);
    subs.add(e3);
    CHECK(e1->getStyle() == subs.getStyleList().get(0), "add assigns Default");
    CHECK_EQ(subs.findSubEntry(5.5, false), 0, "find in time");
    CHECK_EQ(subs.findSubEntry(7.0, false), -1, "not in time");
    CHECK_EQ(subs.findSubEntry(7.0, true), 0, "fuzzy nearest start");
    subs.sort(0, 100);
    CHECK(subs.get(0) == e2 && subs.get(1) == e1 && subs.get(2) == e3, "sort all");
    auto e4 = std::make_shared<SubEntry>(3.0, 4.0, QStringLiteral("d"));
    CHECK_EQ(subs.addSorted(e4), 1, "addSorted position");
    subs.moveRow(0, 0, 3);
    CHECK(subs.get(3) == e2, "moveRow down");
    subs.moveRow(3, 3, 0);
    CHECK(subs.get(0) == e2, "moveRow up");
    CHECK_EQ(subs.getMaxTime(), 10.0, "max time");

    Subtitles copy(subs);
    CHECK_EQ(copy.size(), 4, "copy size");
    CHECK(copy.get(0).get() != subs.get(0).get(), "deep copied entries");
    CHECK(copy.get(0)->getStyle() == copy.getStyleList().get(0), "copied entry linked to copied style");

    Subtitles other;
    auto o1 = std::make_shared<SubEntry>(0.0, 1.0, QStringLiteral("z"));
    other.add(o1);
    SubEntryPtr sel = subs.joinSubs(Subtitles(), other, 2.0);
    CHECK(sel != nullptr, "join returns first appended");
    CHECK_EQ(sel->getStartTime().getMillis(), 2000, "join shifted by max+dt (empty s1 → 0 + 2)");
    CHECK(sel->getStyle() == subs.getStyleList().get(0), "joined entry uses Default");

    TotalSubMetrics tm = subs.getTotalMetrics();
    CHECK_EQ(tm.totallines, 5, "total lines");
}

static void testQualityAndStyles() {
    // Quality pass keeps manual marks; only the error mark is set/cleared.
    Options::setErrorColor(1);
    SubEntry good(Time(0.0), Time(3.0), QStringLiteral("Short"));
    good.setMark(3);
    good.updateQuality();
    CHECK_EQ(good.getMark(), 3, "manual mark kept on a passing entry");
    SubEntry bad(Time(0.0), Time(0.1), QStringLiteral("This text is far too long for a tenth of a second and fails every check"));
    bad.setMark(3);
    bad.updateQuality();
    CHECK_EQ(bad.getMark(), 1, "failing entry gets the error mark");
    bad.setFinishTime(Time(3.0));
    bad.setText(QStringLiteral("Fine"));
    bad.updateQuality();
    CHECK_EQ(bad.getMark(), 0, "error mark cleared when passing again");
    CHECK(!SubEntry(Time(0.0), Time(1.0), QString(QLatin1String(""))).isOneWord(), "empty text is not one word");
    // A loaded non-default style literally named like Default merges into it.
    Subtitles doc;
    Subtitles loaded;
    auto foo = std::make_shared<SubStyle>(QStringLiteral("Foo"));
    auto dup = std::make_shared<SubStyle>(QStringLiteral("Default"));
    loaded.getStyleList().add(foo);
    loaded.getStyleList().add(dup);
    doc.appendSubs(loaded, true);
    CHECK_EQ(doc.getStyleList().size(), 2, "Default, Foo");
    CHECK_EQ(doc.getStyleList().indexOfName(QStringLiteral("Missing")), -1, "indexOfName reports not found");
    // Sorting keeps the relative order of equal starts.
    Subtitles s;
    for (const char *t : {"A", "B", "C"}) s.add(std::make_shared<SubEntry>(Time(1.0), Time(2.0), QString::fromLatin1(t)));
    s.sort(0, 100);
    CHECK_EQ(s.get(0)->getText() + s.get(1)->getText() + s.get(2)->getText(), QStringLiteral("ABC"), "stable sort order");
}

static void testInvalidFps() {
    CHECK(!Time(QStringLiteral("250"), 0.0f).isValid(), "frames with fps 0 are invalid");
    CHECK(!Time(QStringLiteral("0"), QStringLiteral("0"), QStringLiteral("1"), QStringLiteral("5"), 0.0f).isValid(), "h:m:s:f with fps 0 is invalid");
    CHECK(Time(QStringLiteral("50"), 25.0f).getMillis() == 2000, "frames / fps");
}

static void testMarkNamesAndFonts() {
    for (int i = 0; i < SubEntry::MARK_COUNT; ++i) {
        CHECK(!SubEntry::markName(i).isEmpty(), "mark name non-empty");
        const QString key = QString::fromLatin1(SubEntry::MarkColorKeys[i]);
        CHECK(key == key.toLower() && !key.contains(QLatin1Char(' ')), "mark key lower-case without spaces");
    }
    const QStringList keys = {"none", "pink", "yellow", "cyan", "orange", "lightgreen"};
    for (int i = 0; i < SubEntry::MARK_COUNT; ++i)
        CHECK_EQ(QString::fromLatin1(SubEntry::MarkColorKeys[i]), keys.value(i), "mark key");
    CHECK(SubEntry::markName(SubEntry::MARK_COUNT - 1) != SubEntry::markName(0), "one name per key");
    CHECK_EQ(WebSafeFonts::renderFamily(QString()), WebSafeFonts::sansSerif(), "null family → sans-serif");
    CHECK_EQ(WebSafeFonts::renderFamily(QStringLiteral("   ")), WebSafeFonts::sansSerif(), "blank family → sans-serif");
    // Uninstalled names fall back by category (installed ones are kept, below).
    const struct { const char *name; QString expected; } fallbacks[] = {
        {"Arial", WebSafeFonts::sansSerif()},
        {"Times New Roman", WebSafeFonts::serif()},
        {"Courier New", WebSafeFonts::monospace()},
        {"Zztop Fake Mono 9000", WebSafeFonts::monospace()},
        {"Zztop Fake Serif 9000", WebSafeFonts::serif()},
        {"Zztop Fake Sans 9000", WebSafeFonts::sansSerif()},
        {"Zztop Completely Unknown 9000", WebSafeFonts::sansSerif()},
    };
    for (const auto &f : fallbacks) {
        const QString name = QString::fromLatin1(f.name);
        CHECK_EQ(WebSafeFonts::renderFamily(name), WebSafeFonts::isInstalled(name) ? name : f.expected, qPrintable(QStringLiteral("font fallback ") + name));
    }
    const QStringList installed = QFontDatabase::families();
    SubStyle::setFontNames(installed);   // as the app does at start
    if (!installed.isEmpty()) CHECK_EQ(WebSafeFonts::renderFamily(installed.first()), installed.first(), "installed family verbatim");
}

// Parser values of the wrong type are coerced, so the editors' std::get never throws.
static void testOverStyleCoercion() {
    SubEntry e(1.0, 2.0, QStringLiteral("abcdef"));
    e.addOverStyle(StyleType::BOLD, StyleValue(QStringLiteral("true")), 0);
    e.addOverStyle(StyleType::ITALIC, StyleValue(1), 2);
    e.addOverStyle(StyleType::PRIMARY, StyleValue(42), 3);
    e.addOverStyle(StyleType::FONTSIZE, StyleValue(QStringLiteral("30")), 4);
    CHECK(std::holds_alternative<bool>(e.getStyleover(StyleType::BOLD)->getEvent(0).value), "string → flag");
    CHECK(std::holds_alternative<bool>(e.getStyleover(StyleType::ITALIC)->getEvent(0).value), "int → flag");
    CHECK(std::holds_alternative<AlphaColor>(e.getStyleover(StyleType::PRIMARY)->getEvent(0).value), "int → colour default");
    CHECK(std::holds_alternative<int>(e.getStyleover(StyleType::FONTSIZE)->getEvent(0).value), "string → size");
}

// MicroDVD "{1}{1}fps": the declared frame rate is used and written back;
// a forced FPS (encoding bar reload) wins over it.
static void testMicroDvdFrameRate() {
    const QString text = QStringLiteral("{1}{1}23.976\n{24}{48}Hello\n");
    SubFile sf(QStringLiteral("/tmp/jubler-qt-test.sub"), SubFile::EXTENSION_GIVEN);
    Subtitles subs(sf);
    subs.populate(sf, text, false);
    CHECK_EQ(subs.size(), 1, "header line is not a subtitle");
    CHECK(std::abs(subs.getSubFile().getFPS() - 23.976f) < 0.001f, "declared FPS committed");
    CHECK(subs.getSubFile().hasFrameRateHeader(), "header remembered");
    const QString out = QStringLiteral("/tmp/jubler-qt-test-out-%1.sub").arg(QCoreApplication::applicationPid());
    SubFile target(out, SubFile::EXTENSION_GIVEN);
    target.setFormat(subs.getSubFile().getFormat());
    target.setFPS(subs.getSubFile().getFPS());
    CHECK(FileCommunicator::save(subs, target, nullptr).isNull(), "saved");
    QFile f(out);
    CHECK(f.open(QIODevice::ReadOnly), "read back");
    const QByteArray written = f.readAll();
    CHECK_EQ(QString::fromLatin1(written).remove(QLatin1Char(0x0d)).left(26), QStringLiteral("{1}{1}23.976\n{24}{48}Hello"), "header written back");
    f.remove();

    SubFile forced(QStringLiteral("/tmp/jubler-qt-test.sub"), SubFile::EXTENSION_GIVEN);
    forced.setFPS(25);
    forced.setFPSForced(true);
    Subtitles reloaded(forced);
    reloaded.populate(forced, text, false);
    CHECK_EQ(reloaded.getSubFile().getFPS(), 25.0f, "forced FPS wins");
    CHECK_EQ(reloaded.get(0)->getStartTime().getMillis(), 960, "times at the forced FPS");
}

int main(int argc, char **argv) {
    if (qEnvironmentVariableIsEmpty("QT_QPA_PLATFORM")) qputenv("QT_QPA_PLATFORM", "offscreen");
    QGuiApplication app(argc, argv);   // the font checks need the font database
    testInitPrefs();
    testFloatToString();
    testTime();
    testAlphaColor();
    testOverStyleCoercion();
    testStyleType();
    testSubStyle();
    testStyleover();
    testSubEntry();
    testSubtitles();
    testMicroDvdFrameRate();
    testQualityAndStyles();
    testInvalidFps();
    testMarkNamesAndFonts();
    return testFinish("test_model");
}
