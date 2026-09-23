/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// Tool algorithms and the command-line path (the Java CommandLineToolsTest /
// CommandLineIntegrationTest cases).
#include "TestSupport.h"

#include <QFile>
#include <cmath>

#include "core/cmdline/CommandLine.h"
#include "core/os/Escapes.h"
#include "core/subs/Subtitles.h"
#include "core/tools/CoreTools.h"
#include "core/tools/Tool.h"

static QString g_fixtures;

static void loadDefault(const QString &name, const QString &tag = QString()) {
    QString error;
    const QString file = g_fixtures + QLatin1Char('/') + name;
    const QString arg = tag.isEmpty() ? file : QLatin1Char(':') + tag + QLatin1Char(':') + file;
    CommandLine::loadSubtitles(arg, false, error);
    CHECK(error.isNull(), qPrintable("load " + name + ": " + error));
}

static QString run(const QString &toolAndParams) {
    const int colon = toolAndParams.indexOf(QLatin1Char(':'));
    auto tool = ToolRegistry::instance().findByCommandName(toolAndParams.left(colon));
    if (!tool) return QStringLiteral("no such tool");
    return tool->executeParamsLine(toolAndParams.mid(colon + 1), false);
}

static Subtitles &subs() { return *CommandLineContext::getSubtitles(QString()); }

static void testEscapes() {
    CHECK_EQ(Escapes::parseParametersWithEscaping(QStringLiteral("a=1:b=2\\:3:c=\\d")).join(QLatin1Char('|')), QStringLiteral("a=1|b=2:3|c=\\d"), "split keeps escapes other than \\:");
    CHECK_EQ(Escapes::unescapeParameterValue(QStringLiteral("12\\:30\\=x\\\\y\\d")), QStringLiteral("12:30=x\\y\\d"), "unescape values");
    CHECK_EQ(Escapes::unescapeJavaLenient(QStringLiteral("a\\tb\\u0041\\101\\q\\")), QStringLiteral("a\tbAA\\q\\"), "java lenient unescape");
}

static void testShiftAndRound() {
    loadDefault(QStringLiteral("simple.srt"));
    const double s0 = subs().get(0)->getStartTime().toSeconds();
    CHECK(run(QStringLiteral("shift:start=0:end=200:delta=1.5")).isNull(), "shift ok");
    CHECK(std::abs(subs().get(0)->getStartTime().toSeconds() - (s0 + 1.5)) < 0.001, "shifted by 1.5");
    const QString err = run(QStringLiteral("shift:start=0:end=200:delta=invalid"));
    CHECK(!err.isNull() && (err.contains(QStringLiteral("delta")) || err.contains(QStringLiteral("nvalid"))), "invalid delta reported");
    CHECK(run(QStringLiteral("shift:start=0:end=200:delta=-1.0")).isNull(), "negative shift ok");
    CHECK(std::abs(subs().get(1)->getStartTime().toSeconds() - (3.0 + 0.5)) < 0.001, "second entry net +0.5");
    CHECK(run(QStringLiteral("shift:start=0:end=200:delta=1.23456789")).isNull(), "odd shift");
    CHECK(run(QStringLiteral("round:start=0:end=200:decimals=1")).isNull(), "round ok");
    for (int i = 0; i < 5; ++i) {
        const double v = subs().get(i)->getStartTime().toSeconds();
        CHECK(std::abs(v - std::round(v * 10) / 10) < 1e-4, "rounded to 1 decimal");
    }
    QString e5 = run(QStringLiteral("round:start=0:end=200:decimals=5"));
    CHECK(e5.contains(QStringLiteral("3")) || e5.contains(QStringLiteral("nvalid")), "decimals=5 rejected");
    CHECK(!run(QStringLiteral("round:start=0:end=200:decimals=-1")).isNull(), "decimals=-1 rejected");
    CHECK(!run(QStringLiteral("shift:invalidparam=value")).isNull(), "unknown parameter rejected");
    CHECK(run(QStringLiteral("shift:invalidparam=value")).contains(QStringLiteral("Invalid parameter")), "unknown parameter message");
    CHECK(run(QStringLiteral("shift:start=0:end=30:delta=2.0")).isNull(), "partial range shift");
}

