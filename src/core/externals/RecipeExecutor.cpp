/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/externals/RecipeExecutor.h"

#include <QPointer>
#include <QCoreApplication>
#include <QDir>
#include <QFile>
#include <QFileInfo>
#include <QProcessEnvironment>
#include <QRegularExpression>
#include <QTemporaryDir>
#include <QTimer>
#include <QUuid>

#ifdef Q_OS_UNIX
#include <csignal>
#include <sys/types.h>
#include <unistd.h>
#endif

#include "core/i18n/I18N.h"
#include "core/os/Debug.h"
#include "core/os/FileCommunicator.h"
#include "core/os/SystemDependent.h"
#include "core/subs/SubFile.h"

namespace {
const QRegularExpression PLACEHOLDER(QStringLiteral("%([A-Za-z][A-Za-z0-9]*)"));

bool isWindows() { return SystemDependent::getAssetTag() == QLatin1String("win"); }

// Save a copy of `entries` (with the styles/attributes of `source`) as a
// file in the wire format, UTF-8. Null on success, else the error.
QString writeSubs(const Subtitles &source, const QList<SubEntryPtr> *entries, const QString &path, const SubFormatPtr &format) {
    Subtitles copy(source);
    if (entries) {
        QList<SubEntryPtr> kept;
        // Only entries that still exist in the document, in document order.
        for (const SubEntryPtr &e : source.entries())
            if (entries->contains(e)) kept.append(e);
        copy.setSublist(kept);
        copy.revalidateStyles();
    }
    SubFile sf(QStringLiteral("UTF-8"), source.getSubFile().getFPS(), format ? format->newInstance() : nullptr, path, SubFile::EXTENSION_GIVEN);
    return FileCommunicator::save(copy, sf, nullptr);
}
}  // namespace

RecipeExecutor::RecipeExecutor(QObject *parent) : QObject(parent) {}

RecipeExecutor::~RecipeExecutor() {
    if (wavCancel_) *wavCancel_ = true;
    if (process_ && process_->state() != QProcess::NotRunning) {
#ifdef Q_OS_UNIX
        // The whole process group, not only the direct child.
        if (process_->processId() > 0) ::kill(-pid_t(process_->processId()), SIGKILL);
#else
        if (process_->processId() > 0)
            QProcess::execute(QStringLiteral("taskkill"), {QStringLiteral("/T"), QStringLiteral("/F"), QStringLiteral("/PID"), QString::number(process_->processId())});
#endif
        process_->kill();
        process_->waitForFinished(1000);
    }
    cleanup();
}

QStringList RecipeExecutor::tokenize(const QString &text) {
    QStringList out;
    QString cur;
    bool inToken = false;
    QChar quote;
    for (int i = 0; i < text.length(); ++i) {
        const QChar c = text[i];
        if (!quote.isNull()) {
            if (c == quote) quote = QChar();
            else cur += c;
            continue;
        }
        if (c == QLatin1Char('"') || c == QLatin1Char('\'')) {
            quote = c;
            inToken = true;
            continue;
        }
        if (c.isSpace()) {
            if (inToken) {
                out.append(cur);
                cur.clear();
                inToken = false;
            }
            continue;
        }
        cur += c;
        inToken = true;
    }
    if (inToken) out.append(cur);
    return out;
}

QStringList RecipeExecutor::buildCommandLine(const QString &templ, const Recipe &recipe, const QMap<QString, QString> &values, const QMap<QChar, QString> &singles) {
    auto paramOf = [&](const QString &key) -> const RecipeParam * {
        for (const RecipeParam &p : recipe.params)
            if (p.key == key) return &p;
        return nullptr;
    };
    auto substitute = [&](const QString &token) {
        QString out;
        int last = 0;
        auto it = PLACEHOLDER.globalMatch(token);
        while (it.hasNext()) {
            const auto m = it.next();
            out += token.mid(last, m.capturedStart() - last);
            const QString name = m.captured(1);
            if (name.length() == 1) {
                const QChar c = name[0];
                out += singles.contains(c) ? singles.value(c) : m.captured(0);
            } else
                out += paramOf(name) ? values.value(name) : m.captured(0);
            last = m.capturedEnd();
        }
        out += token.mid(last);
        return out;
    };
    QStringList args;
    for (const QString &token : tokenize(templ)) {
        if (token.length() > 2 && token.startsWith(QLatin1Char('%'))) {
            const QString key = token.mid(1);
            if (const RecipeParam *p = paramOf(key)) {
                const QString value = values.value(key);
                if (p->type == RecipeParam::Type::CHECKBOX) {
                    for (const QString &piece : tokenize(value)) args.append(piece);
                } else
                    args.append(value.isNull() ? QString(QLatin1String("")) : value);
                continue;
            }
        }
        // Empty literal tokens ("") are dropped; a substituted token stays an
        // argument even when it expands to nothing (Java).
        if (token.isEmpty()) continue;
        const QString s = substitute(token);
        args.append(s.isNull() ? QString(QLatin1String("")) : s);
    }
    return args;
}

