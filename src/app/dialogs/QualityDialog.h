/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>

// "Quality settings": the mark colour of quality violations and the
// thresholds; every change is applied immediately. Port of `JQuality`.
class QualityDialog : public QDialog {
    Q_OBJECT
public:
    explicit QualityDialog(QWidget *parent = nullptr);
};