static void testRecode() {
    loadDefault(QStringLiteral("simple.srt"));
    const double d0 = subs().get(0)->getDurationTime().toSeconds();
    CHECK(run(QStringLiteral("recode:start=0:end=200:fromfps=25:tofps=23.976")).isNull(), "recode fps ok");
    CHECK(std::abs(subs().get(0)->getDurationTime().toSeconds() - d0 * 25 / 23.976) < 0.01, "duration scaled");
    loadDefault(QStringLiteral("simple.srt"));
    const double s1 = subs().get(1)->getStartTime().toSeconds();
    CHECK(run(QStringLiteral("recode:start=0:end=200:center=5:factor=1.1")).isNull(), "recode manual ok");
    CHECK(std::abs(subs().get(1)->getStartTime().toSeconds() - std::max(0.0, 5 + (s1 - 5) * 1.1)) < 0.01, "manual recode value");
    CHECK(!run(QStringLiteral("recode:start=0:end=200:center=5")).isNull(), "missing factor rejected");
}

static void testMarkStyleDelete() {
    loadDefault(QStringLiteral("simple.srt"));
    CHECK(run(QStringLiteral("mark:start=0:end=10:mark=pink")).isNull(), "mark pink ok");
    bool any = false;
    for (const SubEntryPtr &e : subs().entries())
        if (e->getStartTime().toSeconds() <= 10 && e->getMark() == 1) any = true;
    CHECK(any, "some entry marked pink");
    CHECK(run(QStringLiteral("mark:start=0:end=10:mark=3")).isNull(), "numeric mark index accepted");
    CHECK(!run(QStringLiteral("mark:start=0:end=10:mark=invalidcolor")).isNull(), "invalid colour rejected");
    CHECK(!run(QStringLiteral("mark:start=0:end=10:mark=10")).isNull(), "mark=10 rejected");
    for (const char *c : {"none", "pink", "yellow", "cyan", "orange", "lightgreen"})
        CHECK(run(QStringLiteral("mark:start=0:end=10:mark=%1").arg(QLatin1String(c))).isNull(), "colour key accepted");
    for (const char *c : {"red", "blue", "green", "purple", "black", "white"})
        CHECK(!run(QStringLiteral("mark:start=0:end=10:mark=%1").arg(QLatin1String(c))).isNull(), "non-key colour rejected");
    CHECK(run(QStringLiteral("shift:bymark=cyan:delta=1.0")).isNull(), "bymark filter");
    CHECK(run(QStringLiteral("shift:bymark=invalidcolor:delta=1.0")).contains(QStringLiteral("No valid subtitle filtering criteria")), "invalid bymark → no criteria");
    CHECK(run(QStringLiteral("style:start=0:end=10:style=Default")).isNull(), "style Default ok");
    CHECK(!run(QStringLiteral("style:start=0:end=10:style=Nope")).isNull(), "unknown style rejected");
    const int n = subs().size();
    CHECK(run(QStringLiteral("delete:start=0:end=5")).isNull(), "delete ok");
    CHECK(subs().size() < n, "entries deleted");
    for (const SubEntryPtr &e : subs().entries())
        CHECK(e->getStartTime().toSeconds() >= 5 || e->getFinishTime().toSeconds() < 0, "remaining start after the range");
    CHECK(run(QStringLiteral("shift:start=0:end=200:alsomark=yellow:delta=0")).isNull(), "alsomark");
    CHECK_EQ(subs().get(0)->getMark(), 2, "alsomark applied");
}

