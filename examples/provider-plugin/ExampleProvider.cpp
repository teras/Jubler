/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include <QObject>

#include "jubler/SubDownloadApi.h"

// A keyless provider that shows how the API fits together.
class ExampleProvider : public jubler::SubtitleProvider {
public:
    explicit ExampleProvider(jubler::HostServices *host) : host_(host) {}
    QString getName() const override { return QStringLiteral("Example"); }
    bool needsConfiguration() const override { return false; }
    QString isReady() override { return QString(); }
    QString ensureReady(QWidget *) override { return QString(); }
    void configure(QWidget *) override {}
    // Failures are returned, never thrown: jubler::guarded() turns a
    // ProviderException thrown inside the plugin into the result's error.
    jubler::ProviderResult<QList<jubler::Candidate>> search(const jubler::SearchRequest &req) override {
        return jubler::guarded([&] { return find(req); });
    }
    jubler::ProviderResult<jubler::DownloadData> download(const jubler::Candidate &) override {
        return jubler::DownloadData{QByteArrayLiteral("1\n00:00:01,000 --> 00:00:03,000\nHello from the example plugin\n"), QStringLiteral("text/plain")};
    }
    void cancelSearch() override {}

private:
    QList<jubler::Candidate> find(const jubler::SearchRequest &req) {
        if (req.query.trimmed().isEmpty()) throw jubler::ProviderException(jubler::ProviderException::Kind::PARSE, host_->tr("Enter something to search for."));
        const auto q = host_->parseQuery(req.query);
        jubler::Candidate c;
        c.provider = getName();
        c.releaseName = q.title + QStringLiteral(" (example)");
        c.language = req.languageCode.isEmpty() ? QStringLiteral("en") : req.languageCode;
        c.handle = QStringLiteral("example");
        return {c};
    }
    jubler::HostServices *host_;
};

class ExamplePlugin : public QObject, public jubler::SubtitleProviderPlugin {
    Q_OBJECT
    Q_PLUGIN_METADATA(IID JublerSubtitleProviderPlugin_iid FILE "plugin.json")
    Q_INTERFACES(jubler::SubtitleProviderPlugin)
public:
    QList<std::shared_ptr<jubler::SubtitleProvider>> createProviders(jubler::HostServices *host) override {
        return {std::make_shared<ExampleProvider>(host)};
    }
};

#include "ExampleProvider.moc"
