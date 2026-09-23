/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// An example Jubler plugin using every extension point of jubler/PluginApi.h.
// Build it with examples/plugin/CMakeLists.txt and drop the library into the
// per-user plugins folder, then enable it in Preferences ▸ Plugins.

#include <QCheckBox>
#include <QObject>
#include <QRegularExpression>
#include <QVBoxLayout>
#include <algorithm>

#include "jubler/PluginApi.h"

namespace {

jubler::HostServices *g_host = nullptr;

// Upper-case the text outside the {\…} override blocks.
QString upper(const QString &tagged) {
    QString out;
    bool inTag = false;
    for (const QChar c : tagged) {
        if (c == QLatin1Char('{')) inTag = true;
        out += inTag ? c : c.toUpper();
        if (c == QLatin1Char('}')) inTag = false;
    }
    return out;
}

void upperRows(jubler::Document &doc, QList<int> rows) {
    if (rows.isEmpty())
        for (int i = 0; i < doc.count(); ++i) rows.append(i);
    for (const int r : rows) {
        jubler::Subtitle s = doc.at(r);
        s.text = upper(s.text);
        doc.replace(r, s);
    }
}

class UpperTool : public jubler::Tool {
public:
    QString id() const override { return QStringLiteral("exampleUpper"); }
    QString menuText() const override { return QStringLiteral("Upper case (example plugin)"); }
    bool run(jubler::Document &doc, QWidget *) override {
        upperRows(doc, doc.selection());
        return true;
    }
};

class UpperCommand : public jubler::CommandLineTool {
public:
    QString name() const override { return QStringLiteral("upper"); }
    QString help() const override { return QStringLiteral("Upper-case every subtitle (example plugin)."); }
    QString run(jubler::Document &doc, const QMap<QString, QString> &) override {
        upperRows(doc, {});
        return QString();
    }
};

// "#EXAMPLE-LINES" then one "start|end|text" line per subtitle ("\n" as "\\n").
class LinesFormat : public jubler::Format {
public:
    QString name() const override { return QStringLiteral("Example Lines"); }
    QString extension() const override { return QStringLiteral("exl"); }
    bool canRead(const QString &text) const override { return text.startsWith(QLatin1String("#EXAMPLE-LINES")); }
    bool read(const QString &text, float, jubler::Document &out) override {
        const QStringList lines = text.split(QLatin1Char('\n'));
        for (int i = 1; i < lines.size(); ++i) {
            const QStringList f = lines[i].split(QLatin1Char('|'));
            if (f.size() < 3) continue;
            jubler::Subtitle s;
            s.start = f[0].toDouble();
            s.end = f[1].toDouble();
            s.text = f.mid(2).join(QLatin1Char('|')).replace(QLatin1String("\\n"), QLatin1String("\n"));
            out.insert(out.count(), s);
        }
        return out.count() > 0;
    }
    QString write(const jubler::Document &doc, float) override {
        QString out = QStringLiteral("#EXAMPLE-LINES\n");
        for (int i = 0; i < doc.count(); ++i) {
            const jubler::Subtitle s = doc.at(i);
            out += QStringLiteral("%1|%2|%3\n").arg(s.start, 0, 'f', 3).arg(s.end, 0, 'f', 3).arg(QString(s.text).replace(QLatin1Char('\n'), QLatin1String("\\n")));
        }
        return out;
    }
};

class ReverseTranslator : public jubler::Translator {
public:
    QString name() const override { return QStringLiteral("Reverse (example plugin)"); }
    QList<jubler::Language> sourceLanguages() const override { return {{QStringLiteral("en"), QStringLiteral("English")}}; }
    QList<jubler::Language> targetLanguages(const QString &) const override { return {{QStringLiteral("en-rev"), QStringLiteral("Reversed English")}}; }
    QString defaultTarget() const override { return QStringLiteral("en-rev"); }
    bool translate(QStringList &texts, const QString &, const QString &, const std::function<bool(int)> &progress, QString *) override {
        for (int i = 0; i < texts.size(); ++i) {
            std::reverse(texts[i].begin(), texts[i].end());
            if (!progress(i + 1)) return false;
        }
        return true;
    }
};

class TehChecker : public jubler::SpellChecker {
public:
    QString name() const override { return QStringLiteral("Example speller"); }
    bool start(QString *) override { return true; }
    QList<jubler::Misspelling> check(const QString &text) override {
        QList<jubler::Misspelling> out;
        static const QRegularExpression teh(QStringLiteral("\\bteh\\b"));
        for (auto it = teh.globalMatch(text); it.hasNext();) {
            const auto m = it.next();
            out.append({int(m.capturedStart()), m.captured(), {QStringLiteral("the")}});
        }
        return out;
    }
};

class Logger : public jubler::EventListener {
public:
    void documentOpened(jubler::Document &doc) override {
        if (enabled()) g_host->debug(QStringLiteral("Example plugin: opened %1 (%2 subtitles)").arg(doc.filePath()).arg(doc.count()));
    }
    void afterSave(jubler::Document &doc) override {
        if (enabled()) g_host->debug(QStringLiteral("Example plugin: saved %1").arg(doc.filePath()));
    }
    static bool enabled() { return g_host->setting(QStringLiteral("example.log"), QStringLiteral("true")) == QLatin1String("true"); }
};

class Page : public jubler::PreferencesPage {
public:
    QString title() const override { return QStringLiteral("Example"); }
    QWidget *createWidget(QWidget *parent) override {
        auto *w = new QWidget(parent);
        auto *lay = new QVBoxLayout(w);
        box_ = new QCheckBox(QStringLiteral("Log opened and saved documents"), w);
        lay->addWidget(box_);
        lay->addStretch(1);
        return w;
    }
    void load() override { if (box_) box_->setChecked(Logger::enabled()); }
    void save() override {
        if (box_) g_host->setSetting(QStringLiteral("example.log"), box_->isChecked() ? QStringLiteral("true") : QStringLiteral("false"));
    }

private:
    QCheckBox *box_ = nullptr;
};

}  // namespace

class ExamplePlugin : public QObject, public jubler::Plugin {
    Q_OBJECT
    Q_PLUGIN_METADATA(IID JublerPlugin_iid FILE "plugin.json")
    Q_INTERFACES(jubler::Plugin)
public:
    void registerExtensions(jubler::Registrar &r) override {
        g_host = &r.host();
        r.addTool(std::make_shared<UpperTool>());
        r.addCommandLineTool(std::make_shared<UpperCommand>());
        r.addFormat(std::make_shared<LinesFormat>());
        r.addTranslator(std::make_shared<ReverseTranslator>());
        r.addSpellChecker(std::make_shared<TehChecker>());
        r.addEventListener(std::make_shared<Logger>());
        r.addPreferencesPage(std::make_shared<Page>());
    }
};

#include "ExamplePlugin.moc"
