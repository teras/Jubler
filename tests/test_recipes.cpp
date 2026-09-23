/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// External tool recipes: JSON, key validation and command-line building
// (the cases of the Java RecipeTest).
#include "TestSupport.h"

#include "core/externals/Recipe.h"
#include "core/externals/RecipeExecutor.h"
#include "core/options/Prefs.h"

#include <QElapsedTimer>
#include <QEventLoop>
#include "core/subs/SubFile.h"
#include "core/formats/SubFormat.h"
#include <QFile>
#include <QTemporaryDir>
#include <QThread>
#include <QTimer>
#ifdef Q_OS_UNIX
#include <csignal>
#endif

static Recipe sample() {
    Recipe r(QStringLiteral("Whisper"));
    r.path = QStringLiteral("/opt/whisper cli/whisper");
    r.outputMode = OutputMode::PATCH_TIMING;
    RecipeParam model;
    model.key = QStringLiteral("model");
    model.type = RecipeParam::Type::COMBOBOX;
    model.choices = QStringLiteral("tiny|base|small");
    model.defaultValue = QStringLiteral("base");
    RecipeParam lang;
    lang.key = QStringLiteral("lang");
    r.params = {model, lang};
    return r;
}

static void testJsonRoundTrip() {
    const Recipe r = sample();
    const auto back = Recipe::fromJsonString(r.toJsonString(false));
    CHECK(back.has_value(), "parses");
    CHECK_EQ(back->name, QStringLiteral("Whisper"), "name");
    CHECK_EQ(back->path, r.path, "path");
    CHECK(back->outputMode == OutputMode::PATCH_TIMING, "output mode");
    CHECK(!back->outputFolder, "outputFolder false");
    CHECK_EQ(int(back->params.size()), 2, "param count");
    CHECK_EQ(back->params[0].key, QStringLiteral("model"), "param key");
    CHECK_EQ(back->params[0].defaultValue, QStringLiteral("base"), "param default");
    CHECK_EQ(back->params[0].choices, QStringLiteral("tiny|base|small"), "choices");
    CHECK_EQ(back->params[0].getChoiceList().join(QLatin1Char(',')), QStringLiteral("tiny,base,small"), "choice list");

    Recipe s(QStringLiteral("S"));
    RecipeParam secret;
    secret.key = QStringLiteral("token");
    secret.type = RecipeParam::Type::SECRET;
    secret.defaultValue = QStringLiteral("ENCRYPTED");
    s.params = {secret};
    CHECK_EQ(Recipe::fromJsonString(s.toJsonString(false))->params[0].defaultValue, QStringLiteral("ENCRYPTED"), "secret kept locally");
    const auto shared = Recipe::fromJsonString(s.toJsonString(true));
    CHECK_EQ(int(shared->params.size()), 1, "secret param kept when sharing");
    CHECK_EQ(shared->params[0].defaultValue, QString(), "secret value never shared");

    Recipe f(QStringLiteral("F"));
    f.outputFolder = true;
    f.description = QStringLiteral("desc");
    f.url = QStringLiteral("https://x");
    for (bool sharing : {false, true}) {
        const auto b = Recipe::fromJsonString(f.toJsonString(sharing));
        CHECK(b->outputFolder, "outputFolder round trip");
        CHECK_EQ(b->description, QStringLiteral("desc"), "description");
        CHECK_EQ(b->url, QStringLiteral("https://x"), "url");
    }
}

static void testKeyValidation() {
    const QSet<QString> existing{QStringLiteral("model")};
    CHECK(!RecipeParam::validateKey(QStringLiteral("i"), existing).isNull(), "single letter rejected");
    CHECK(!RecipeParam::validateKey(QStringLiteral("a"), existing).isNull(), "single letter rejected");
    CHECK(RecipeParam::validateKey(QStringLiteral("lang"), existing).isNull(), "lang ok");
    CHECK(!RecipeParam::validateKey(QStringLiteral("mo del"), existing).isNull(), "space rejected");
    CHECK(!RecipeParam::validateKey(QStringLiteral("model"), existing).isNull(), "duplicate rejected");
    CHECK(RecipeParam::validateKey(QStringLiteral("model2"), existing).isNull(), "model2 ok");
}

