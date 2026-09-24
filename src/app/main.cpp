/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include <QAction>
#include <QApplication>
#include <QDir>
#include <QFileInfo>
#include <QFileOpenEvent>
#include <QScreen>
#include <QLibraryInfo>
#include <QLocalServer>
#include <QLocalSocket>
#include <QLocale>
#include <QMessageBox>
#include <QSurfaceFormat>
#include <QTextStream>
#include <QTimer>
#include <QTranslator>
#include <clocale>
#include <cmath>
#include <exception>
#include <utility>

#ifdef Q_OS_UNIX
#include <unistd.h>
#endif

#include "app/AppContext.h"
#include "app/DesktopIntegration.h"
#include "app/Theme.h"
#include "app/media/AppMediaFile.h"
#include "app/tools/TranslateDialogs.h"
#include "app/externals/RecipeDialogs.h"
#include "core/externals/Recipe.h"
#include "app/subdownload/PluginRegistry.h"
#include "app/update/AutoUpdater.h"
#include "app/ui/MainWindow.h"
#include "core/cmdline/CommandLine.h"
#include "core/i18n/I18N.h"
#include "core/options/JavaPrefs.h"
#include "core/options/Options.h"
#include "core/options/Prefs.h"
#include "core/os/AutoSaver.h"
#include "core/os/Debug.h"
#include "core/os/SystemDependent.h"
#include "core/subs/SubFile.h"
#include "core/subs/Subtitles.h"

#ifdef Q_OS_WIN
// Only AttachConsole: without the GDI macros (ERROR, …) and min/max.
#define WIN32_LEAN_AND_MEAN
#define NOGDI
#ifndef NOMINMAX
#define NOMINMAX
#endif
#include <cstdio>
#include <windows.h>
#endif