QString RecipeExecutor::pickOutputFile(const QString &dir, const QString &wireExtension) {
    static const QStringList known{QStringLiteral("srt"), QStringLiteral("ass"), QStringLiteral("ssa"), QStringLiteral("vtt"), QStringLiteral("sub"), QStringLiteral("ttml"),
                                   QStringLiteral("dfxp"), QStringLiteral("itt"), QStringLiteral("xml"), QStringLiteral("sbv"), QStringLiteral("stl"), QStringLiteral("smi")};
    const QFileInfoList files = QDir(dir).entryInfoList(QDir::Files, QDir::NoSort);
    for (const QFileInfo &fi : files)
        if (fi.suffix().compare(wireExtension, Qt::CaseInsensitive) == 0) return fi.filePath();
    for (const QFileInfo &fi : files)
        if (known.contains(fi.suffix().toLower())) return fi.filePath();
    return QString();
}

QString RecipeExecutor::applyPatch(const Subtitles &result, const QList<SubEntryPtr> &scope, OutputMode mode) {
    if (result.size() < scope.size()) return __("Output has {0} entries but {1} were sent; cannot patch by index.", result.size(), scope.size());
    for (int k = 0; k < scope.size(); ++k) {
        const SubEntryPtr &src = result.get(k);
        if (patchTiming(mode)) {
            scope[k]->setStartTime(src->getStartTime());
            scope[k]->setFinishTime(src->getFinishTime());
        }
        if (patchText(mode)) scope[k]->setText(src->getText());
    }
    return QString();
}

void RecipeExecutor::fail(const QString &message) {
    cleanup();
    emit finished(false, message);
}

void RecipeExecutor::cleanup() {
    if (!tempDir_.isEmpty()) {
        QDir(tempDir_).removeRecursively();
        tempDir_.clear();
    }
}