static void testCommandLine() {
    const Recipe r = sample();
    QMap<QString, QString> values{{QStringLiteral("model"), QStringLiteral("base")}, {QStringLiteral("lang"), QString(QLatin1String(""))}};
    QMap<QChar, QString> singles{{QLatin1Char('x'), QStringLiteral("/opt/whisper cli/whisper")}, {QLatin1Char('a'), QStringLiteral("/tmp/au dio.wav")}, {QLatin1Char('o'), QStringLiteral("/tmp/out.srt")}};
    const QStringList args = RecipeExecutor::buildCommandLine(QStringLiteral("%x -m %model -l %lang -f %a -o %o"), r, values, singles);
    const QStringList expected{QStringLiteral("/opt/whisper cli/whisper"), QStringLiteral("-m"), QStringLiteral("base"), QStringLiteral("-l"), QString(QLatin1String("")), QStringLiteral("-f"), QStringLiteral("/tmp/au dio.wav"), QStringLiteral("-o"), QStringLiteral("/tmp/out.srt")};
    CHECK_EQ(args.join(QLatin1Char('|')), expected.join(QLatin1Char('|')), "values never split, empty kept");

    Recipe mkv(QStringLiteral("mkv"));
    RecipeParam track;
    track.key = QStringLiteral("track");
    track.type = RecipeParam::Type::VIDEO_SUBTITLE;
    mkv.params = {track};
    const QStringList a2 = RecipeExecutor::buildCommandLine(QStringLiteral("%x %v tracks %track:%o"), mkv, {{QStringLiteral("track"), QStringLiteral("3")}},
                                                            {{QLatin1Char('x'), QStringLiteral("mkvextract")}, {QLatin1Char('v'), QStringLiteral("/movies/a film.mkv")}, {QLatin1Char('o'), QStringLiteral("/tmp/out.srt")}});
    CHECK_EQ(a2.join(QLatin1Char('|')), QStringLiteral("mkvextract|/movies/a film.mkv|tracks|3:/tmp/out.srt"), "embedded placeholder stays one argument");

    Recipe w(QStringLiteral("w"));
    RecipeParam model;
    model.key = QStringLiteral("model");
    model.type = RecipeParam::Type::PATH;
    w.params = {model};
    const QStringList a3 = RecipeExecutor::buildCommandLine(QStringLiteral("%x -m %model -f %w -osrt -of %o/subs"), w, {{QStringLiteral("model"), QStringLiteral("/models/ggml-small.bin")}},
                                                            {{QLatin1Char('x'), QStringLiteral("whisper-cli")}, {QLatin1Char('w'), QStringLiteral("/tmp/run/A Movie.wav")}, {QLatin1Char('o'), QStringLiteral("/tmp/out")}});
    CHECK_EQ(a3.join(QLatin1Char('|')), QStringLiteral("whisper-cli|-m|/models/ggml-small.bin|-f|/tmp/run/A Movie.wav|-osrt|-of|/tmp/out/subs"), "wav placeholder stays one argument");

    Recipe c(QStringLiteral("c"));
    RecipeParam flags;
    flags.key = QStringLiteral("flags");
    flags.type = RecipeParam::Type::CHECKBOX;
    c.params = {flags};
    auto run = [&](const QString &v) { return RecipeExecutor::buildCommandLine(QStringLiteral("%x %flags end"), c, {{QStringLiteral("flags"), v}}, {{QLatin1Char('x'), QStringLiteral("t")}}).join(QLatin1Char('|')); };
    CHECK_EQ(run(QStringLiteral("--foo --bar")), QStringLiteral("t|--foo|--bar|end"), "checkbox value splits into flags");
    CHECK_EQ(run(QString(QLatin1String(""))), QStringLiteral("t|end"), "unchecked adds nothing");
    CHECK_EQ(run(QStringLiteral("--name \"John Doe\" --flag")), QStringLiteral("t|--name|John Doe|--flag|end"), "quoted checkbox value");
    CHECK_EQ(RecipeExecutor::buildCommandLine(QStringLiteral("%x %unknown %z"), c, {}, {{QLatin1Char('x'), QStringLiteral("t")}}).join(QLatin1Char('|')), QStringLiteral("t|%unknown|%z"), "unknown placeholders stay verbatim");
}

static void testOutputModes() {
    CHECK(patchText(OutputMode::PATCH_BOTH) && patchTiming(OutputMode::PATCH_BOTH), "both");
    CHECK(patchText(OutputMode::PATCH_TEXT) && !patchTiming(OutputMode::PATCH_TEXT), "text only");
    CHECK(!isPatch(OutputMode::REPLACE), "replace is not patch");
    CHECK(isPatch(OutputMode::PATCH_BOTH), "both is patch");
    CHECK(outputModeFromName(QStringLiteral("nonsense")) == OutputMode::REPLACE, "default mode");
}