namespace {

// One instance per user: the real user id on Unix (the environment can be
// missing or changed), the user name on Windows.
QString instanceName() {
#ifdef Q_OS_UNIX
    return QStringLiteral("jubler-") + QString::number(getuid());
#else
    QString user = qEnvironmentVariable("USERNAME");
    if (user.isEmpty()) user = QStringLiteral("default");
    return QStringLiteral("jubler-") + user;
#endif
}

// Open every existing, readable path into the first window (when empty) or
// a new one; the running instance also raises its current window.
QStringList g_pendingFiles;   // arrived before the first window existed

void openFiles(const QStringList &paths) {
    if (AppContext::windows().isEmpty()) {
        g_pendingFiles += paths;   // opened once the first window is up
        return;
    }
    MainWindow *first = AppContext::windows().first();
    for (const QString &p : paths) {
        const QFileInfo fi(p);
        if (!fi.exists() || !fi.isFile() || !fi.isReadable()) continue;
        first->loadFile(SubFile(fi.absoluteFilePath(), SubFile::EXTENSION_GIVEN), false);
    }
}

void raiseCurrent() {
    MainWindow *w = AppContext::currentWindow();
    if (!w && !AppContext::windows().isEmpty()) w = AppContext::windows().last();
    if (!w) return;
    w->setWindowState((w->windowState() & ~Qt::WindowMinimized) | Qt::WindowActive);
    w->show();
    w->raise();
    w->activateWindow();
}

// Every uncaught exception is logged and reported; the application keeps
// running. Port of `ExceptionHandler`.
class JublerApplication : public QApplication {
public:
    using QApplication::QApplication;
    bool notify(QObject *receiver, QEvent *event) override {
        try {
            return QApplication::notify(receiver, event);
        } catch (const std::exception &e) {
            Debug::debug(e);
            QMessageBox::critical(nullptr, __("Error"),
                                  __("An unexpected error occurred:\n{0}\n\nPlease check the log file for details.", QString::fromUtf8(e.what())));
        } catch (...) {
            Debug::debug(QStringLiteral("Unexpected non-standard exception"));
            QMessageBox::critical(nullptr, __("Error"),
                                  __("An unexpected error occurred.\n\nPlease check the log file for details."));
        }
        return false;
    }
    // Files handed over by the OS ("open with", dock drop on macOS).
    bool event(QEvent *e) override {
        if (e->type() == QEvent::FileOpen) {
            openFiles({static_cast<QFileOpenEvent *>(e)->file()});
            return true;
        }
        return QApplication::event(e);
    }
};

// The standard dialog buttons (QPlatformTheme) through Jubler's own catalog:
// Qt ships no qtbase translation for some of Jubler's languages (el, sr).
class StandardButtonTranslator : public QTranslator {
public:
    using QTranslator::QTranslator;
    bool isEmpty() const override { return false; }
    QString translate(const char *context, const char *sourceText, const char *, int) const override {
        if (!context || qstrcmp(context, "QPlatformTheme") != 0 || !sourceText) return QString();
        const QString plain = QString::fromUtf8(sourceText).remove(QLatin1Char('&'));
        const QString t = QCoreApplication::translate("jubler", plain.toUtf8().constData());
        return t == plain ? QString() : t;
    }
};

// Listed for the catalog (the texts QPlatformTheme asks for).
[[maybe_unused]] const char *const STANDARD_BUTTONS[] = {N__("OK"), N__("Save"), N__("Save All"), N__("Open"), N__("Yes"), N__("Yes to All"), N__("No"),
                                                         N__("No to All"), N__("Abort"), N__("Retry"), N__("Ignore"), N__("Close"), N__("Cancel"),
                                                         N__("Discard"), N__("Help"), N__("Apply"), N__("Reset"), N__("Restore Defaults")};

// Callable again (after a preferences migration): replaces the translators it installed.
void installTranslations(QCoreApplication &app) {
    static QList<QTranslator *> installed;
    for (QTranslator *t : std::as_const(installed)) {
        app.removeTranslator(t);
        delete t;
    }
    installed.clear();
    const QString pref = Options::getLanguage();
    const bool automatic = pref.isEmpty() || pref == QLatin1String("auto");
    QLocale locale = automatic ? QLocale::system() : QLocale(pref);
    // A chosen language is Qt's default locale too (formats, dialogs); the C
    // library's numeric locale stays "C" for libmpv.
    QLocale::setDefault(locale);
    auto *qt = new QTranslator(&app);
    installed.append(qt);
    if (qt->load(locale, QStringLiteral("qtbase"), QStringLiteral("_"), QLibraryInfo::path(QLibraryInfo::TranslationsPath))) app.installTranslator(qt);
    // A jubler_<lang>.qm next to the binary (i18n/) wins over the embedded one,
    // so translators can try their work without rebuilding.
    auto *own = new QTranslator(&app);
    installed.append(own);
    if (own->load(locale, QStringLiteral("jubler"), QStringLiteral("_"), QCoreApplication::applicationDirPath() + QStringLiteral("/i18n"))
        || own->load(locale, QStringLiteral("jubler"), QStringLiteral("_"), QStringLiteral(":/i18n")))
        app.installTranslator(own);
    auto *buttons = new StandardButtonTranslator(&app);   // installed last: asked first
    installed.append(buttons);
    app.installTranslator(buttons);
}

}  // namespace

