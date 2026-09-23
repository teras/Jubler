/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/plugins/PluginHost.h"

#include <QEventLoop>
#include <QObject>
#include <QThread>
#include <QTimer>
#include <atomic>

#include "core/formats/SubFormat.h"
#include "core/formats/TextSubFormat.h"
#include "core/formats/text/SubStationAlpha.h"
#include "core/i18n/I18N.h"
#include "core/os/Debug.h"
#include "core/spell/SpellChecker.h"
#include "core/subs/Subtitles.h"
#include "core/tools/Tool.h"
#include "core/translate/Translator.h"

// ---- PluginDocument ---------------------------------------------------------------------------

namespace {

void fill(SubEntry &e, const jubler::Subtitle &s, const Subtitles &subs) {
    e.setStartTime(Time(s.start));
    e.setFinishTime(Time(s.end));
    e.setStyle(subs.getStyleList().getStyleByName(s.style));   // unknown → Default; first: the tags reset to it
    AdvancedSubStation::setTaggedText(e, s.text);
    e.setMark(s.mark >= 0 && s.mark < SubEntry::MARK_COUNT ? s.mark : 0);
    e.setLayer(s.layer);
}

}  // namespace

PluginDocument::PluginDocument(Subtitles &subs, QList<int> selection, QString mediaPath)
    : subs_(subs), selection_(std::move(selection)), mediaPath_(std::move(mediaPath)) {}

int PluginDocument::count() const {
    return subs_.size();
}

jubler::Subtitle PluginDocument::at(int index) const {
    jubler::Subtitle s;
    if (index < 0 || index >= subs_.size()) return s;
    const SubEntryPtr e = subs_.get(index);
    s.start = e->getStartTime().toSeconds();
    s.end = e->getFinishTime().toSeconds();
    s.text = AdvancedSubStation::toTaggedText(*e);
    s.style = e->getStyle() ? e->getStyle()->getName() : QStringLiteral("Default");
    s.mark = e->getMark();
    s.layer = e->getLayer();
    return s;
}

void PluginDocument::replace(int index, const jubler::Subtitle &subtitle) {
    if (index < 0 || index >= subs_.size()) return;
    fill(*subs_.get(index), subtitle, subs_);
    changed_ = true;
}

void PluginDocument::insert(int index, const jubler::Subtitle &subtitle) {
    auto e = std::make_shared<SubEntry>();
    fill(*e, subtitle, subs_);
    subs_.insert(std::clamp(index, 0, subs_.size()), e);
    changed_ = true;
}

void PluginDocument::remove(int index) {
    if (index < 0 || index >= subs_.size()) return;
    subs_.remove(index);
    changed_ = true;
}

QStringList PluginDocument::styleNames() const {
    QStringList out;
    for (int i = 0; i < subs_.getStyleList().size(); ++i) out.append(subs_.getStyleList().getNameAt(i));
    return out;
}

float PluginDocument::fps() const {
    return subs_.getSubFile().getFPS();
}

QString PluginDocument::filePath() const {
    return subs_.getSubFile().getSaveFile();
}

// ---- adapters ----------------------------------------------------------------------------------