static void testUnparsableSlotsKept() {
    Prefs::set(QStringLiteral("external.recipes.recipe1"), sample().toJsonString(false));
    Prefs::set(QStringLiteral("external.recipes.recipe2"), QStringLiteral("{not json"));
    Prefs::remove(QStringLiteral("external.recipes.recipe3"));
    Recipes::load();
    CHECK_EQ(Recipes::getList().size(), qsizetype(1), "parsable recipe loaded");
    Recipes::getList().append(Recipe(QStringLiteral("Added")));
    Recipes::save();
    Recipes::load();
    CHECK_EQ(Recipes::getList().size(), qsizetype(2), "both parsable recipes back");
    CHECK_EQ(Prefs::getString(QStringLiteral("external.recipes.recipe3"), QString()), QStringLiteral("{not json"), "unparsable slot kept verbatim");
    CHECK(!Prefs::contains(QStringLiteral("external.recipes.recipe4")), "terminator after the kept slot");
}

#ifdef Q_OS_UNIX
static QString writeScript(const QTemporaryDir &dir, const QString &body) {
    const QString path = dir.filePath(QStringLiteral("tool.sh"));
    QFile f(path);
    CHECK(f.open(QIODevice::WriteOnly), "script written");
    f.write(("#!/bin/sh\n" + body).toUtf8());
    f.close();
    f.setPermissions(f.permissions() | QFileDevice::ExeOwner);
    return path;
}

// Runs the recipe to its end (or calls `during` once it is running).
static std::pair<bool, QString> runRecipe(const RecipeRun &run, const std::function<void(RecipeExecutor &)> &during = {}) {
    RecipeExecutor ex;
    bool success = false, done = false;
    QString message;
    QEventLoop loop;
    QObject::connect(&ex, &RecipeExecutor::finished, &loop, [&](bool ok, const QString &msg) {
        success = ok;
        message = msg;
        done = true;
        loop.quit();
    });
    QTimer::singleShot(0, &loop, [&]() { ex.start(run); });
    if (during) QTimer::singleShot(0, &loop, [&]() { during(ex); });
    QTimer::singleShot(20000, &loop, &QEventLoop::quit);
    loop.exec();
    CHECK(done, "executor finished");
    return {success, message};
}

static void testExecutorPatchSizeMismatch() {
    QTemporaryDir dir;
    Recipe r(QStringLiteral("Short"));
    r.path = writeScript(dir, QStringLiteral("printf '1\\n00:00:01,000 --> 00:00:02,000\\nA\\n' > \"$1\"\n"));
    r.command = QStringLiteral("%x %o");
    r.outputMode = OutputMode::PATCH_TEXT;
    Subtitles current;
    current.add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("one")));
    current.add(std::make_shared<SubEntry>(3.0, 4.0, QStringLiteral("two")));
    RecipeRun run;
    run.recipe = r;
    run.current = &current;
    const auto [ok, msg] = runRecipe(run);
    CHECK(!ok, "fewer output entries than sent is a failure");
    CHECK(msg.contains(QStringLiteral("cannot patch")), "mismatch reason reported");
}

static void testExecutorWav() {
    QTemporaryDir dir;
    Recipe r(QStringLiteral("Wav"));
    // The tool sees the extracted WAV and turns its first 4 bytes into a subtitle.
    r.path = writeScript(dir, QStringLiteral("printf '1\\n00:00:01,000 --> 00:00:02,000\\n%s\\n' \"$(head -c 4 \"$1\")\" > \"$2\"\n"));
    r.command = QStringLiteral("%x %w %o");
    Subtitles current;
    current.add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("one")));
    RecipeRun run;
    run.recipe = r;
    run.current = &current;

    auto [ok, msg] = runRecipe(run);
    CHECK(!ok && msg.contains(QStringLiteral("%w")), "no media: %w fails");

    run.makeWav = [](const QString &dst, const std::atomic<bool> &, const std::function<void(float)> &progress, QString *) {
        progress(0.5f);
        QFile f(dst);
        return f.open(QIODevice::WriteOnly) && f.write("RIFF") == 4;
    };
    std::tie(ok, msg) = runRecipe(run);
    CHECK(ok, "the tool runs after the extraction");

    run.makeWav = [](const QString &, const std::atomic<bool> &cancel, const std::function<void(float)> &, QString *error) {
        while (!cancel) std::this_thread::sleep_for(std::chrono::milliseconds(10));
        *error = QStringLiteral("Cancelled");
        return false;
    };
    std::tie(ok, msg) = runRecipe(run, [](RecipeExecutor &ex) { QTimer::singleShot(100, &ex, [&ex]() { ex.cancel(); }); });
    CHECK(!ok && msg == QStringLiteral("Cancelled."), "cancel during the extraction");
}

