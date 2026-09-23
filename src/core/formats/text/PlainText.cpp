/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/text/PlainText.h"

const QRegularExpression &PlainText::getPattern() {
    static const QRegularExpression pat(QStringLiteral("(.*?)") + nl);
    return pat;
}

SubEntryPtr PlainText::getSubEntry(const QRegularExpressionMatch &m) {
    const Time start(currentTime_);
    currentTime_ += 2;
    const Time finish(currentTime_);
    currentTime_ += 1;
    return std::make_shared<SubEntry>(start, finish, m.captured(1));
}

QString PlainText::initLoader(const QString &input) {
    currentTime_ = 0;
    return AbstractTextSubFormat::initLoader(input);
}

QList<SubEntryPtr> PlainText::loadSubtitles(const QString &input, bool debug) {
    QList<SubEntryPtr> entries = AbstractTextSubFormat::loadSubtitles(input, debug);
    // The decoder's artificial trailing line terminators must not become
    // empty subtitles.
    while (!entries.isEmpty() && entries.last()->getText().isEmpty())
        entries.removeLast();
    return entries;
}

void PlainText::appendSubEntry(const SubEntry &sub, QString &str) {
    str += sub.getText();
    str += QLatin1Char('\n');
}
