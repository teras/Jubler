/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

// Preferences: Java preferences migration (XML, registry escapes, shortcuts).
#include "TestSupport.h"

#include <QFile>

#include "core/options/AutoSaveOptions.h"
#include "core/options/JavaPrefs.h"

// A `Preferences.exportNode` export: the ancestors come with empty maps.
static const char *EXPORT_XML = R"XML(<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE preferences SYSTEM "http://java.sun.com/dtd/preferences.dtd">
<preferences EXTERNAL_XML_VERSION="1.0">
  <root type="user">
    <map/>
    <node name="com">
      <map><entry key="stray" value="x"/></map>
      <node name="panayotis">
        <map/>
        <node name="jubler">
          <map>
            <entry key="default.fps" value="25.0"/>
            <entry key="plugins.known" value="example.jar"/>
            <entry key="shortcut.keys" value="SAV=(83,128),PLA=(32,0)"/>
            <entry key="system.lastfile1" value="/tmp/a &amp; b.srt"/>
          </map>
        </node>
      </node>
    </node>
  </root>
</preferences>
)XML";

static void testParseXml() {
    QString err;
    const QMap<QString, QString> m = JavaPrefs::parseXml(EXPORT_XML, &err);
    CHECK_EQ(int(m.size()), 4, "export: only the jubler node's entries");
    CHECK_EQ(m.value(QStringLiteral("default.fps")), QStringLiteral("25.0"), "export: value");
    CHECK_EQ(m.value(QStringLiteral("system.lastfile1")), QStringLiteral("/tmp/a & b.srt"), "export: entity decoded");
    CHECK(!m.contains(QStringLiteral("stray")), "export: ancestor entries ignored");

    // The Linux backing store (~/.java/.userPrefs/…/prefs.xml).
    const QMap<QString, QString> store = JavaPrefs::parseXml(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE map SYSTEM \"http://java.sun.com/dtd/preferences.dtd\">\n"
        "<map MAP_XML_VERSION=\"1.0\">\n  <entry key=\"ui.language\" value=\"el\"/>\n  <entry key=\"plugins.known\" value=\"a.jar&#10;b.jar\"/>\n</map>\n");
    CHECK_EQ(store.value(QStringLiteral("ui.language")), QStringLiteral("el"), "map: value");
    CHECK_EQ(store.value(QStringLiteral("plugins.known")), QStringLiteral("a.jar\nb.jar"), "map: newline entity");

    err.clear();
    CHECK(JavaPrefs::parseXml("<html><body/></html>", &err).isEmpty() && !err.isEmpty(), "foreign XML rejected");
    err.clear();
    CHECK(JavaPrefs::parseXml("<preferences><root", &err).isEmpty() && !err.isEmpty(), "broken XML rejected");
}

static void testShortcuts() {
    // Ctrl (down mask and old mask), Ctrl+Shift, keypad, removed, unknown code.
    CHECK_EQ(JavaPrefs::convertShortcuts(QStringLiteral("SAV=(83,128),PLA=(32,0),BAR=(112,2),FOO=(83,192),KPD=(96,0),DEL=(0,0),UNK=(99999,0),ALT=(37,512)")),
#ifdef Q_OS_MACOS
             QStringLiteral("SAV=(Meta+S),PLA=(Space),BAR=(Meta+F1),FOO=(Meta+Shift+S),KPD=(Num+0),DEL=(),ALT=(Alt+Left)"),
#else
             QStringLiteral("SAV=(Ctrl+S),PLA=(Space),BAR=(Ctrl+F1),FOO=(Ctrl+Shift+S),KPD=(Num+0),DEL=(),ALT=(Alt+Left)"),
#endif
             "shortcut conversion");
#ifndef Q_OS_MACOS
    CHECK_EQ(JavaPrefs::convertShortcuts(QStringLiteral("CMD=(90,256)")), QStringLiteral("CMD=(Meta+Z)"), "meta");
#endif
    CHECK_EQ(JavaPrefs::convertShortcuts(QString()), QString(), "empty list");
}

static void testRegistryEscapes() {
    CHECK_EQ(JavaPrefs::fromRegistry(QStringLiteral("system.last/Dir/Path"), false), QStringLiteral("system.lastDirPath"), "name: upper case");
    CHECK_EQ(JavaPrefs::fromRegistry(QStringLiteral("/C:\\/Users\\a//b"), true), QStringLiteral("C:/Users/a\\b"), "value: slashes");
    CHECK_EQ(JavaPrefs::fromRegistry(QStringLiteral("/u03b1/u03b2c"), true), QStringLiteral("αβc"), "value: /u escapes");
}

static void testImport() {
    testInitPrefs();
    Prefs::set(QStringLiteral("old.key"), QStringLiteral("gone"));
    const QString path = QStringLiteral("/tmp/jubler-qt-test-javaprefs-%1.xml").arg(QCoreApplication::applicationPid());
    QFile f(path);
    CHECK(f.open(QIODevice::WriteOnly), "write export");
    f.write(EXPORT_XML);
    f.close();
    CHECK_EQ(Prefs::importPrefs(path), QString(), "import succeeds");
    CHECK(!Prefs::contains(QStringLiteral("old.key")), "import replaces everything");
    CHECK_EQ(Prefs::getFloat(QStringLiteral("default.fps"), 0), 25.0f, "float value");
    CHECK(!Prefs::contains(QStringLiteral("plugins.known")), "Java plugin keys skipped");
#ifndef Q_OS_MACOS
    CHECK_EQ(Prefs::getString(QStringLiteral("shortcut.keys"), QString()), QStringLiteral("SAV=(Ctrl+S),PLA=(Space)"), "shortcuts converted");
#endif
    QFile::remove(path);
}