void RecipeExecutor::start(const RecipeRun &run) {
    run_ = run;
    cancelled_ = false;
    const Recipe &recipe = run_.recipe;
    const QString exe = RecipeResolver::resolve(recipe.path);
    if (exe.isEmpty()) {
        fail(__("Executable not found: {0}", recipe.path));
        return;
    }
    tempDir_ = QDir::tempPath() + QStringLiteral("/jubler_") + QUuid::createUuid().toString(QUuid::WithoutBraces).left(8) + QStringLiteral("_recipe");
    if (!QDir().mkpath(tempDir_)) {
        fail(__("Could not create a temporary working folder."));
        return;
    }
    const SubFormatPtr format = recipe.getFormat();
    const QString ext = format ? format->getExtension() : QStringLiteral("srt");
    QMap<QChar, QString> singles;
    singles.insert(QLatin1Char('x'), exe);

    // %i
    const QString input = tempDir_ + QStringLiteral("/input.") + ext;
    if (!run_.current) {
        fail(__("Could not read tool output."));
        return;
    }
    const QString err = writeSubs(*run_.current, run_.scope ? &*run_.scope : nullptr, input, format);
    if (!err.isNull()) {
        fail(err);
        return;
    }
    singles.insert(QLatin1Char('i'), input);

    // %o
    const bool hasOutput = recipe.command.contains(QLatin1String("%o"));
    outputIsFolder_ = false;
    if (!hasOutput)
        outputPath_ = input;   // the tool edits %i in place
    else if (recipe.outputFolder) {
        outputPath_ = tempDir_ + QStringLiteral("/out");
        QDir().mkpath(outputPath_);
        outputIsFolder_ = true;
    } else
        outputPath_ = tempDir_ + QStringLiteral("/output.") + ext;
    singles.insert(QLatin1Char('o'), outputPath_);

    // WINDOW params referenced in the command.
    QMap<QString, QString> values = run_.values;
    for (const RecipeParam &p : recipe.params) {
        if (p.type != RecipeParam::Type::WINDOW || !recipe.command.contains(QLatin1Char('%') + p.key)) continue;
        const Subtitles *other = run_.windows.value(p.key, nullptr);
        if (!other) {
            fail(__("This recipe needs another subtitle window ({0}) but none is available.", p.displayLabel()));
            return;
        }
        const QString path = tempDir_ + QStringLiteral("/window_") + p.key + QLatin1Char('.') + ext;
        const QString werr = writeSubs(*other, nullptr, path, format);
        if (!werr.isNull()) {
            fail(werr);
            return;
        }
        values.insert(p.key, path);
    }

    // %a %v %w
    if (recipe.command.contains(QLatin1String("%a"))) {
        if (run_.audioPath.isEmpty() || !QFileInfo::exists(run_.audioPath)) {
            fail(__("This recipe needs audio (%a) but no media file is attached."));
            return;
        }
        singles.insert(QLatin1Char('a'), run_.audioPath);
    }
    if (recipe.command.contains(QLatin1String("%v"))) {
        if (run_.videoPath.isEmpty() || !QFileInfo::exists(run_.videoPath)) {
            fail(__("This recipe needs video (%v) but no media file is attached."));
            return;
        }
        singles.insert(QLatin1Char('v'), run_.videoPath);
    }
    if (recipe.command.contains(QLatin1String("%w"))) {
        if (!run_.makeWav) {
            fail(__("This recipe needs audio (%w) but no media file is attached."));
            return;
        }
        // The WAV is extracted from the media first, on a worker thread.
        const QString wav = tempDir_ + QStringLiteral("/audio.wav");
        singles.insert(QLatin1Char('w'), wav);
        emit log(__("Extracting audio…"));
        extracting_ = true;
        wavCancel_ = std::make_shared<std::atomic<bool>>(false);
        const QPointer<RecipeExecutor> self(this);
        std::thread([self, cancel = wavCancel_, wav, values, singles, makeWav = run_.makeWav]() {
            int lastTenth = -1;
            QString err;
            const bool ok = makeWav(wav, *cancel, [self, &lastTenth](float p) {
                const int tenth = p < 0 ? -1 : int(p * 10);
                if (tenth > lastTenth && tenth < 10) {
                    lastTenth = tenth;
                    QMetaObject::invokeMethod(qApp, [self, tenth]() { if (self) emit self->log(QStringLiteral("  %1%").arg(tenth * 10)); }, Qt::QueuedConnection);
                }
            }, &err);
            QMetaObject::invokeMethod(qApp, [self, ok, err, values, singles]() {
                if (!self) return;   // the run window is gone
                self->extracting_ = false;
                if (self->cancelled_) self->fail(__("Cancelled."));
                else if (!ok) self->fail(__("Could not extract the audio: {0}", err));
                else self->launch(values, singles);
            }, Qt::QueuedConnection);
        }).detach();
        return;
    }
    launch(values, singles);
}

