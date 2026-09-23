/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

#include <QJsonObject>
#include <QList>
#include <QMap>
#include <QSet>
#include <QString>
#include <QStringList>
#include <functional>
#include <memory>
#include <optional>

#include "core/formats/SubFormat.h"

// How a tool's output is applied back. Port of `OutputMode`.
enum class OutputMode { REPLACE, PATCH_TEXT, PATCH_TIMING, PATCH_BOTH };
QString outputModeName(OutputMode m);      // enum name
QString outputModeLabel(OutputMode m);     // translated label
OutputMode outputModeFromName(const QString &name);
inline bool isPatch(OutputMode m) { return m != OutputMode::REPLACE; }
inline bool patchText(OutputMode m) { return m == OutputMode::PATCH_TEXT || m == OutputMode::PATCH_BOTH; }
inline bool patchTiming(OutputMode m) { return m == OutputMode::PATCH_TIMING || m == OutputMode::PATCH_BOTH; }

// A typed recipe parameter. Port of `RecipeParam`.
struct RecipeParam {
    enum class Type { TEXTBOX, COMBOBOX, CHECKBOX, PATH, LANGUAGE, WINDOW, VIDEO_SUBTITLE, SECRET };
    static QString typeName(Type t);         // enum name
    static QString typeLabel(Type t);        // translated label
    static QString typeDescription(Type t);  // one-sentence help
    static Type typeFromName(const QString &name);
    static QList<Type> allTypes();

    QString key = QStringLiteral("param");
    QString label, help;
    Type type = Type::TEXTBOX;
    QString defaultValue;   // Secret: the encrypted value; Checkbox: "true"/"false"
    QString choices;        // "|"-separated
    bool folder = false;
    QString checkedValue;
    QString field = QStringLiteral("index");   // index|id|language
    QString accept = QStringLiteral("any");    // text|image|any

    QString displayLabel() const { return label.isEmpty() ? key : label; }
    QStringList getChoiceList() const;
    bool isPerRun() const { return type == Type::WINDOW || type == Type::VIDEO_SUBTITLE; }
    bool isSecret() const { return type == Type::SECRET; }
    QString toString() const { return displayLabel() + QStringLiteral(" (%") + key + QLatin1Char(')'); }
    // Null when valid, else the (translated) error.
    static QString validateKey(const QString &key, const QSet<QString> &existing);
    QJsonObject toJson(bool forSharing) const;
    static RecipeParam fromJson(const QJsonObject &o);
};

// An external command run against the document. Port of `Recipe`.
class Recipe {
public:
    static const QString DEFAULT_COMMAND;   // "%x --input %i --output %o"
    Recipe();
    explicit Recipe(const QString &name);

    QString name, description, url, path, command;
    bool outputFolder = false;
    OutputMode outputMode = OutputMode::REPLACE;
    QList<RecipeParam> params;

    SubFormatPtr getFormat() const;   // the wire format (default: srt)
    void setFormat(const SubFormatPtr &f) { format_ = f; }
    bool hasFormat() const { return format_ != nullptr; }
    static SubFormatPtr defaultFormat();
    QSet<QString> keysExcept(const RecipeParam *self) const;
    QJsonObject toJson(bool forSharing) const;
    QString toJsonString(bool forSharing) const;
    static Recipe fromJson(const QJsonObject &o);
    static std::optional<Recipe> fromJsonString(const QString &json);
    void copyFrom(const Recipe &other) { *this = fromJson(other.toJson(false)); }
    bool hasSecretValue() const;

private:
    SubFormatPtr format_;
};

// The recipe registry with its persistence. Port of `Recipes`.
namespace Recipes {
QList<Recipe> &getList();
void load();
void save();
QString saveToFile(const Recipe &r, const QString &path);            // null on success
QList<Recipe> loadFromFile(const QString &path, QString *error);      // one object or an array
}  // namespace Recipes

// Last per-run values, keyed by recipe name. Port of `RecipeValues`.
namespace RecipeValues {
QMap<QString, QString> get(const QString &recipeName);
void put(const QString &recipeName, const QMap<QString, QString> &values);
}  // namespace RecipeValues

// The shared online catalog. Port of `RecipeCatalog`.
namespace RecipeCatalog {
const QString &url();
QString cachePath();
std::optional<QList<Recipe>> cached();
// Download (blocking, ≤5 s); on failure the cached list.
std::optional<QList<Recipe>> fetch();
std::optional<QList<Recipe>> parse(const QByteArray &data);
}  // namespace RecipeCatalog

// Executable resolution and the augmented PATH. Port of `RecipeResolver`.
namespace RecipeResolver {
bool isAvailable(const Recipe &r);
QString resolve(const QString &path);   // null when not found
QStringList searchDirs();
QString augmentedPath();
// Start computing the login-shell PATH in the background (once).
void startLoginShellProbe();
}  // namespace RecipeResolver

// At-rest protection of SECRET values with a session PIN. Port of
// `RecipeSecrets`; the prompts are injected by the app.
namespace RecipeSecrets {
struct Prompts {
    std::function<QString()> askExisting;   // "Enter your PIN…"; null when cancelled
    // Twice, validated; null when cancelled. `change` is true from changePin().
    std::function<QString(bool change)> askNew;
    std::function<void(const QString &msg, bool change)> warn;
};
void setPrompts(Prompts p);
bool anySecretStored();
QString resolvePin();   // null when unavailable
void forgetPin();
QString encrypt(const QString &plain);
QString decrypt(const QString &blob);      // warns on failure, ""
std::optional<QString> tryDecrypt(const QString &blob);
bool recodeForSecretChange(RecipeParam &param, bool nowSecret);
bool changePin();   // true when the PIN was changed
}  // namespace RecipeSecrets
