/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include "core/formats/SimpleStyledTextSubFormat.h"

// SubRip (.srt).
class SubRip : public SimpleStyledTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("srt"); }
    QString getName() const override { return QStringLiteral("SubRip"); }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<SubRip>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override { return getPattern(); }
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    void initSaver(const Subtitles &subs, const MediaFile *media, QString &header) override;
    bool isEventCompact() override { return false; }

private:
    int counter_ = 0;
};