static void testFixer() {
    loadDefault(QStringLiteral("simple.srt"));
    CHECK(run(QStringLiteral("fix:start=0:end=60:mintime=1.0:maxtime=4.0")).isNull(), "fix ok");
    for (const SubEntryPtr &e : subs().entries())
        if (e->getStartTime().toSeconds() <= 60) {
            const double d = e->getDurationTime().toSeconds();
            CHECK(d >= 0.999 && d <= 4.001, "duration within [1,4]");
        }
    CHECK(run(QStringLiteral("fix:start=0:end=60:mintime=1.0:maxtime=4.0:overlap=distribute:gap=0.1")).isNull(), "fix distribute ok");
    CHECK(run(QStringLiteral("fix:start=0:end=60:overlap=divide:gap=0.1")).isNull(), "fix divide ok");
    CHECK(run(QStringLiteral("fix:start=0:end=60:overlap=shift:gap=0.1")).isNull(), "fix shift ok");
    CHECK(!run(QStringLiteral("fix:start=0:end=60:overlap=bogus")).isNull(), "bad overlap rejected");
    // Overlapping entries get separated by the shift model.
    loadDefault(QStringLiteral("simple.srt"));
    subs().get(0)->setFinishTime(Time(4.0));  // overlaps entry 1 (starts at 3)
    CHECK(run(QStringLiteral("fix:start=0:end=60:overlap=shift:gap=0.5")).isNull(), "fix overlap shift");
    CHECK(subs().get(1)->getStartTime().toSeconds() >= subs().get(0)->getFinishTime().toSeconds() + 0.499, "overlap resolved with gap");
}

static void testSyncJoinSplit() {
    loadDefault(QStringLiteral("simple.srt"));
    loadDefault(QStringLiteral("simple.srt"), QStringLiteral("source"));
    CHECK(run(QStringLiteral("sync:start=0:end=200:sourcesub=source:timestamp=true:offset=1")).isNull(), "sync ok");
    CHECK_EQ(subs().get(0)->getStartTime().getMillis(), CommandLineContext::getSubtitles(QStringLiteral("source"))->get(1)->getStartTime().getMillis(), "sync copied offset time");
    CHECK(run(QStringLiteral("sync:start=0:end=200:sourcesub=missing")).contains(QStringLiteral("locate")), "missing source");
    CHECK(run(QStringLiteral("sync:start=0:end=200:offset=1")).contains(QStringLiteral("source")), "no sourcesub");

    loadDefault(QStringLiteral("simple.srt"));
    loadDefault(QStringLiteral("simple.srt"), QStringLiteral("append"));
    const int n = subs().size();
    const double maxFinish = subs().getMaxTime();
    CHECK(run(QStringLiteral("join:append=append:gap=2.0")).isNull(), "join ok");
    CHECK(subs().size() > n, "join grew");
    CHECK(subs().get(n)->getStartTime().toSeconds() >= maxFinish + 1.5, "appended after the end + gap");
    CHECK(subs().get(n)->getFinishTime() > subs().get(n)->getStartTime(), "appended entry keeps its duration");

    loadDefault(QStringLiteral("simple.srt"));
    const int before = subs().size();
    CHECK(run(QStringLiteral("split:at=15.0:target=/tmp/jubler-qt-split2.srt")).isNull(), "split ok");
    CHECK(subs().size() <= before && subs().size() > 0, "split kept the first part");
    for (const SubEntryPtr &e : subs().entries())
        CHECK(e->getStartTime().toSeconds() < 15, "first part before 15");
    CHECK(QFile::exists(QStringLiteral("/tmp/jubler-qt-split2.srt")), "second part written to target");
    QFile::remove(QStringLiteral("/tmp/jubler-qt-split2.srt"));
    CHECK(!run(QStringLiteral("split:at=invalid")).isNull(), "split invalid at");
}

