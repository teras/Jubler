/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// Spell-check tokenizer, the encryption helper and the translator plumbing.
#include "TestSupport.h"

#include "core/os/Encryption.h"
#include "core/spell/HunspellChecker.h"
#include "core/translate/Translator.h"
#include "core/update/VersionData.h"
#include <QJsonArray>
#include <QJsonDocument>

static void testTokenizer() {
    auto words = HunspellDicts::tokenize(QStringLiteral("Hello, <i>world</i>! Don't {\\an8}stop 123 it’s"));
    QStringList got;
    for (const auto &[pos, w] : words) got.append(QString::number(pos) + QLatin1Char(':') + w);
    CHECK_EQ(got.join(QLatin1Char(' ')), QStringLiteral("0:Hello 10:world 21:Don't 33:stop 42:it’s"), "tokens skip tags, keep apostrophes inside words");
    words = HunspellDicts::tokenize(QStringLiteral("open <tag never closed"));
    CHECK_EQ(int(words.size()), 1, "an unterminated tag runs to the end");
    words = HunspellDicts::tokenize(QStringLiteral("rock'n'roll it' 'quoted'"));
    got.clear();
    for (const auto &[pos, w] : words) got.append(w);
    CHECK_EQ(got.join(QLatin1Char(' ')), QStringLiteral("rock'n'roll it quoted"), "apostrophes only between letters");
}

static void testEncryption() {
    const auto enc = Encryption::encrypt(QStringLiteral("secret κλειδί"), QStringLiteral("pin1234"));
    CHECK(enc.has_value(), "encrypt succeeds");
    const auto dec = Encryption::decrypt(*enc, QStringLiteral("pin1234"));
    CHECK(dec.has_value() && *dec == QStringLiteral("secret κλειδί"), "round trip");
    CHECK(!Encryption::decrypt(*enc, QStringLiteral("wrong")).has_value(), "wrong password fails");
    CHECK(!Encryption::decrypt(QStringLiteral("abc"), QStringLiteral("pin1234")).has_value(), "garbage fails");
    const auto enc2 = Encryption::encrypt(QStringLiteral("secret κλειδί"), QStringLiteral("pin1234"));
    CHECK(enc2.has_value() && *enc2 != *enc, "random salt/iv");
}

static void testAzureUrl() {
    class Probe : public AzureTranslator {
    public:
        QString url(const Language &f, const Language &t) const { return getTranslationURL(f, t); }
    } azure;
    Prefs::set(QStringLiteral("azure.translation.baseurl"), QStringLiteral("https://api.example/translate?api-version=3.0"));
    CHECK_EQ(azure.url({QString(), QStringLiteral("auto")}, {QStringLiteral("el"), QStringLiteral("Greek")}), QStringLiteral("https://api.example/translate?api-version=3.0&to=el"), "auto source");
    CHECK_EQ(azure.url({QStringLiteral("en"), QStringLiteral("English")}, {QStringLiteral("el"), QStringLiteral("Greek")}), QStringLiteral("https://api.example/translate?api-version=3.0&from=en&to=el"), "from and to joined with &");
    CHECK_EQ(int(azure.getSourceLanguages().size()), 42, "41 languages plus auto");
    CHECK_EQ(int(azure.getDestinationLanguagesFor(azure.getDefaultSourceLanguage()).size()), 41, "destinations exclude auto");
}

static void testVersions() {
    auto v = [](const char *s) { return VersionData::parse(QString::fromUtf8(s)); };
    CHECK(v("v10.0.1").isNewerThan(v("10.0.0")), "patch newer");
    CHECK(!v("10.0.0").isNewerThan(v("10.0.0")), "equal");
    CHECK(!v("10.1.0-beta.1").isNewerThan(v("10.1.0")), "final beats beta");
    CHECK(v("10.1.0").isNewerThan(v("10.1.0-rc.2")), "final newer than rc");
    CHECK(v("10.1.0-beta.2").isNewerThan(v("10.1.0-beta.1")), "beta number");
    CHECK(v("10.1.0-rc").isNewerThan(v("10.1.0-beta.9")), "label order");
    CHECK_EQ(v("garbage").major, 0, "unparsable is 0.0.0");
    const QByteArray json = R"([
      {"tag_name":"v10.2.0","html_url":"u1","body":"- a
- b","draft":false,"prerelease":false,"assets":[{"name":"Jubler-10.2.0.AppImage"}]},
      {"tag_name":"v10.3.0","html_url":"u2","body":"notes","draft":false,"prerelease":true,"assets":[{"name":"x.appimage"}]},
      {"tag_name":"v10.1.5","html_url":"u3","body":"notes","draft":false,"prerelease":false,"assets":[{"name":"Jubler-mac.dmg"}]},
      {"tag_name":"v10.4.0","html_url":"u4","body":"","draft":false,"prerelease":false,"assets":[{"name":"x.appimage"}]}
    ])";
    const auto list = AutoUpdateCore::newerReleases(QJsonDocument::fromJson(json).array(), v("10.0.0"), QStringLiteral(".appimage"), QStringLiteral("linux"));
    CHECK_EQ(int(list.size()), 1, "prerelease, missing notes and foreign assets skipped");
    CHECK_EQ(list.first().tag, QStringLiteral("10.2.0"), "the linux release");
    const QByteArray mac = R"([
      {"tag_name":"v10.5.0","html_url":"u5","body":"notes","draft":false,"prerelease":false,"assets":[{"name":"Jubler-darwin.dmg"}]},
      {"tag_name":"v10.6.0","html_url":"u6","body":"notes","draft":false,"prerelease":false,"assets":[{"name":"Jubler-10.6.0-win-x64.zip"}]}
    ])";
    const auto win = AutoUpdateCore::newerReleases(QJsonDocument::fromJson(mac).array(), v("10.0.0"), QStringLiteral(".exe"), QStringLiteral("win"));
    CHECK_EQ(int(win.size()), 1, "darwin is not a windows asset");
    CHECK_EQ(win.value(0).tag, QStringLiteral("10.6.0"), "the windows release by tag word");
}

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    testInitPrefs();
    testTokenizer();
    testEncryption();
    testAzureUrl();
    testVersions();
    if (g_failures) std::fprintf(stderr, "%d failure(s)\n", g_failures);
    return g_failures ? 1 : 0;
}
