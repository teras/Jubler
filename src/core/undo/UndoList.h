/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QList>
#include <QString>
#include <memory>

#include "core/subs/Subtitles.h"

// One undo step: a whole-document snapshot with a label. Port of `UndoEntry`.
class UndoEntry {
public:
    UndoEntry(const Subtitles &subs, const QString &name) : name_(name), subs_(std::make_unique<Subtitles>(subs)) {}
    QString getName() const { return name_; }
    // Swap the snapshot with the given document, returning the old snapshot
    // (the same entry serves as undo and redo record).
    std::unique_ptr<Subtitles> flipSubtitles(std::unique_ptr<Subtitles> current) {
        std::unique_ptr<Subtitles> old = std::move(subs_);
        subs_ = std::move(current);
        return old;
    }

private:
    QString name_;
    std::unique_ptr<Subtitles> subs_;
};

// What the undo list needs from its window. Port of the JubFrame calls made
// by `UndoList`.
class UndoHost {
public:
    virtual ~UndoHost() = default;
    // null text → disabled with the plain "Undo"/"Redo" label.
    virtual void setDoText(const QString &text, bool isUndo) = 0;
    virtual void resetUndoMark() = 0;
    virtual void setUnsaved(bool unsaved) = 0;
    virtual void autoHideEncodingBar() = 0;
    virtual void showInfo() = 0;
    // Install `subs` as the window's document, returning the previous one.
    virtual std::unique_ptr<Subtitles> swapSubtitles(std::unique_ptr<Subtitles> subs) = 0;
    virtual void setSelectedRows(const QList<int> &rows, bool updateVisuals) = 0;
};

// Unlimited undo/redo stacks of document snapshots. Port of `UndoList`.
class UndoList {
public:
    explicit UndoList(UndoHost *host) : host_(host) {}

    void addUndo(std::unique_ptr<UndoEntry> entry);
    void addUndo(const Subtitles &subs, const QString &name) { addUndo(std::make_unique<UndoEntry>(subs, name)); }
    // Apply the top of the undo (isUndo) or redo stack.
    void applyDoCommand(bool isUndo, const QList<int> &selectedRows);
    void setSaveMark();
    void invalidateSaveMark();
    int size() const { return undo_.size(); }
    bool canUndo() const { return !undo_.isEmpty(); }
    bool canRedo() const { return !redo_.isEmpty(); }
    QString undoName() const { return undo_.isEmpty() ? QString() : undo_.last()->getName(); }
    QString redoName() const { return redo_.isEmpty() ? QString() : redo_.last()->getName(); }

private:
    UndoHost *host_;
    QList<std::shared_ptr<UndoEntry>> undo_;
    QList<std::shared_ptr<UndoEntry>> redo_;
    int unsavedPos_ = 0;
};