static void testTextTools() {
    loadDefault(QStringLiteral("simple.srt"));
    const int n = subs().size();
    CHECK(run(QStringLiteral("jointext:start=0:end=10")).isNull(), "jointext ok");
    CHECK(subs().size() <= n, "jointext reduced");
    loadDefault(QStringLiteral("simple.srt"));
    for (int i = 0; i < 3; ++i)
        subs().get(i)->setText(QStringLiteral("line one\nline two\nline three"));
    const int m = subs().size();
    CHECK(run(QStringLiteral("splittext:start=0:end=10")).isNull(), "splittext ok");
    CHECK(subs().size() >= m + 6, "splittext grew");
    CHECK(!subs().get(0)->getText().contains(QLatin1Char('\n')), "entry 0 single line");
    CHECK(subs().get(0)->getStartTime() < subs().get(1)->getStartTime(), "split parts ordered");

    loadDefault(QStringLiteral("simple.srt"));
    subs().get(0)->setText(QStringLiteral("Hello 123 World 456"));
    CHECK(run(QStringLiteral("regex:start=0:end=30:pattern=\\d+:replace=XXX")).isNull(), "regex ok");
    CHECK_EQ(subs().get(0)->getText(), QStringLiteral("Hello XXX World XXX"), "regex replaced digits");
    CHECK(run(QStringLiteral("regex:start=0:end=30:pattern=Hello:replace=Hi")).isNull(), "regex literal");
    CHECK_EQ(subs().get(0)->getText(), QStringLiteral("Hi XXX World XXX"), "regex literal replaced");
    CHECK(!run(QStringLiteral("regex:start=0:end=30:pattern=(:replace=x")).isNull(), "invalid pattern rejected");
    CHECK(!run(QStringLiteral("regex:start=0:end=30:pattern=(a):replace=$2")).isNull(), "missing group rejected (Java)");
    CHECK(!run(QStringLiteral("regex:start=0:end=30:pattern=(a):replace=$x")).isNull(), "illegal reference rejected (Java)");
    {
        const QRegularExpression many(QStringLiteral("(a)(b)?(c)?(d)?(e)?(f)?(g)?(h)?(i)?(j)?(k)?(l)?"));
        CHECK_EQ(RegExpReplace::replaceAll(QStringLiteral("a"), many, QStringLiteral("[$12]")), QStringLiteral("[]"), "unmatched group 12 inserts nothing");
    }
    CHECK_EQ(RegExpReplace::replaceAll(QStringLiteral("a1 b22"), QRegularExpression(QStringLiteral("(\\d+)")), QStringLiteral("[$1]")), QStringLiteral("a[1] b[22]"), "Java $1 group reference");
    CHECK_EQ(RegExpReplace::replaceAll(QStringLiteral("x"), QRegularExpression(QStringLiteral("(?<w>x)")), QStringLiteral("${w}$\\$")), QStringLiteral("x$$"), "named group and escapes");
    CHECK_EQ(RegExpReplace::replaceAll(QStringLiteral("abc"), QRegularExpression(QStringLiteral("z*")), QStringLiteral("-")), QStringLiteral("-a-b-c-"), "empty matches advance");
}

static void testAddAndSort() {
    loadDefault(QStringLiteral("simple.srt"));
    const int n = subs().size();
    CHECK(run(QStringLiteral("add:start=5.0:end=7.5:text=Hello World")).isNull(), "add ok");
    CHECK_EQ(subs().size(), n + 1, "add grew");
    int idx = -1;
    for (int i = 0; i < subs().size(); ++i)
        if (subs().get(i)->getText() == QLatin1String("Hello World")) idx = i;
    CHECK(idx > 0, "added entry found");
    CHECK(subs().get(idx - 1)->getStartTime() <= subs().get(idx)->getStartTime(), "chronological insert");
    CHECK_EQ(subs().get(idx)->getStartTime().getMillis(), 5000, "add start");
    CHECK(run(QStringLiteral("add:start=1.0:end=2.0:text=Line one\\nLine two:mark=yellow")).isNull(), "add with newline and mark");
    bool found = false;
    for (const SubEntryPtr &e : subs().entries())
        if (e->getText() == QStringLiteral("Line one\nLine two")) { found = true; CHECK_EQ(e->getMark(), 2, "mark yellow"); }
    CHECK(found, "newline text");
    CHECK(run(QStringLiteral("add:start=2.0:end=3.0:text=Time is 12\\:30\\=exactly")).isNull(), "add escaped");
    found = false;
    for (const SubEntryPtr &e : subs().entries())
        if (e->getText() == QStringLiteral("Time is 12:30=exactly")) found = true;
    CHECK(found, "escaped text");
    CHECK(!run(QStringLiteral("add:end=3.0:text=x")).isNull(), "missing start");
    CHECK(!run(QStringLiteral("add:start=3.0:text=x")).isNull(), "missing end");
    CHECK(!run(QStringLiteral("add:start=3.0:end=4.0")).isNull(), "missing text");
    CHECK(!run(QStringLiteral("add:start=5.0:end=4.0:text=x")).isNull(), "start>=end");
    CHECK(!run(QStringLiteral("add:start=-1:end=4.0:text=x")).isNull(), "negative start");
    CHECK(!run(QStringLiteral("add:start=abc:end=4.0:text=x")).isNull(), "non-numeric start");
    CHECK(!run(QStringLiteral("add:start=1:end=4.0:text=x:style=Nope")).isNull(), "unknown style");

    loadDefault(QStringLiteral("simple.srt"));
    auto first = subs().get(0);
    subs().moveRow(0, 0, 3);
    CHECK(run(QStringLiteral("sort:")).isNull(), "sort ok");
    CHECK(subs().get(0) == first, "sorted back");
    CHECK(!run(QStringLiteral("sort:start=5:end=2")).isNull(), "sort bad range");
}

