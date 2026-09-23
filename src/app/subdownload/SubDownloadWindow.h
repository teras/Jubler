/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>
#include <QFutureWatcher>
#include <QList>
#include <QMap>
#include <memory>

#include "jubler/SubDownloadApi.h"

class MainWindow;
class QComboBox;
class QCheckBox;
class QLineEdit;
class QPushButton;
class QLabel;
class QTableWidget;

// The curated download languages, sorted by localized name and preceded by
// "<Any language>". Port of `DownloadLanguages`.
namespace DownloadLanguages {
struct Entry { QString id, name; };
QList<Entry> list();
}

// The modeless "Download subtitles — <document>" window of one main window.
// Port of `SubDownloadFrame`.
class SubDownloadWindow : public QDialog {
    Q_OBJECT
public:
    static void open(MainWindow *owner);

protected:
    bool eventFilter(QObject *watched, QEvent *e) override;
    bool event(QEvent *e) override;
    void closeEvent(QCloseEvent *e) override;
    void keyPressEvent(QKeyEvent *e) override;   // Escape does nothing (Java frame)

private:
    void elideStatus();
    explicit SubDownloadWindow(MainWindow *owner);
    ~SubDownloadWindow() override;
    std::shared_ptr<jubler::SubtitleProvider> provider() const;
    void onProviderChanged();
    void updateHashControls();
    void search();
    void download();
    void showResults(const QList<jubler::Candidate> &results);
    void setStatus(const QString &text, bool error);
    void reportFailure(const jubler::ProviderError &error);
    bool applyDownloaded(const jubler::Candidate &c, const jubler::DownloadData &data);

    MainWindow *owner_;
    QList<std::shared_ptr<jubler::SubtitleProvider>> providers_;
    QComboBox *providerC_, *languageC_;
    QPushButton *configureB_, *searchB_, *downloadB_;
    QCheckBox *hashC_;
    QLineEdit *query_;
    QTableWidget *table_;
    QLabel *status_;
    QString statusText_;   // full text; the label shows it elided
    QList<jubler::Candidate> results_;
    QMap<QString, jubler::DownloadData> cache_;   // provider "\n" handle → payload
    std::weak_ptr<jubler::SubtitleProvider> searching_;   // the provider of the running search
    std::shared_ptr<jubler::SubtitleProvider> providerNamed(const QString &name) const;
    bool userHashChoice_ = false;
    bool downloading_ = false;
    int searchSerial_ = 0;
    QFutureWatcher<void> *searchWatcher_ = nullptr;
};