static void testImportKeepsPortKeysAndBom() {
    testInitPrefs();
    Prefs::set(QStringLiteral("plugins.enabled"), QStringLiteral("greek-subs"));
    const QString path = QStringLiteral("/tmp/jubler-qt-test-javaprefs-bom-%1.xml").arg(QCoreApplication::applicationPid());
    QFile f(path);
    CHECK(f.open(QIODevice::WriteOnly), "write export");
    f.write("\xEF\xBB\xBF");
    f.write(EXPORT_XML);
    f.close();
    CHECK_EQ(Prefs::importPrefs(path), QString(), "BOM export imports as XML");
    CHECK_EQ(Prefs::getString(QStringLiteral("default.fps"), QString()), QStringLiteral("25.0"), "BOM export values");
    CHECK_EQ(Prefs::getString(QStringLiteral("plugins.enabled"), QString()), QStringLiteral("greek-subs"), "port plugin choice kept");
    QFile::remove(path);
}

static void testImportRejectsOtherFiles() {
    testInitPrefs();
    Prefs::set(QStringLiteral("default.fps"), QStringLiteral("30.0"));
    const QString path = QStringLiteral("/tmp/jubler-qt-test-notprefs-%1.srt").arg(QCoreApplication::applicationPid());
    for (const QByteArray &content : {QByteArray("1\n00:00:01,000 --> 00:00:02,000\nHello = world\n"), QByteArray(), QByteArray("\x01\x02\xff=\x03")}) {
        QFile f(path);
        CHECK(f.open(QIODevice::WriteOnly), "write file");
        f.write(content);
        f.close();
        CHECK(!Prefs::importPrefs(path).isEmpty(), "a non-preferences file is refused");
        CHECK_EQ(Prefs::getString(QStringLiteral("default.fps"), QString()), QStringLiteral("30.0"), "the store is kept");
    }
    QFile::remove(path);
}

// Cancel of the Preferences dialog after an import/reset: the snapshot comes back exactly.
static void testSnapshotRestore() {
    testInitPrefs();
    Prefs::set(QStringLiteral("default.fps"), QStringLiteral("30.0"));
    Prefs::set(QStringLiteral("options.maxduration"), 9);
    const auto before = Prefs::snapshot();
    Prefs::resetPrefs();
    Prefs::set(QStringLiteral("imported.key"), QStringLiteral("x"));
    Prefs::restore(before);
    CHECK_EQ(Prefs::getString(QStringLiteral("default.fps"), QString()), QStringLiteral("30.0"), "value restored");
    CHECK_EQ(Prefs::getInt(QStringLiteral("options.maxduration"), 0), 9, "int restored");
    CHECK(!Prefs::contains(QStringLiteral("imported.key")), "imported key removed");
    CHECK_EQ(Prefs::keys().size(), int(before.size()), "same keys");
}

static void testColumnOrder() {
    testInitPrefs();
    QList<int> natural;
    for (int c = 0; c < AutoSaveOptions::COLUMN_COUNT; ++c) natural.append(c);
    CHECK(AutoSaveOptions::getColumnOrder() == natural, "default order");
    const QList<int> order = {0, 1, 2, 7, 3, 4, 5, 6, 8};
    AutoSaveOptions::setColumnOrder(order);
    CHECK(AutoSaveOptions::getColumnOrder() == order, "stored order");
    Prefs::set(QStringLiteral("system.columnorder"), QStringLiteral("0,1,1,x"));
    CHECK(AutoSaveOptions::getColumnOrder() == natural, "damaged order → natural");
}

static void testFloatString() {
    CHECK_EQ(Prefs::floatString(25.0f), QStringLiteral("25.0"), "whole number keeps .0");
    CHECK_EQ(Prefs::floatString(23.976f), QStringLiteral("23.976"), "shortest form");
    CHECK_EQ(Prefs::floatString(29.97f), QStringLiteral("29.97"), "shortest form 2");
}

static void testVisibleColumns() {
    testInitPrefs();
    Prefs::set(QStringLiteral("system.visiblecolumns"), QStringLiteral("FE"));   // as the Java stored it
    QList<bool> cols = AutoSaveOptions::getVisibleColumns();
    CHECK(cols[1] && cols[2] && cols[8], "Java value: start, end and the text column shown");
    cols[8] = false;
    AutoSaveOptions::setVisibleColumns(cols);
    CHECK(!AutoSaveOptions::getVisibleColumns()[8], "a hidden text column stays hidden");
    AutoSaveOptions::setVisibleColumns(QList<bool>(9, false));
    CHECK(AutoSaveOptions::getVisibleColumns()[8], "never zero columns");
}

int main(int argc, char **argv) {
    QCoreApplication app(argc, argv);
    testInitPrefs();
    testParseXml();
    testShortcuts();
    testRegistryEscapes();
    testImport();
    testImportKeepsPortKeysAndBom();
    testVisibleColumns();
    testImportRejectsOtherFiles();
    testSnapshotRestore();
    testColumnOrder();
    testFloatString();
    return testFinish("test_prefs");
}