static void testCommandLineDriver() {
    QString outS, errS;
    QTextStream out(&outS), err(&errS);
    const QString src = g_fixtures + QStringLiteral("/simple.srt");
    const QString dst = QStringLiteral("/tmp/jubler-qt-cli-out.vtt");
    CommandLineContext::clear();
    int rc = CommandLine::run({QStringLiteral("--load"), src, QStringLiteral("-x"), QStringLiteral("shift:start=0:end=100:delta=1"), QStringLiteral("--save"), dst}, out, err);
    CHECK_EQ(rc, 0, "cli batch ok");
    CHECK(QFile::exists(dst), "cli saved output");
    CHECK(readTextFile(dst).startsWith(QStringLiteral("WEBVTT")), "saved as WebVTT by extension");
    QFile::remove(dst);
    outS.clear(); errS.clear();
    CommandLineContext::clear();
    rc = CommandLine::run({QStringLiteral("--save"), dst}, out, err);
    CHECK_EQ(rc, 1, "save without load fails");
    CHECK(errS.contains(QStringLiteral("Use --load first")), "save error text");
    outS.clear(); errS.clear();
    rc = CommandLine::run({QStringLiteral("--load"), src, QStringLiteral("-x"), QStringLiteral("nosuch:a=b")}, out, err);
    CHECK_EQ(rc, 1, "unknown tool fails");
    CHECK(errS.contains(QStringLiteral("not found")) && errS.contains(QStringLiteral("shift")), "unknown tool lists tools");
    outS.clear(); errS.clear();
    rc = CommandLine::run({QStringLiteral("--list-tools")}, out, err);
    CHECK(rc == 0 && outS.contains(QStringLiteral("Available tools:\n ")) && outS.contains(QStringLiteral(" fix")) && outS.section(QLatin1Char('\n'), 1, 1).contains(QStringLiteral(" shift")) && outS.contains(QStringLiteral("--help-tool")), "list tools");
    outS.clear(); errS.clear();
    rc = CommandLine::run({QStringLiteral("--help-tool"), QStringLiteral("shift")}, out, err);
    CHECK(rc == 0 && outS.contains(QStringLiteral("delta=seconds")), "help tool");
    outS.clear(); errS.clear();
    rc = CommandLine::run({QStringLiteral("--load"), QStringLiteral(":a:") + src, QStringLiteral("--load"), src, QStringLiteral("--swap"), QStringLiteral("a"), QStringLiteral("--remove"), QStringLiteral("a"), QStringLiteral("--remove"), QStringLiteral("a")}, out, err);
    CHECK_EQ(rc, 1, "second remove fails");
    CHECK(errS.contains(QStringLiteral("to remove")), "remove error text");
    outS.clear(); errS.clear();
    rc = CommandLine::run({QStringLiteral("stray")}, out, err);
    CHECK(rc == 1 && errS.contains(QStringLiteral("Unknown arguments: stray")), "stray argument");
    CHECK(CommandLine::isCommandLineInvocation({QStringLiteral("-l"), QStringLiteral("x")}), "short alias selects CLI mode");
    CHECK(!CommandLine::isCommandLineInvocation({QStringLiteral("file.srt")}), "plain file is GUI mode");
}

