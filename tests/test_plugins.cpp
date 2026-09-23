/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// The general plugin API: the example plugin registers every extension point
// and each one works through Jubler's own registries.
#include "TestSupport.h"

#include <QPluginLoader>

#include "core/formats/SubFormat.h"
#include "core/plugins/PluginHost.h"
#include "core/spell/SpellChecker.h"
#include "core/style/SubStyle.h"
#include "core/subs/SubFile.h"
#include "core/subs/Subtitles.h"
#include "core/tools/Tool.h"
#include "core/translate/Translator.h"

namespace {

class StubHost : public jubler::HostServices {
public:
    jubler::HttpResponse get(const QString &, const QMap<QString, QString> &, bool, std::shared_ptr<jubler::CancelToken> *) override { return {}; }
    jubler::HttpResponse post(const QString &, const QMap<QString, QString> &, const QByteArray &, bool, std::shared_ptr<jubler::CancelToken> *) override { return {}; }
    QString userAgent() const override { return QStringLiteral("test"); }
    QString browserUserAgent() const override { return QStringLiteral("test"); }
    jubler::ProviderResult<QByteArray> extractSubtitleBytes(const QByteArray &p) override { return p; }
    std::optional<QString> movieHash(const QString &) override { return std::nullopt; }
    bool isSecretStored(const QString &) override { return false; }
    bool storeSecret(QWidget *, const QString &, const QString &) override { return false; }
    QString loadSecret(QWidget *, const QString &) override { return QString(); }
    QString tr(const char *english) override { return QString::fromUtf8(english); }
    void debug(const QString &m) override { log.append(m); }
    ParsedQuery parseQuery(const QString &q) override { return {q, {}, {}}; }
    QString askApiKey(QWidget *, const QString &, const QString &, const QString &, bool) override { return QString(); }
    QString setting(const QString &key, const QString &d) override { return settings.value(key, d); }
    void setSetting(const QString &key, const QString &v) override { settings.insert(key, v); }
    QStringList log;
    QMap<QString, QString> settings;
};

}  // namespace

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    testInitPrefs();
    StubHost host;
    PluginHost::setHostServices(&host);

    QPluginLoader loader(QStringLiteral(JUBLER_EXAMPLE_PLUGIN));
    QObject *root = loader.instance();
    CHECK(root, qPrintable(QStringLiteral("plugin loads: ") + loader.errorString()));
    QString error;
    CHECK(PluginHost::registerPlugin(root, QStringLiteral("Example plugin"), &error), qPrintable(error));
    CHECK(!PluginHost::registerPlugin(&app, QStringLiteral("not a plugin"), &error), "a non-plugin object is refused");

    // Menu tool, listeners, pages: kept for the application.
    CHECK_EQ(int(PluginHost::tools().size()), 1, "one tool");
    CHECK_EQ(int(PluginHost::eventListeners().size()), 1, "one listener");
    CHECK_EQ(int(PluginHost::preferencesPages().size()), 1, "one page");

    // The menu tool through the document adapter (inline tags kept).
    Subtitles subs;
    subs.add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("hello")));
    subs.add(std::make_shared<SubEntry>(3.0, 4.0, QStringLiteral("world")));
    {
        PluginDocument doc(subs, {1});
        CHECK(PluginHost::tools().first()->run(doc, nullptr) && doc.changed(), "tool ran");
    }
    CHECK_EQ(subs.get(0)->getText(), QStringLiteral("hello"), "unselected row untouched");
    CHECK_EQ(subs.get(1)->getText(), QStringLiteral("WORLD"), "selected row changed");
    {
        PluginDocument doc(subs);
        jubler::Subtitle s = doc.at(0);
        s.text = QStringLiteral("{\\b1}bold{\\b0} x");
        doc.replace(0, s);
        CHECK_EQ(subs.get(0)->getText(), QStringLiteral("bold x"), "tags parsed into overrides");
        CHECK(subs.get(0)->getStyleover(StyleType::BOLD) != nullptr, "bold override set");
        CHECK(doc.at(0).text.contains(QStringLiteral("\\b1")), "overrides come back as tags");
    }

    // Style changes: the tags reset to the NEW style; literal braces survive.
    {
        auto yellow = std::make_shared<SubStyle>(QStringLiteral("Yellow"));
        yellow->set(StyleType::PRIMARY, StyleValue(AlphaColor(0xFFFFFF00u)));
        subs.getStyleList().add(yellow);
        subs.get(1)->setStyle(yellow);
        PluginDocument doc(subs);
        jubler::Subtitle s = doc.at(1);
        s.style = QStringLiteral("Default");
        s.text = QStringLiteral("{\\1c&H0000FF&}x{\\c}y");
        doc.replace(1, s);
        CHECK(subs.get(1)->getStyle()->getName() == QLatin1String("Default"), "style switched");
        CHECK(!doc.at(1).text.contains(QStringLiteral("00FFFF")), "reset resolves to the new style, not the old one");
        s = doc.at(0);
        s.text = QStringLiteral("\\{laughs\\} hello");
        doc.replace(0, s);
        CHECK_EQ(subs.get(0)->getText(), QStringLiteral("{laughs} hello"), "escaped braces are literal");
        CHECK_EQ(doc.at(0).text, QStringLiteral("\\{laughs\\} hello"), "literal braces come back escaped");
        s.text = doc.at(0).text;
        doc.replace(0, s);
        CHECK_EQ(subs.get(0)->getText(), QStringLiteral("{laughs} hello"), "round trip keeps them");
    }

    // Command-line tool.
    auto cli = ToolRegistry::instance().findByCommandName(QStringLiteral("upper"));
    CHECK(cli != nullptr, "command-line tool registered");
    auto cmdSubs = std::make_unique<Subtitles>();
    cmdSubs->add(std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("quiet")));
    CommandLineContext::addSubtitles(QString(), std::move(cmdSubs));
    if (cli) CHECK_EQ(cli->executeParamsLine(QString(), false), QString(), "command runs");
    CHECK_EQ(CommandLineContext::getSubtitles(QString())->get(0)->getText(), QStringLiteral("QUIET"), "command changed the document");

    // Format: detected, read and written through the normal paths.
    SubFile sf(QStringLiteral("/tmp/x.exl"), SubFile::EXTENSION_GIVEN);
    Subtitles loaded(sf);
    loaded.populate(sf, QStringLiteral("#EXAMPLE-LINES\n1.000|2.500|one\\ntwo\n3.000|4.000|three\n"), false);
    CHECK_EQ(loaded.size(), 2, "plugin format parsed");
    CHECK_EQ(loaded.get(0)->getText(), QStringLiteral("one\ntwo"), "plugin format text");
    CHECK(sf.getFormat() && sf.getFormat()->getName() == QLatin1String("Example Lines"), "plugin format detected");
    SubFormatPtr fmt = Availabilities::formats().findFromName(QStringLiteral("Example Lines"));
    CHECK(fmt != nullptr, "format in the registry");
    if (fmt) {
        const QString out = QStringLiteral("/tmp/jubler-qt-test-plugin-%1.exl").arg(QCoreApplication::applicationPid());
        SubFormat::SaveError err;
        CHECK(fmt->newInstance()->produce(loaded, out, nullptr, err), "plugin format written");
        CHECK(readTextFile(out).startsWith(QStringLiteral("#EXAMPLE-LINES\n1.000|2.500|one\\ntwo")), "written text");
        QFile::remove(out);
    }

    // Content-based detection (no telling extension).
    {
        SubFile sf2(QStringLiteral("/tmp/x.dat"), SubFile::EXTENSION_GIVEN);
        Subtitles byContent(sf2);
        byContent.populate(sf2, QStringLiteral("#EXAMPLE-LINES\n1.000|2.000|one\n"), false);
        CHECK(byContent.size() == 1 && sf2.getFormat() && sf2.getFormat()->getName() == QLatin1String("Example Lines"), "detected by content");
    }

    // Translator, after the built-in ones.
    AvailTranslators::registerBuiltin();
    CHECK(AvailTranslators::all().size() >= 2, "built-in translator kept");
    auto tr = AvailTranslators::all().last();
    CHECK_EQ(tr->getDefinition(), QStringLiteral("Reverse (example plugin)"), "translator registered");
    CHECK(tr->isReady().isNull(), "translator ready");
    QList<SubEntryPtr> list{std::make_shared<SubEntry>(1.0, 2.0, QStringLiteral("abc"))};
    CHECK(tr->translate(list, tr->getDefaultSourceLanguage(), tr->getDefaultDestinationLanguage(), nullptr), "translated");
    CHECK_EQ(list.first()->getText(), QStringLiteral("cba"), "translation applied");

    // Spell checker, after the built-in one.
    CHECK(AvailSpellCheckers::all().size() >= 2, "built-in speller kept");
    auto sp = AvailSpellCheckers::all().last();
    sp->start();
    const QList<SpellError> errs = sp->checkSpelling(QStringLiteral("teh cat"));
    CHECK(errs.size() == 1 && errs.first().alternatives == QStringList{QStringLiteral("the")}, "speller works");

    // Events and settings through the host.
    PluginDocument doc(loaded);
    PluginHost::eventListeners().first()->documentOpened(doc);
    CHECK(!host.log.isEmpty() && host.log.last().contains(QStringLiteral("opened")), "listener used the host");
    return testFinish("test_plugins");
}
