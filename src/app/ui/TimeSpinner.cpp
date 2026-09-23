/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/ui/TimeSpinner.h"

#include <QFontDatabase>

#include <QKeyEvent>
#include <QLineEdit>
#include <QRegularExpression>
#include <QTimer>

TimeSpinner::TimeSpinner(QWidget *parent) : QAbstractSpinBox(parent) {
    setKeyboardTracking(false);
    // Monospaced bold, right-aligned, as the Java editor field.
    QFont f = QFontDatabase::systemFont(QFontDatabase::FixedFont);
    f.setPointSizeF(font().pointSizeF());
    f.setBold(true);
    lineEdit()->setFont(f);
    lineEdit()->setAlignment(Qt::AlignRight);
    // Fixed mask as the Java field: digits are typed over, separators stay.
    lineEdit()->setInputMask(QStringLiteral("99:99:99,999"));
    lineEdit()->setText(value_.toString());
    connect(lineEdit(), &QLineEdit::cursorPositionChanged, this, [this](int, int) { updateSpeedFromCaret(); });
    connect(lineEdit(), &QLineEdit::editingFinished, this, &TimeSpinner::commitText);
    setMinimumWidth(QFontMetrics(f).horizontalAdvance(QStringLiteral("00:00:00,000")) + 40);
}

void TimeSpinner::setTimeValue(const Time &t) {
    suppress_ = true;
    value_ = t;
    refreshText();
    suppress_ = false;
}

void TimeSpinner::refreshText() {
    const int dot = dot_;
    lineEdit()->setText(value_.toString());
    QTimer::singleShot(0, this, [this, dot]() { lineEdit()->setCursorPosition(dot); });
}

void TimeSpinner::updateSpeedFromCaret() {
    const int pos = lineEdit()->cursorPosition();
    switch (pos) {
        case 1: speed_ = 3600; break;
        case 3: speed_ = 600; break;
        case 4: speed_ = 60; break;
        case 6: speed_ = 10; break;
        case 7: speed_ = 1; break;
        case 9: speed_ = 0.1; break;
        case 10: speed_ = 0.01; break;
        case 11: speed_ = 0.001; break;
        case 0: case 12: return;  // keep the previous step and caret
        default: break;           // separators: remember the caret, keep the step
    }
    dot_ = pos;
}

void TimeSpinner::stepBy(int steps) {
    commitText();
    Time t = value_;
    t.addTime(steps * speed_);
    if (t != value_) {
        value_ = t;
        refreshText();
        emit timeChanged(value_);
    }
}

QValidator::State TimeSpinner::validate(QString &input, int &) const {
    static const QRegularExpression full(QStringLiteral("^\\d{1,2}:\\d\\d:\\d\\d[,.]\\d{1,3}$"));
    static const QRegularExpression partial(QStringLiteral("^[\\d:,. ]*$"));   // ' ' = a cleared mask position
    if (full.match(input).hasMatch()) return QValidator::Acceptable;
    return partial.match(input).hasMatch() ? QValidator::Intermediate : QValidator::Invalid;
}

void TimeSpinner::fixup(QString &input) const {
    input = value_.toString();
}

void TimeSpinner::commitText() {
    static const QRegularExpression full(QStringLiteral("^(\\d{1,2}):(\\d\\d):(\\d\\d)[,.](\\d{1,3})$"));
    const QRegularExpressionMatch m = full.match(lineEdit()->text());
    if (!m.hasMatch()) {
        refreshText();
        return;
    }
    Time t(m.captured(1), m.captured(2), m.captured(3), m.captured(4));
    if (!t.isValid()) {
        refreshText();
        return;
    }
    if (t != value_) {
        value_ = t;
        emit timeChanged(value_);
    }
}

void TimeSpinner::keyPressEvent(QKeyEvent *e) {
    switch (e->key()) {
        case Qt::Key_PageUp: emit navigation(PREVIOUS_LOCK); e->accept(); return;
        case Qt::Key_PageDown: emit navigation(NEXT_LOCK); e->accept(); return;
        case Qt::Key_Return: case Qt::Key_Enter: commitText(); emit navigation(NEXT_TIME_SPINNER); e->accept(); return;
        case Qt::Key_Up: stepBy(1); e->accept(); return;
        case Qt::Key_Down: stepBy(-1); e->accept(); return;
        default: break;
    }
    QAbstractSpinBox::keyPressEvent(e);
}

void TimeSpinner::focusInEvent(QFocusEvent *e) {
    QAbstractSpinBox::focusInEvent(e);
    const int dot = dot_;
    QTimer::singleShot(0, this, [this, dot]() { lineEdit()->setCursorPosition(dot); });
}