void RecipeExecutor::launch(const QMap<QString, QString> &values, const QMap<QChar, QString> &singles) {
    const Recipe &recipe = run_.recipe;
    QStringList args = buildCommandLine(recipe.command, recipe, values, singles);
    if (args.isEmpty()) {
        fail(__("Empty command."));
        return;
    }
    if (isWindows() && (args.first().endsWith(QLatin1String(".bat"), Qt::CaseInsensitive) || args.first().endsWith(QLatin1String(".cmd"), Qt::CaseInsensitive)))
        args = QStringList{QStringLiteral("cmd.exe"), QStringLiteral("/c")} + args;
    emit log(__("Executing command:"));
    emit log(args.join(QLatin1Char(' ')));
    emit log(QStringLiteral("----------------------------------"));

    process_ = std::make_unique<QProcess>();
    process_->setWorkingDirectory(tempDir_);
    process_->setProcessChannelMode(QProcess::MergedChannels);
    QProcessEnvironment env = QProcessEnvironment::systemEnvironment();
    env.insert(QStringLiteral("PATH"), RecipeResolver::augmentedPath());
    env.insert(QStringLiteral("PYTHONUNBUFFERED"), QStringLiteral("1"));
    process_->setProcessEnvironment(env);
    connect(process_.get(), &QProcess::readyReadStandardOutput, this, [this]() {
        pending_ += process_->readAllStandardOutput();
        // Lines end with \n, \r\n or a lone \r (progress output rewriting its line).
        for (;;) {
            int nl = -1;
            for (int i = 0; i < pending_.size(); ++i)
                if (pending_[i] == '\n' || pending_[i] == '\r') { nl = i; break; }
            if (nl < 0) break;
            // A \r at the very end may be the first half of \r\n: wait for more.
            if (pending_[nl] == '\r' && nl + 1 == pending_.size()) break;
            const QByteArray line = pending_.left(nl);
            const int skip = (pending_[nl] == '\r' && pending_[nl + 1] == '\n') ? 2 : 1;
            pending_ = pending_.mid(nl + skip);
            emit log(QString::fromUtf8(line));
        }
    });
    connect(process_.get(), &QProcess::finished, this, &RecipeExecutor::onProcessFinished);
    connect(process_.get(), &QProcess::errorOccurred, this, [this](QProcess::ProcessError e) {
        if (e == QProcess::FailedToStart) fail(__("Executable not found: {0}", run_.recipe.path));
    });
#ifdef Q_OS_UNIX
    // Its own process group, so that cancel() reaches the whole tree.
    process_->setChildProcessModifier([]() { ::setpgid(0, 0); });
#endif
    const QString program = args.takeFirst();
    process_->start(program, args);
}

void RecipeExecutor::cancel() {
    cancelled_ = true;
    if (wavCancel_) *wavCancel_ = true;
    if (!process_ || process_->state() == QProcess::NotRunning) return;
    // The whole process tree: terminate, then kill after a grace period.
    const qint64 pid = process_->processId();
#ifdef Q_OS_UNIX
    if (pid > 0) ::kill(-pid_t(pid), SIGTERM);
#else
    if (pid > 0) QProcess::execute(QStringLiteral("taskkill"), {QStringLiteral("/T"), QStringLiteral("/F"), QStringLiteral("/PID"), QString::number(pid)});
#endif
    process_->terminate();
    QTimer::singleShot(3000, this, [this, pid]() {
        if (!process_ || process_->state() == QProcess::NotRunning) return;
#ifdef Q_OS_UNIX
        if (pid > 0) ::kill(-pid_t(pid), SIGKILL);
#endif
        process_->kill();
    });
}

void RecipeExecutor::onProcessFinished(int exitCode, QProcess::ExitStatus status) {
    if (pending_.endsWith('\r')) pending_.chop(1);
    if (!pending_.isEmpty()) {
        emit log(QString::fromUtf8(pending_));
        pending_.clear();
    }
    if (cancelled_) {
        fail(__("Cancelled."));
        return;
    }
    if (status != QProcess::NormalExit || exitCode != 0) {
        fail(__("Tool failed (exit code {0}).", exitCode));
        return;
    }
    QString outFile = outputPath_;
    if (outputIsFolder_) {
        const SubFormatPtr format = run_.recipe.getFormat();
        outFile = pickOutputFile(outputPath_, format ? format->getExtension() : QStringLiteral("srt"));
        if (outFile.isEmpty()) {
            fail(__("The tool produced no subtitle file in its output folder."));
            return;
        }
    }
    SubFile sf(outFile, SubFile::EXTENSION_GIVEN);
    sf.setEncoding(QStringLiteral("UTF-8"));
    // Frame-based wire formats: read back at the frame rate %i was written with.
    if (run_.current) sf.setFPS(run_.current->getSubFile().getFPS());
    const QString data = FileCommunicator::load(sf, false);
    if (data.isNull()) {
        fail(__("Could not read tool output."));
        return;
    }
    auto result = std::make_shared<Subtitles>(sf);
    result->populate(sf, data, false);
    if (result->isEmpty()) {
        fail(__("Tool output not recognized or empty."));
        return;
    }
    if (isPatch(run_.recipe.outputMode)) {
        const qsizetype sent = run_.scope ? run_.scope->size() : (run_.current ? run_.current->size() : 0);
        if (result->size() < sent) {
            fail(__("Output has {0} entries but {1} were sent; cannot patch by index.", result->size(), sent));
            return;
        }
    }
    if (run_.current) result->setSubFile(run_.current->getSubFile());
    cleanup();
    emit resultReady(result, run_);
    emit finished(true, __("Done."));
}
