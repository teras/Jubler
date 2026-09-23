/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include "core/formats/TextSubFormat.h"

// Plain text (.txt): one line per entry, 2 s duration with 1 s gaps. The
// last-resort format: only tried when no structured format matches.
class PlainText : public AbstractTextSubFormat {
public:
    QString getExtension() const override { return QStringLiteral("txt"); }
    QString getName() const override { return QStringLiteral("PlainText"); }
    QString getExtendedName() const override { return QStringLiteral("Plain text"); }
    bool isLastResort() const override { return true; }
    bool supportsFPS() const override { return false; }
    std::shared_ptr<SubFormat> newInstance() const override { return std::make_shared<PlainText>(); }

protected:
    const QRegularExpression &getPattern() override;
    const QRegularExpression &getTestPattern() override { return getPattern(); }
    SubEntryPtr getSubEntry(const QRegularExpressionMatch &m) override;
    void appendSubEntry(const SubEntry &sub, QString &str) override;
    QString initLoader(const QString &input) override;
    QList<SubEntryPtr> loadSubtitles(const QString &input, bool debug) override;

private:
    double currentTime_ = 0;
};