namespace {

// Every call into a plugin: an exception it throws is logged, never
// propagated into Jubler (it would end the program in the event loop).
template <typename F, typename R>
R guarded(const QString &what, F &&call, R fallback) {
    try {
        return call();
    } catch (const std::exception &e) {
        Debug::debug(QStringLiteral("Plugin error in %1: %2").arg(what, QString::fromUtf8(e.what())));
    } catch (...) {
        Debug::debug(QStringLiteral("Plugin error in %1").arg(what));
    }
    return fallback;
}

class PluginFormat : public SubFormat {
public:
    explicit PluginFormat(std::shared_ptr<jubler::Format> f) : f_(std::move(f)) {}
    QString getExtension() const override { return f_->extension(); }
    QString getName() const override { return f_->name(); }
    bool supportsFPS() const override { return f_->usesFps(); }
    QString classId() const override { return QStringLiteral("plugin:") + f_->name(); }
    std::shared_ptr<SubFormat> newInstance() const override {
        auto p = std::make_shared<PluginFormat>(f_);
        p->setFormatOrder(getFormatOrder());
        return p;
    }
    std::unique_ptr<Subtitles> parse(const QString &input, float fps, const QString &file, bool debug) override {
        Q_UNUSED(file);
        Q_UNUSED(debug);
        if (!guarded(f_->name(), [&] { return f_->canRead(input); }, false)) return nullptr;
        auto subs = std::make_unique<Subtitles>();
        PluginDocument doc(*subs);
        if (!guarded(f_->name(), [&] { return f_->read(input, fps, doc); }, false) || subs->isEmpty()) return nullptr;
        return subs;
    }
    bool produce(const Subtitles &subs, const QString &outfile, const MediaFile *media, SaveError &error) override {
        Q_UNUSED(media);
        if (!f_->canWrite()) {
            error = {SaveError::Io, __("The {0} format cannot be written", f_->name())};
            return false;
        }
        PluginDocument doc(const_cast<Subtitles &>(subs));   // write() gets it as const
        QByteArray bytes;
        const QString text = guarded(f_->name(), [&] { return f_->write(doc, FPS_); }, QString());
        if (text.isNull()) {
            error = {SaveError::Io, __("The {0} format could not write the subtitles", f_->name())};
            return false;
        }
        const EncodeResult r = encodeText(text, ENCODING_, bytes);
        if (r != EncodeResult::Ok) {
            error = {SaveError::Encoding, encodeErrorMessage(r, ENCODING_)};
            return false;
        }
        QString detail;
        if (writeFileAtomically(outfile, bytes, detail)) return true;
        error = {SaveError::Io, detail};
        return false;
    }

private:
    std::shared_ptr<jubler::Format> f_;
};

::Language toLanguage(const jubler::Language &l) {
    return ::Language{l.code.isEmpty() ? QString() : l.code, l.name};
}

class PluginTranslator : public ::Translator {
public:
    explicit PluginTranslator(std::shared_ptr<jubler::Translator> t) : t_(std::move(t)) {}
    QList<::Language> getSourceLanguages() const override { return map(t_->sourceLanguages()); }
    QList<::Language> getDestinationLanguagesFor(const ::Language &from) const override { return map(t_->targetLanguages(from.id)); }
    ::Language getDefaultSourceLanguage() const override { return pick(getSourceLanguages(), t_->defaultSource()); }
    ::Language getDefaultDestinationLanguage() const override { return pick(getDestinationLanguagesFor(getDefaultSourceLanguage()), t_->defaultTarget()); }
    QString getDefinition() const override { return t_->name(); }
    void configure(QWidget *parent) override {
        guarded(t_->name(), [&] {
            t_->configure(parent);
            return true;
        }, false);
    }
    QString isReady() override {
        const QString r = t_->isReady();
        return r.isEmpty() ? QString() : r;
    }
    bool translate(const QList<SubEntryPtr> &subs, const ::Language &from, const ::Language &to, TranslateProgress *progress) override {
        QStringList texts;
        for (const SubEntryPtr &e : subs) texts.append(e->getText());
        if (progress) progress->setValues(int(texts.size()), __("Translating to {0}", to.displayName));
        QString error;
        // The plugin runs in a worker thread (the API contract): the progress
        // and Cancel stay live on the GUI thread meanwhile.
        std::atomic<int> done{0};
        std::atomic<bool> cancelled{false};
        bool ok = false;
        QThread *worker = QThread::create([&]() {
            ok = guarded(t_->name(), [&] {
                return t_->translate(texts, from.id, to.id, [&](int d) {
                    done = d;
                    return !cancelled.load();
                }, &error);
            }, false);
        });
        QEventLoop loop;
        QObject::connect(worker, &QThread::finished, &loop, &QEventLoop::quit);
        QTimer poll;
        QObject::connect(&poll, &QTimer::timeout, &loop, [&]() {
            if (!progress) return;
            progress->updateProgress(done);
            if (progress->isCancelled()) cancelled = true;
        });
        poll.start(100);
        worker->start();
        loop.exec();
        worker->wait();
        delete worker;
        poll.stop();
        if (progress) progress->updateProgress(done);
        if (cancelled && ok) ok = false;
        if (!ok) {
            lastError_ = error.isEmpty() ? __("Translation failed") : error;
            return false;
        }
        for (int i = 0; i < subs.size() && i < texts.size(); ++i) subs[i]->setText(texts[i]);
        return true;
    }

private:
    static QList<::Language> map(const QList<jubler::Language> &in) {
        QList<::Language> out;
        for (const jubler::Language &l : in) out.append(toLanguage(l));
        return out;
    }
    static ::Language pick(const QList<::Language> &list, const QString &code) {
        for (const ::Language &l : list)
            if ((code.isEmpty() && l.isAuto()) || l.id == code) return l;
        return list.isEmpty() ? ::Language{} : list.first();
    }
    std::shared_ptr<jubler::Translator> t_;
};

class PluginSpellChecker : public ::SpellChecker {
public:
    explicit PluginSpellChecker(std::shared_ptr<jubler::SpellChecker> c) : c_(std::move(c)) {}
    QString getName() const override { return c_->name(); }
    void start() override {
        QString error;
        if (!guarded(c_->name(), [&] { return c_->start(&error); }, false)) throw SpellException(error.isEmpty() ? __("{0} is not available", c_->name()) : error);
    }
    QList<SpellError> checkSpelling(const QString &text) override {
        QList<SpellError> out;
        for (const jubler::Misspelling &m : guarded(c_->name(), [&] { return c_->check(text); }, QList<jubler::Misspelling>())) out.append(SpellError{m.position, m.word, m.suggestions});
        return out;
    }
    void stop() override { c_->stop(); }
    bool supportsInsert() const override { return c_->canAddWords(); }
    bool insertWord(const QString &w) override { return c_->addWord(w); }
    QList<SpellLanguage> getInstalledLanguages() const override {
        QList<SpellLanguage> out;
        for (const jubler::Language &l : c_->languages()) out.append(SpellLanguage{l.code, l.name, false});
        return out;
    }
    std::optional<SpellLanguage> getActiveLanguage() const override {
        const QString code = c_->activeLanguage();
        if (code.isEmpty()) return std::nullopt;
        for (const SpellLanguage &l : getInstalledLanguages())
            if (l.code == code) return l;
        return SpellLanguage{code, code, false};
    }
    void setActiveLanguage(const SpellLanguage &l) override { c_->setActiveLanguage(l.code); }

private:
    std::shared_ptr<jubler::SpellChecker> c_;
};

class PluginCommandLineTool : public ::Tool {
public:
    explicit PluginCommandLineTool(std::shared_ptr<jubler::CommandLineTool> t) : ::Tool(std::nullopt), t_(std::move(t)) {}
    QString getToolTitle() const override { return t_->name(); }
    QString getCommandOptionName() const override { return t_->name(); }
    QString getCommandLineHelp() const override { return t_->help(); }
    QStringList gatherToolTags() const override { return t_->parameterNames(); }
    QString executeParams(const QMap<QString, QString> &params, bool debug) override {
        Q_UNUSED(debug);
        Subtitles *subs = CommandLineContext::getSubtitles(QString());
        if (!subs) return QStringLiteral("No default subtitle file loaded");
        PluginDocument doc(*subs);
        const QString err = guarded(t_->name(), [&] { return t_->run(doc, params); }, QStringLiteral("The %1 tool failed").arg(t_->name()));
        return err.isEmpty() ? QString() : err;
    }

private:
    std::shared_ptr<jubler::CommandLineTool> t_;
};

// ---- registration -------------------------------------------------------------------------------

struct State {
    jubler::HostServices *services = nullptr;
    std::function<void(std::shared_ptr<jubler::SubtitleProvider>)> providerSink;
    QList<std::shared_ptr<jubler::Tool>> tools;
    QList<std::shared_ptr<jubler::EventListener>> listeners;
    QList<std::shared_ptr<jubler::PreferencesPage>> pages;
};

State &state() {
    static State s;
    return s;
}

class Registrar : public jubler::Registrar {
public:
    explicit Registrar(QString plugin) : plugin_(std::move(plugin)) {}
    jubler::HostServices &host() override { return *state().services; }
    void addTool(std::shared_ptr<jubler::Tool> tool) override {
        if (tool) state().tools.append(std::move(tool));
    }
    void addCommandLineTool(std::shared_ptr<jubler::CommandLineTool> tool) override {
        if (!tool) return;
        if (ToolRegistry::instance().findByCommandName(tool->name())) {
            Debug::debug(QStringLiteral("Plugin %1: command-line tool %2 already exists; ignored").arg(plugin_, tool->name()));
            return;
        }
        ToolRegistry::instance().add(std::make_shared<PluginCommandLineTool>(std::move(tool)));
    }
    void addFormat(std::shared_ptr<jubler::Format> format) override {
        if (!format) return;
        if (Availabilities::formats().findFromName(format->name())) {
            Debug::debug(QStringLiteral("Plugin %1: format %2 already exists; ignored").arg(plugin_, format->name()));
            return;
        }
        Availabilities::formats().add(std::make_shared<PluginFormat>(std::move(format)));
    }
    void addTranslator(std::shared_ptr<jubler::Translator> translator) override {
        if (!translator) return;
        AvailTranslators::registerBuiltin();   // the built-ins first (they register into an empty list only)
        AvailTranslators::all().append(std::make_shared<PluginTranslator>(std::move(translator)));
    }
    void addSpellChecker(std::shared_ptr<jubler::SpellChecker> checker) override {
        if (!checker) return;
        AvailSpellCheckers::registerBuiltin();
        AvailSpellCheckers::all().append(std::make_shared<PluginSpellChecker>(std::move(checker)));
    }
    void addEventListener(std::shared_ptr<jubler::EventListener> listener) override {
        if (listener) state().listeners.append(std::move(listener));
    }
    void addPreferencesPage(std::shared_ptr<jubler::PreferencesPage> page) override {
        if (page) state().pages.append(std::move(page));
    }
    void addSubtitleProvider(std::shared_ptr<jubler::SubtitleProvider> provider) override {
        if (provider && state().providerSink) state().providerSink(std::move(provider));
    }

private:
    QString plugin_;
};

}  // namespace