// The Java parameter-variation and workflow tests (TOOL-256/266/267/268/277/279).
static void testParameterVariations() {
    loadDefault(QStringLiteral("simple.srt"));
    CHECK_EQ(subs().size(), 17, "simple.srt has 17 entries");
    QList<double> starts, ends;
    for (int i = 0; i < 10; ++i) { starts.append(subs().get(i)->getStartTime().toSeconds()); ends.append(subs().get(i)->getFinishTime().toSeconds()); }
    CHECK(run(QStringLiteral("shift:start=0:end=200:delta=2.5")).isNull(), "shift 2.5");
    CHECK(std::abs(subs().get(0)->getFinishTime().toSeconds() - (ends[0] + 2.5)) < 0.001, "first end moved by 2.5");
    for (int i = 0; i < 10; ++i) CHECK(std::abs(subs().get(i)->getStartTime().toSeconds() - (starts[i] + 2.5)) < 0.001, "starts moved by 2.5");
    loadDefault(QStringLiteral("simple.srt"));
    const double late = subs().get(16)->getStartTime().toSeconds();
    CHECK(run(QStringLiteral("shift:start=0:end=30:delta=2.0")).isNull(), "partial shift");
    CHECK(std::abs(subs().get(16)->getStartTime().toSeconds() - late) < 0.001, "entries after 30 s untouched");
    for (const char *d : {"0.1", "5.0", "-2.5", "0.001", "30.0"}) {
        loadDefault(QStringLiteral("simple.srt"));
        CHECK(run(QStringLiteral("shift:start=0:end=200:delta=") + QLatin1String(d)).isNull(), "delta variation ok");
    }
    for (const char *d : {"abc", "1.2.3", ""}) CHECK(!run(QStringLiteral("shift:start=0:end=200:delta=") + QLatin1String(d)).isNull(), "non-numeric delta rejected");
    for (int dec : {0, 2, 3}) { loadDefault(QStringLiteral("simple.srt")); CHECK(run(QStringLiteral("round:start=0:end=200:decimals=%1").arg(dec)).isNull(), "decimals accepted"); }
    for (int dec : {-1, 4, 10}) CHECK(!run(QStringLiteral("round:start=0:end=200:decimals=%1").arg(dec)).isNull(), "decimals rejected");
    const char *fpsPairs[][2] = {{"25", "29.97"}, {"29.97", "25"}, {"23.976", "25"}, {"25", "23.976"}, {"30", "25"}, {"24", "30"}};
    for (const auto &pr : fpsPairs) { loadDefault(QStringLiteral("simple.srt")); CHECK(run(QStringLiteral("recode:start=0:end=200:fromfps=%1:tofps=%2").arg(QLatin1String(pr[0]), QLatin1String(pr[1]))).isNull(), "fps recode ok"); }
    const char *manual[][2] = {{"0", "1.1"}, {"10", "0.9"}, {"5.5", "1.0"}, {"0", "2.0"}};
    for (const auto &pr : manual) { loadDefault(QStringLiteral("simple.srt")); CHECK(run(QStringLiteral("recode:start=0:end=200:center=%1:factor=%2").arg(QLatin1String(pr[0]), QLatin1String(pr[1]))).isNull(), "manual recode ok"); }
    const char *fixer[][2] = {{"1.0", "5.0"}, {"0.5", "10.0"}, {"2.0", "3.0"}, {"0.1", "20.0"}};
    for (const auto &pr : fixer) {
        loadDefault(QStringLiteral("simple.srt"));
        CHECK(run(QStringLiteral("fix:start=0:end=200:mintime=%1:maxtime=%2").arg(QLatin1String(pr[0]), QLatin1String(pr[1]))).isNull(), "fixer min/max ok");
        for (const SubEntryPtr &e : subs().entries()) {
            const double d = e->getFinishTime().toSeconds() - e->getStartTime().toSeconds();
            CHECK(d >= QString::fromLatin1(pr[0]).toDouble() - 0.001 && d <= QString::fromLatin1(pr[1]).toDouble() + 0.001, "duration within bounds");
        }
    }
    for (const char *cps : {"10", "15", "20", "25"}) { loadDefault(QStringLiteral("simple.srt")); CHECK(run(QStringLiteral("fix:start=0:end=200:mincps=") + QLatin1String(cps)).isNull(), "mincps variation ok"); }
    // Real characters per second: maxcps bounds the duration from below
    // (characters / maxcps), mincps from above (characters / mincps).
    for (const auto &[param, isMin] : {std::pair{"maxcps=20", true}, std::pair{"mincps=40", false}}) {
        loadDefault(QStringLiteral("simple.srt"));
        CHECK(run(QStringLiteral("fix:start=0:end=200:") + QLatin1String(param)).isNull(), "cps fix runs");
        const double rate = QString::fromLatin1(param).section(QLatin1Char('='), 1).toDouble();
        for (const SubEntryPtr &e : subs().entries()) {
            const double d = e->getFinishTime().toSeconds() - e->getStartTime().toSeconds();
            const double bound = e->getMetrics().length / rate;
            CHECK(isMin ? d >= bound - 0.002 : d <= bound + 0.002, isMin ? "at least characters / maxcps" : "at most characters / mincps");
        }
    }
    {
        // 20 counted characters at maxcps=10 → at least 2 s.
        loadDefault(QStringLiteral("simple.srt"));
        SubEntryPtr e = subs().get(0);
        e->setText(QStringLiteral("abcdefghij\nklmnopqrst"));
        e->getFinishTime().setTime(e->getStartTime().toSeconds() + 0.5);
        CHECK(run(QStringLiteral("fix:start=0:end=200:maxcps=10")).isNull(), "fix maxcps=10");
        CHECK(std::abs(e->getFinishTime().toSeconds() - e->getStartTime().toSeconds() - 2.0) < 0.002, "20 characters at 10 cps = 2 s");
    }
    const char *ranges[][2] = {{"0", "10"}, {"5.5", "15.7"}, {"0", "1000"}, {"10", "10.1"}};
    for (const auto &pr : ranges) { loadDefault(QStringLiteral("simple.srt")); CHECK(run(QStringLiteral("shift:start=%1:end=%2:delta=1").arg(QLatin1String(pr[0]), QLatin1String(pr[1]))).isNull(), "time range variation ok"); }
}

