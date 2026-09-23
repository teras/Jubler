/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QDialog>
#include <QMap>
#include <QWidget>
#include <memory>

#include "app/media/MediaProbe.h"
#include "core/externals/Recipe.h"
#include "core/externals/RecipeExecutor.h"

class MainWindow;
class QLineEdit;
class QPlainTextEdit;
class QComboBox;
class QCheckBox;
class QLabel;
class QListWidget;
class QPushButton;
class QProgressBar;
class QStackedWidget;
class QToolButton;
class TimeFullSelection;

// An "i" button showing a help text. Port of `InfoButton`.
QToolButton *makeInfoButton(QWidget *parent, const QString &title, std::function<QString()> text);
// A password field with an eye toggle. Port of `SecretField`.
QWidget *makeSecretField(QWidget *parent, QLineEdit *&field);

// Detail editor of one parameter. Port of `JParamDetail`.
class ParamDetail : public QWidget {
    Q_OBJECT
public:
    explicit ParamDetail(QWidget *parent = nullptr);
    void bind(Recipe *recipe, RecipeParam *param);   // null = nothing selected
    void flush();                                    // commit a pending secret edit
    RecipeParam *param() const { return param_; }

signals:
    void keyEdited();

private:
    void populate();
    void onTypeChanged(int index);
    Recipe *recipe_ = nullptr;
    RecipeParam *param_ = nullptr;
    bool loading_ = false;
    QWidget *header_;
    QLineEdit *key_, *label_, *help_;
    QComboBox *type_;
    QStackedWidget *cards_;
    QLineEdit *textDefault_, *comboChoices_, *comboDefault_, *checkValue_, *pathDefault_, *langDefault_, *secret_;
    QCheckBox *checkDefault_, *pathFolder_;
    QComboBox *vsShow_, *vsEmit_;
    bool secretDirty_ = false;
};

// The "Edit recipe" dialog. Port of `JRecipeEditor`.
class RecipeEditorDialog : public QDialog {
    Q_OBJECT
public:
    RecipeEditorDialog(QWidget *parent, Recipe &recipe);
    bool accepted() const { return accepted_; }

protected:
    void reject() override;

private:
    void refreshStatus();
    void refreshParams(const QString &selectKey);
    void onOk();
    Recipe &recipe_;
    Recipe snapshot_;
    bool accepted_ = false;
    QLineEdit *name_, *url_, *path_, *command_;
    QPlainTextEdit *description_;
    QLabel *status_;
    QCheckBox *outputFolder_;
    QComboBox *format_, *result_;
    QListWidget *params_;
    QPushButton *remove_;
    ParamDetail *detail_;
};

// The per-run dialog. Port of `JRecipeRunDialog`.
class RecipeRunDialog : public QDialog {
    Q_OBJECT
public:
    RecipeRunDialog(MainWindow *window, const Recipe &recipe, const QList<SubtitleStreamInfo> &streams);
    static bool needsPrompt(const Recipe &recipe, const MainWindow *window);
    static bool isEmptyDocument(const MainWindow *window);
    QMap<QString, QString> getValues() const;
    std::optional<QList<SubEntryPtr>> getScope() const;
    QMap<QString, const Subtitles *> getWindowSelections() const;
    bool getReplaceInCurrent() const;
    // Values with the author defaults only (no dialog).
    static QMap<QString, QString> defaultValues(const Recipe &recipe);

private:
    struct Row {
        RecipeParam param;
        QWidget *widget = nullptr;
        QLineEdit *field = nullptr;
        QComboBox *combo = nullptr;
        QCheckBox *check = nullptr;
        QList<MainWindow *> windows;
        QList<SubtitleStreamInfo> streams;
    };
    MainWindow *window_;
    Recipe recipe_;
    QList<Row> rows_;
    TimeFullSelection *scope_ = nullptr;
    QCheckBox *replace_ = nullptr;
};

// The lifecycle dialog of a running recipe. Port of `JRecipeProgress`.
class RecipeProgressDialog : public QDialog {
    Q_OBJECT
public:
    RecipeProgressDialog(MainWindow *window, const RecipeRun &run);
    void reject() override;   // Esc / window close: Cancel while running

private:
    void onFinished(bool success, const QString &message);
    void applyResult(std::shared_ptr<Subtitles> result, const RecipeRun &run);
    MainWindow *window_;
    RecipeExecutor *executor_;
    QLabel *status_;
    QProgressBar *bar_;
    QPlainTextEdit *log_;
    QPushButton *toggleLog_, *button_;
    bool finished_ = false;
    QString name_;
};

// The online catalog picker. Port of `JCatalogChooser`.
class CatalogChooser : public QDialog {
    Q_OBJECT
public:
    explicit CatalogChooser(QWidget *parent, const QList<Recipe> &recipes);
    void setRecipes(const QList<Recipe> &recipes);
    QList<Recipe> choose();   // exec(); empty when cancelled

private:
    QList<Recipe> recipes_;
    QListWidget *list_;
    QPlainTextEdit *preview_;
    QToolButton *globe_;
    bool accepted_ = false;
};

namespace RecipeUi {
// Install the PIN prompts (once, at startup).
void installSecretPrompts();
// Tools ▸ Externals ▸ <recipe>.
void runRecipe(MainWindow *window, const Recipe &recipe);
// Rebuild the Externals menu of a window from the registry.
void buildExternalsMenu(MainWindow *window, QMenu *menu);
}  // namespace RecipeUi