namespace PluginHost {

void setHostServices(jubler::HostServices *services) {
    state().services = services;
}

void setProviderSink(std::function<void(std::shared_ptr<jubler::SubtitleProvider>)> sink) {
    state().providerSink = std::move(sink);
}

bool registerPlugin(QObject *root, const QString &pluginName, QString *error) {
    auto *plugin = root ? qobject_cast<jubler::Plugin *>(root) : nullptr;
    if (!plugin) {
        if (error) *error = QStringLiteral("not a Jubler plugin");
        return false;
    }
    if (plugin->apiVersion() > jubler::PLUGIN_API_VERSION) {
        if (error) *error = QStringLiteral("needs a newer Jubler (plugin API %1, this Jubler has %2)").arg(plugin->apiVersion()).arg(jubler::PLUGIN_API_VERSION);
        return false;
    }
    if (!state().services) {
        if (error) *error = QStringLiteral("host services not ready");
        return false;
    }
    Registrar reg(pluginName);
    try {
        plugin->registerExtensions(reg);
    } catch (...) {
        if (error) *error = QStringLiteral("failed while registering its extensions");
        return false;
    }
    return true;
}

const QList<std::shared_ptr<jubler::Tool>> &tools() { return state().tools; }
const QList<std::shared_ptr<jubler::EventListener>> &eventListeners() { return state().listeners; }
const QList<std::shared_ptr<jubler::PreferencesPage>> &preferencesPages() { return state().pages; }

}  // namespace PluginHost