// A frame-based wire format round-trips at the document's frame rate.
static void testExecutorFrameFormat() {
    QTemporaryDir dir;
    Recipe r(QStringLiteral("Copy"));
    r.path = writeScript(dir, QStringLiteral("cp \"$1\" \"$2\"\n"));
    r.command = QStringLiteral("%x %i %o");
    r.setFormat(Availabilities::formats().findFromName(QStringLiteral("MicroDVD")));
    SubFile sf(QStringLiteral("/tmp/fps.sub"), SubFile::EXTENSION_GIVEN);
    sf.setFPS(23.976f);
    Subtitles current(sf);
    current.add(std::make_shared<SubEntry>(1.0, 4.5, QStringLiteral("one")));
    RecipeRun run;
    run.recipe = r;
    run.current = &current;
    RecipeExecutor ex;
    std::shared_ptr<Subtitles> got;
    QObject::connect(&ex, &RecipeExecutor::resultReady, [&](std::shared_ptr<Subtitles> res, const RecipeRun &) { got = res; });
    const auto [ok, msg] = std::pair<bool, QString>{};
    Q_UNUSED(ok); Q_UNUSED(msg);
    QEventLoop loop;
    QObject::connect(&ex, &RecipeExecutor::finished, &loop, &QEventLoop::quit);
    QTimer::singleShot(0, &loop, [&]() { ex.start(run); });
    QTimer::singleShot(20000, &loop, &QEventLoop::quit);
    loop.exec();
    CHECK(got && got->size() == 1, "frame-format output read");
    if (got && got->size() == 1) {
        CHECK(std::abs(got->get(0)->getStartTime().toSeconds() - 1.0) < 0.05, "start kept at the document fps");
        CHECK(std::abs(got->get(0)->getFinishTime().toSeconds() - 4.5) < 0.05, "end kept at the document fps");
    }
}

static void testExecutorCancelKillsTree() {
    QTemporaryDir dir;
    const QString pidFile = dir.filePath(QStringLiteral("child.pid"));
    Recipe r(QStringLiteral("Slow"));
    r.path = writeScript(dir, QStringLiteral("sleep 60 &\necho $! > '%1'\nwait\n").arg(pidFile));
    r.command = QStringLiteral("%x");
    Subtitles current;
    current.add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("one")));
    RecipeRun run;
    run.recipe = r;
    run.current = &current;
    const auto [ok, msg] = runRecipe(run, [&](RecipeExecutor &ex) {
        QElapsedTimer t;
        t.start();
        // Until the pid line is complete (the file exists before echo writes).
        auto written = [&]() { QFile f(pidFile); return f.open(QIODevice::ReadOnly) && f.readAll().endsWith('\n'); };
        while (!written() && t.elapsed() < 5000) QCoreApplication::processEvents(QEventLoop::AllEvents, 50);
        ex.cancel();
    });
    CHECK(!ok && msg == QStringLiteral("Cancelled."), "cancelled run fails");
    QFile f(pidFile);
    CHECK(f.open(QIODevice::ReadOnly), "grandchild started");
    const QByteArray pidLine = f.readAll();
    CHECK(pidLine.endsWith('\n'), "grandchild pid written");
    const pid_t child = pid_t(QString::fromLatin1(pidLine).trimmed().toInt());
    bool gone = false;
    for (int i = 0; i < 50 && !gone; ++i) {
        // A zombie waiting for its (sub)reaper has terminated too.
        QFile stat(QStringLiteral("/proc/%1/stat").arg(child));
        const bool zombie = stat.open(QIODevice::ReadOnly) && stat.readAll().contains(") Z ");
        gone = child > 0 && (::kill(child, 0) != 0 || zombie);
        if (!gone) QThread::msleep(100);
    }
    CHECK(gone, "grandchild killed with the tool");
}
#endif

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    testInitPrefs();
    testJsonRoundTrip();
    testKeyValidation();
    testCommandLine();
    testOutputModes();
    testUnparsableSlotsKept();
#ifdef Q_OS_UNIX
    testExecutorPatchSizeMismatch();
    testExecutorWav();
    testExecutorFrameFormat();
    testExecutorCancelKillsTree();
#endif
    if (g_failures) std::fprintf(stderr, "%d failure(s)\n", g_failures);
    return g_failures ? 1 : 0;
}
