/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/undo/UndoList.h"

void UndoList::addUndo(std::unique_ptr<UndoEntry> entry) {
    if (!entry)
        return;
    undo_.append(std::shared_ptr<UndoEntry>(std::move(entry)));
    host_->setDoText(undo_.last()->getName(), true);
    redo_.clear();
    host_->setDoText(QString(), false);
    host_->resetUndoMark();
    host_->setUnsaved(true);
    host_->autoHideEncodingBar();  // first edit: drop the transient encoding bar and its byte buffer
    host_->showInfo();
}

void UndoList::applyDoCommand(bool isUndo, const QList<int> &selectedRows) {
    QList<std::shared_ptr<UndoEntry>> &source = isUndo ? undo_ : redo_;
    QList<std::shared_ptr<UndoEntry>> &dest = isUndo ? redo_ : undo_;
    if (source.isEmpty())
        return;
    std::shared_ptr<UndoEntry> entry = source.takeLast();
    dest.append(entry);
    host_->setDoText(source.isEmpty() ? QString() : source.last()->getName(), isUndo);
    host_->setDoText(entry->getName(), !isUndo);
    host_->setUnsaved(undo_.size() != unsavedPos_);
    // Swap the window document with the snapshot.
    std::unique_ptr<Subtitles> placeholder = std::make_unique<Subtitles>();
    std::unique_ptr<Subtitles> current = host_->swapSubtitles(std::move(placeholder));
    std::unique_ptr<Subtitles> restored = entry->flipSubtitles(std::move(current));
    host_->swapSubtitles(std::move(restored));
    host_->setSelectedRows(selectedRows, true);
}

void UndoList::setSaveMark() {
    unsavedPos_ = undo_.size();
    host_->setUnsaved(false);
}

void UndoList::invalidateSaveMark() {
    unsavedPos_ = -1;
    host_->setUnsaved(true);
}
