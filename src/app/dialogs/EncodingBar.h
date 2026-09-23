/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QWidget>

#include "core/formats/SubFormat.h"

class QComboBox;
class QLabel;
class QToolButton;
class QMenu;
class AppMediaFile;
class Subtitles;

// A charset picker: every codec the runtime knows plus a "presets" popup of
// common encodings grouped by region. Port of `JEncodingChooser`.
class EncodingChooser : public QWidget {
    Q_OBJECT
public:
    explicit EncodingChooser(QWidget *parent = nullptr);
    QString encoding() const;
    // Select without notifying; unknown names fall back to US-ASCII.
    void setEncoding(const QString &enc);
    static QString canonicalCodecName(const QString &spec);
    static QString displayLabel(QString label);

signals:
    void encodingChanged(const QString &enc);

private:
    void buildPresets();
    QComboBox *combo_;
    QToolButton *presets_;
    QMenu *menu_;
    bool updating_ = false;
};

// The editable FPS combo with the standard rates and the "from the video"
// button. Port of `JRateChooser`.
class RateChooser : public QWidget {
    Q_OBJECT
public:
    explicit RateChooser(QWidget *parent = nullptr);
    void setDataFiles(AppMediaFile *mfile, Subtitles *subs);
    void setFPS(float fps);
    float fps() const;   // the default FPS when unparsable
    void setChooserEnabled(bool enabled);

signals:
    void fpsChanged();
    void mediaChanged();

private:
    QComboBox *combo_;
    QToolButton *fromVideo_;
    AppMediaFile *mfile_ = nullptr;
    Subtitles *subs_ = nullptr;
    bool updating_ = false;
    bool commitPending_ = false;
};

// The bar above the table: encoding, FPS and format of the document. Port of
// `JEncodingBar`.
class EncodingBar : public QWidget {
    Q_OBJECT
public:
    explicit EncodingBar(QWidget *parent = nullptr);
    void showFor(const QString &encoding, AppMediaFile *mfile, Subtitles *subs);
    QString encoding() const;
    float fps() const;
    QString formatName() const;

signals:
    void reloadRequested();
    void formatSelected(const SubFormatPtr &format);
    void mediaChanged();

private:
    EncodingChooser *encoding_;
    RateChooser *rate_;
    QLabel *fpsLabel_;
    QComboBox *format_;
    bool updating_ = false;
};