static void testWorkflow() {
    loadDefault(QStringLiteral("simple.srt"));
    loadDefault(QStringLiteral("simple.srt"), QStringLiteral("other"));
    CHECK(run(QStringLiteral("mark:start=0:end=30:mark=pink")).isNull(), "workflow mark");
    CHECK(run(QStringLiteral("shift:start=0:end=200:delta=1.0")).isNull(), "workflow shift");
    CHECK(run(QStringLiteral("round:start=0:end=200:decimals=2")).isNull(), "workflow round");
    CHECK(run(QStringLiteral("fix:start=0:end=200:mintime=1.0:maxtime=6.0")).isNull(), "workflow fix");
    CHECK(!run(QStringLiteral("shift:start=0:end=200:delta=bogus")).isNull(), "invalid step fails alone");
    CHECK(run(QStringLiteral("recode:start=0:end=200:fromfps=25:tofps=29.97")).isNull(), "workflow recode after a failed step");
    CHECK(run(QStringLiteral("sync:start=0:end=200:sourcesub=other:timestamp=true")).isNull(), "workflow sync");
    CHECK(run(QStringLiteral("join:gap=1.0:append=other")).isNull(), "workflow join");
    CHECK(run(QStringLiteral("split:at=100")).isNull(), "workflow split");
    CHECK(run(QStringLiteral("delete:start=0:end=5")).isNull(), "workflow delete");
    CHECK(subs().size() > 0, "document survives the workflow");
}

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    testInitPrefs();
    g_fixtures = argc > 1 ? QString::fromLocal8Bit(argv[1]) : QStringLiteral("tests/fixtures");
    testEscapes();
    testShiftAndRound();
    testRecode();
    testMarkStyleDelete();
    testFixer();
    testSyncJoinSplit();
    testTextTools();
    testAddAndSort();
    testCommandLineDriver();
    testParameterVariations();
    testWorkflow();
    return testFinish("test_tools");
}
