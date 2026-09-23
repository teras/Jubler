/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/dialogs/SubFileDialog.h"

#include <QDir>
#include <QFileDialog>
#include <QFileInfo>
#include <QMessageBox>

#include "core/formats/SubFormat.h"
#include "core/i18n/I18N.h"
#include "core/os/FileCommunicator.h"
#include "core/os/SystemDependent.h"
#include "core/subs/Subtitles.h"

namespace {

QString startDir() {
    const QString d = FileCommunicator::getDefaultDirPath();
    const QFileInfo fi(d);
    return fi.isDir() && fi.isReadable() ? d : QDir::homePath();
}

void rememberDir(const QString &file) {
    const QFileInfo fi(file);
    FileCommunicator::setDefaultDir(fi.isDir() ? fi.filePath() : fi.path());
}

}  // namespace

namespace SubFileDialog {

std::optional<SubFile> getLoadFile(QWidget *parent, AppMediaFile *) {
    QStringList filters;
    QStringList all;
    for (const SubFormatPtr &f : Availabilities::formats().getFormats()) {
        filters.append(f->getName() + QStringLiteral(" (*.") + f->getExtension() + QLatin1Char(')'));
        all.append(QStringLiteral("*.") + f->getExtension());
    }
    filters.prepend(__("All subtitle files") + QStringLiteral(" (") + all.join(QLatin1Char(' ')) + QLatin1Char(')'));
    filters.append(__("All files") + QStringLiteral(" (*)"));
    QFileDialog dlg(parent, __("Load Subtitles"), startDir());
    dlg.setAcceptMode(QFileDialog::AcceptOpen);
    dlg.setFileMode(QFileDialog::ExistingFile);
    dlg.setNameFilters(filters);
    dlg.setLabelText(QFileDialog::Accept, __("Load Subtitles"));
    if (dlg.exec() != QDialog::Accepted || dlg.selectedFiles().isEmpty()) return std::nullopt;
    const QString file = dlg.selectedFiles().first();
    rememberDir(file);
    return SubFile(file, SubFile::EXTENSION_GIVEN);
}

std::optional<SubFile> getSaveFile(QWidget *parent, Subtitles *subs, AppMediaFile *) {
    if (!subs) return std::nullopt;
    const SubFile &current = subs->getSubFile();
    const SubFormatPtr fmt = current.getFormat();
    const QString ext = fmt ? fmt->getExtension() : QStringLiteral("srt");
    const QString filter = (fmt ? fmt->getName() : __("Subtitles")) + QStringLiteral(" (*.") + ext + QLatin1Char(')');
    // The last dialog directory with the document's name (Java behaviour).
    QString suggested = startDir() + QLatin1Char('/') + QFileInfo(current.getStrippedFile()).fileName() + QLatin1Char('.') + ext;
    while (true) {
        QFileDialog dlg(parent, __("Save Subtitles"), suggested, filter);
        dlg.setAcceptMode(QFileDialog::AcceptSave);
        dlg.setFileMode(QFileDialog::AnyFile);
        dlg.setLabelText(QFileDialog::Accept, __("Save Subtitles"));
        // The overwrite check is ours (extension forced first) except under the
        // portal, which already confirmed it.
        if (!SystemDependent::isFlatpak()) dlg.setOption(QFileDialog::DontConfirmOverwrite);
        if (dlg.exec() != QDialog::Accepted || dlg.selectedFiles().isEmpty()) return std::nullopt;
        const QString file = dlg.selectedFiles().first();
        SubFile out(current);
        out.setFile(file);
        if (SystemDependent::isFlatpak()) {
            rememberDir(file);
            return out;
        }
        out.updateFileByType();
        if (QFileInfo::exists(out.getSaveFile())) {
            const auto r = QMessageBox::warning(parent, __("Confirm Overwrite"), __("File already exists. Do you want to overwrite it?"), QMessageBox::Yes | QMessageBox::No);
            if (r != QMessageBox::Yes) {
                suggested = out.getSaveFile();
                continue;
            }
        }
        rememberDir(file);
        return out;
    }
}

}  // namespace SubFileDialog
