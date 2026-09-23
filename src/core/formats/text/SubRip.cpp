/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/formats/text/SubRip.h"

#include <QRegularExpression>

namespace {
// The numeric keypad positions of "{\anN}" (1 = bottom left … 9 = top right).
const Direction AN_DIRECTIONS[9] = {Direction::BOTTOMLEFT, Direction::BOTTOM, Direction::BOTTOMRIGHT, Direction::LEFT, Direction::CENTER,
                                    Direction::RIGHT, Direction::TOPLEFT, Direction::TOP, Direction::TOPRIGHT};
}  // namespace

const QRegularExpression &SubRip::getPattern() {
    // Counter line, "H:MM:SS,mmm --> HH:MM:SS,mmm", optional "X1:…" coordinates
    // (dropped), then the text up to a blank line. An empty text is accepted
    // when the next block (counter + timing) follows directly. A separator
    // line holding only spaces or tabs counts as blank.
    static const QRegularExpression pat(
        QStringLiteral("(?s)(\\d+)") + sp + nl + QStringLiteral("(\\d{1,2}):(\\d\\d):(\\d\\d),(\\d\\d?\\d?)") + sp
        + QStringLiteral("-->") + sp + QStringLiteral("(\\d\\d):(\\d\\d):(\\d\\d),(\\d\\d?\\d?)") + sp
        + QStringLiteral("(X1:\\d.*?)??") + nl
        + QStringLiteral("(.*?)(?:") + nl + sp + nl + QStringLiteral("|") + nl
        + QStringLiteral("(?=\\d+[ \\t]*\\r?\\n\\d{1,2}:\\d\\d:\\d\\d,)|\\z)"));
    return pat;
}

SubEntryPtr SubRip::getSubEntry(const QRegularExpressionMatch &m) {
    const Time start(m.captured(2), m.captured(3), m.captured(4), m.captured(5));
    const Time finish(m.captured(6), m.captured(7), m.captured(8), m.captured(9));
    // Files in the wild: "{\an8}" placement tags (as players read them) and
    // "<br>" line breaks.
    static const QRegularExpression an(QStringLiteral("\\{\\\\an([1-9])\\}"));
    static const QRegularExpression br(QStringLiteral("<br\\s*/?>"), QRegularExpression::CaseInsensitiveOption);
    QString text = m.captured(11);
    int placement = 0;
    if (const auto a = an.match(text); a.hasMatch()) placement = a.captured(1).toInt();
    text.remove(an);
    text.replace(br, QStringLiteral("\n"));
    SubEntryPtr entry = makeSubEntry(start, finish, text);
    if (placement > 0 && AN_DIRECTIONS[placement - 1] != Direction::BOTTOM)
        entry->setOverStyle(StyleType::DIRECTION, StyleValue(AN_DIRECTIONS[placement - 1]), 0, entry->getText().length());
    return entry;
}

void SubRip::appendSubEntry(const SubEntry &sub, QString &str) {
    str += QString::number(counter_++);
    str += QLatin1Char('\n');
    str += sub.getStartTime().getSeconds(QLatin1Char(','));
    str += QLatin1String(" --> ");
    str += sub.getFinishTime().getSeconds(QLatin1Char(','));
    str += QLatin1Char('\n');
    // A placement other than the bottom goes out as "{\anN}" (read by the players).
    Direction d = sub.getStyle() ? sub.getStyle()->direction() : Direction::BOTTOM;
    if (const auto v = leadingOverride(sub, StyleType::DIRECTION); v && std::holds_alternative<Direction>(*v)) d = std::get<Direction>(*v);
    if (d != Direction::BOTTOM)
        for (int i = 0; i < 9; ++i)
            if (AN_DIRECTIONS[i] == d) str += QStringLiteral("{\\an%1}").arg(i + 1);
    str += rebuildSubText(sub);
    str += QLatin1String("\n\n");
}

void SubRip::initSaver(const Subtitles &subs, const MediaFile *media, QString &header) {
    SimpleStyledTextSubFormat::initSaver(subs, media, header);
    counter_ = 1;
}