int main(int argc, char *argv[]) {
    QStringList args;
    for (int i = 1; i < argc; ++i) args.append(QString::fromLocal8Bit(argv[i]));

    if (CommandLine::isCommandLineInvocation(args)) {
#ifdef Q_OS_WIN
        // A GUI program has no console of its own: the command line writes to
        // the one it was started from — unless its output already goes
        // somewhere (a pipe, a file), which it keeps.
        const HANDLE stdOut = GetStdHandle(STD_OUTPUT_HANDLE);
        if ((stdOut == nullptr || stdOut == INVALID_HANDLE_VALUE) && AttachConsole(ATTACH_PARENT_PROCESS)) {
            std::freopen("CONOUT$", "w", stdout);
            std::freopen("CONOUT$", "w", stderr);
        }
#endif
        QCoreApplication app(argc, argv);
        std::setlocale(LC_NUMERIC, "C");
        QCoreApplication::setApplicationName(QStringLiteral("Jubler"));
        Options::load();
        installTranslations(app);
        PluginRegistry::discover(false);   // plugin command-line tools and formats
        QTextStream out(stdout), err(stderr);
        return CommandLine::run(args, out, err);
    }

#ifdef Q_OS_LINUX
    // The AppImage carries Qt's GTK theme (GNOME and the GTK desktops use it by
    // themselves) but not KDE's: on KDE its dialogs come from the desktop
    // through the portal (as the Java's did), unless the user chose a theme.
    if (!qEnvironmentVariableIsEmpty("APPIMAGE") && !qEnvironmentVariableIsSet("QT_QPA_PLATFORMTHEME")
        && qEnvironmentVariable("XDG_CURRENT_DESKTOP").split(QLatin1Char(':')).contains(QStringLiteral("KDE"), Qt::CaseInsensitive))
        qputenv("QT_QPA_PLATFORMTHEME", "xdgdesktopportal");
#endif
    // The UI scaling preference must reach Qt before the application exists.
    QCoreApplication::setApplicationName(QStringLiteral("Jubler"));
    const bool firstRun = !Prefs::storeExists();
    Options::load();
    const float scaling = Options::getScaling();
    if (scaling > 0 && std::abs(scaling - 1.0f) >= 0.005f && !qEnvironmentVariableIsSet("QT_SCALE_FACTOR"))
        qputenv("QT_SCALE_FACTOR", QByteArray::number(double(scaling)));

    // The GL context libmpv renders into (set before the application exists).
    QSurfaceFormat fmt = QSurfaceFormat::defaultFormat();
    fmt.setSwapInterval(1);
    fmt.setAlphaBufferSize(0);
    QSurfaceFormat::setDefaultFormat(fmt);
    JublerApplication app(argc, argv);
    // libmpv parses numbers with the C locale; Qt may have switched it.
    std::setlocale(LC_NUMERIC, "C");
    QApplication::setApplicationDisplayName(QStringLiteral("Jubler"));
    QApplication::setDesktopFileName(QStringLiteral("com.panayotis.jubler"));
    // Single instance: hand the file list to the running instance and exit.
    {
        QLocalSocket probe;
        probe.connectToServer(instanceName());
        if (probe.waitForConnected(500)) {
            QStringList absolute;
            for (const QString &a : args) absolute.append(QFileInfo(a).absoluteFilePath());
            probe.write((absolute.join(QLatin1Char('\n')) + QLatin1Char('\n')).toUtf8());
            probe.flush();
            probe.waitForBytesWritten(1000);
            probe.disconnectFromServer();
            return 0;
        }
    }
    Debug::init();
    if (const QScreen *screen = QGuiApplication::primaryScreen()) {
        const double using_ = screen->devicePixelRatio();
        Debug::debug(QStringLiteral("UI scaling: screen %1 dpi, system %2, extra zoom %3, using %4")
                         .arg(qRound(screen->physicalDotsPerInch()))
                         .arg(using_ / qEnvironmentVariable("QT_SCALE_FACTOR", QStringLiteral("1")).toDouble())
                         .arg(double(Options::getScaling()))
                         .arg(using_));
    }
    Theme::applyVariation(int(Options::getThemeVariation()));
    installTranslations(app);
    app.setWindowIcon(Theme::logo());
    RecipeUi::installSecretPrompts();
    RecipeResolver::startLoginShellProbe();
    PluginRegistry::discover();

    auto *server = new QLocalServer(&app);
    server->setSocketOptions(QLocalServer::UserAccessOption);
    QLocalServer::removeServer(instanceName());
    if (server->listen(instanceName())) {
        QObject::connect(server, &QLocalServer::newConnection, &app, [server]() {
            while (QLocalSocket *s = server->nextPendingConnection()) {
                QObject::connect(s, &QLocalSocket::readyRead, s, [s]() {
                    const QStringList lines = QString::fromUtf8(s->readAll()).split(QLatin1Char('\n'), Qt::SkipEmptyParts);
                    if (lines.isEmpty()) raiseCurrent(); else openFiles(lines);
                    raiseCurrent();
                });
                QObject::connect(s, &QLocalSocket::disconnected, s, &QObject::deleteLater);
            }
        });
    } else
        Debug::debug(QStringLiteral("Single-instance server unavailable: ") + server->errorString());

    // First run: offer the preferences of the Java Jubler, if it was used here.
    if (firstRun) {
        const QMap<QString, QString> java = JavaPrefs::readNativeStore();
        if (!java.isEmpty()
            && QMessageBox::question(nullptr, __("Import from Java Jubler"),
                                     __("Preferences of the older, Java based, Jubler were found.\nDo you want to use them in this version?"))
                   == QMessageBox::Yes) {
            JavaPrefs::apply(java);
            Options::load();
            Theme::applyVariation(int(Options::getThemeVariation()));
            installTranslations(app);
            if (std::abs(Options::getScaling() - scaling) >= 0.005f)
                QMessageBox::information(nullptr, __("Import from Java Jubler"), __("The interface scaling will be applied the next time Jubler starts."));
        }
        Prefs::set(QStringLiteral("system.javaprefs.asked"), true);   // never ask again, whatever the answer
    }

    auto *first = new MainWindow();
    first->show();
    // Files from the command line, then the recovered autosaves.
    QTimer::singleShot(0, &app, [args, first]() {
        TranslateUi::migrateLegacyKeys(first);
        const QList<PluginRegistry::PluginInfo> fresh = PluginRegistry::newPlugins();
        if (!fresh.isEmpty()) {
            QString msg = __("New plugins were found but not loaded yet:");
            for (const auto &p : fresh) msg += QStringLiteral("\n  • ") + p.name;
            msg += QStringLiteral("\n\n") + __("To use them, enable them in Preferences → Plugins and restart Jubler.");
            QMessageBox::information(first, __("New plugins found"), msg);
        }
        openFiles(args + std::exchange(g_pendingFiles, {}));
        openFiles(AutoSaver::getAutoSaveListOnLoad());
        AutoUpdater::checkAtStartup();
        DesktopIntegration::registerAppImage();
        AutoSaver::launch([]() {
            QList<AutoSaver::Candidate> out;
            for (MainWindow *w : AppContext::windows())
                if (w->isUnsaved() && !w->isEmptyContent() && w->getSubtitles())
                    out.append({w->getSubtitles(), QFileInfo(w->getSubtitles()->getSubFile().getSaveFile()).fileName()});
            return out;
        });
    });
    // Development aid: JUBLER_SCREENSHOT=<png> grabs the first window after
    // three seconds and quits (works with QT_QPA_PLATFORM=offscreen).
    // JUBLER_VIDEO=<file> attaches a video first, JUBLER_ACTIONS=<names> triggers
    // named menu actions (comma separated) one second after start, or
    // JUBLER_ACTIONS_DELAY ms later (or "NAME@ms" each). JUBLER_WINDOW=<w>x<h> sizes the window
    // (the offscreen platform gives a small screen).
    const QString shot = qEnvironmentVariable("JUBLER_SCREENSHOT");
    if (!shot.isEmpty()) {
        QTimer::singleShot(1000, &app, []() {
            if (AppContext::windows().isEmpty()) return;
            MainWindow *w = AppContext::windows().first();
            if (const QString size = qEnvironmentVariable("JUBLER_WINDOW"); size.contains(QLatin1Char('x'))) {
                const QStringList wh = size.split(QLatin1Char('x'));
                w->resize(wh.at(0).toInt(), wh.at(1).toInt());   // offscreen: the screen is 800x600
            }
            const QString video = qEnvironmentVariable("JUBLER_VIDEO");
            if (!video.isEmpty()) { w->getMediaFile()->setNewVideoFile(video); w->mediaChanged(); }
            // "TPE,TPP" fires JUBLER_ACTIONS_DELAY ms after the start; "TPE@2000,TPP@6000"
            // gives each its own moment (a media needs seconds to settle).
            const int common = qEnvironmentVariableIntValue("JUBLER_ACTIONS_DELAY");
            for (const QString &item : qEnvironmentVariable("JUBLER_ACTIONS").split(QLatin1Char(','), Qt::SkipEmptyParts)) {
                const QString name = item.section(QLatin1Char('@'), 0, 0);
                const int at = item.contains(QLatin1Char('@')) ? item.section(QLatin1Char('@'), 1).toInt() : common;
                QTimer::singleShot(at, w, [w, name]() {
                    if (auto *a = w->findChild<QAction *>(name)) a->trigger();
                });
            }
        });
        QTimer::singleShot(qEnvironmentVariableIntValue("JUBLER_SCREENSHOT_DELAY") > 0 ? qEnvironmentVariableIntValue("JUBLER_SCREENSHOT_DELAY") : 3000, &app, [shot]() {
            QWidget *target = QApplication::activeWindow();
            if (!target && !AppContext::windows().isEmpty()) target = AppContext::windows().first();
            if (target) target->grab().save(shot);
            QApplication::exit(0);
        });
    }
    const int rc = app.exec();
    return rc;
}
