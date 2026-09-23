/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "core/i18n/I18N.h"
#include "core/options/JavaPrefs.h"

#include "core/options/JavaPrefsWin.h"
#include "core/options/Prefs.h"
#include "core/os/Debug.h"

#include <QDir>
#include <QFile>
#include <QKeySequence>
#include <QProcess>
#include <QRegularExpression>
#include <QStringList>
#include <QXmlStreamReader>

namespace JavaPrefs {

namespace {

const QStringList NODE_PATH = {QStringLiteral("com"), QStringLiteral("panayotis"), QStringLiteral("jubler")};
const QString SHORTCUT_KEY = QStringLiteral("shortcut.keys");
// Java-only meaning: the Java drop-in plugins were jar files, the port's are Qt plugins.
const QStringList SKIPPED = {QStringLiteral("plugins.enabled"), QStringLiteral("plugins.known")};

// java.awt.event.KeyEvent VK_* code → Qt key (0 when there is no equivalent).
Qt::Key javaKey(int vk, bool *keypad) {
    *keypad = false;
    if ((vk >= 0x30 && vk <= 0x39) || (vk >= 0x41 && vk <= 0x5A))
        return Qt::Key(vk);   // VK_0…VK_9, VK_A…VK_Z match Qt
    if (vk >= 0x70 && vk <= 0x7B)
        return Qt::Key(Qt::Key_F1 + (vk - 0x70));
    if (vk >= 0xF000 && vk <= 0xF00B)
        return Qt::Key(Qt::Key_F13 + (vk - 0xF000));
    if (vk >= 0x60 && vk <= 0x69) {
        *keypad = true;
        return Qt::Key(Qt::Key_0 + (vk - 0x60));
    }
    switch (vk) {
        case 0x08: return Qt::Key_Backspace;
        case 0x09: return Qt::Key_Tab;
        case 0x0A: return Qt::Key_Return;
        case 0x0C: return Qt::Key_Clear;
        case 0x13: return Qt::Key_Pause;
        case 0x1B: return Qt::Key_Escape;
        case 0x20: return Qt::Key_Space;
        case 0x21: return Qt::Key_PageUp;
        case 0x22: return Qt::Key_PageDown;
        case 0x23: return Qt::Key_End;
        case 0x24: return Qt::Key_Home;
        case 0x25: return Qt::Key_Left;
        case 0x26: return Qt::Key_Up;
        case 0x27: return Qt::Key_Right;
        case 0x28: return Qt::Key_Down;
        case 0x2C: return Qt::Key_Comma;
        case 0x2D: return Qt::Key_Minus;
        case 0x2E: return Qt::Key_Period;
        case 0x2F: return Qt::Key_Slash;
        case 0x3B: return Qt::Key_Semicolon;
        case 0x3D: return Qt::Key_Equal;
        case 0x5B: return Qt::Key_BracketLeft;
        case 0x5C: return Qt::Key_Backslash;
        case 0x5D: return Qt::Key_BracketRight;
        case 0x6A: *keypad = true; return Qt::Key_Asterisk;
        case 0x6B: *keypad = true; return Qt::Key_Plus;
        case 0x6C: *keypad = true; return Qt::Key_Comma;
        case 0x6D: *keypad = true; return Qt::Key_Minus;
        case 0x6E: *keypad = true; return Qt::Key_Period;
        case 0x6F: *keypad = true; return Qt::Key_Slash;
        case 0x7F: return Qt::Key_Delete;
        case 0x90: return Qt::Key_NumLock;
        case 0x91: return Qt::Key_ScrollLock;
        case 0x96: return Qt::Key_Ampersand;
        case 0x97: return Qt::Key_Asterisk;
        case 0x98: return Qt::Key_QuoteDbl;
        case 0x99: return Qt::Key_Less;
        case 0x9A: return Qt::Key_Print;
        case 0x9B: return Qt::Key_Insert;
        case 0x9C: return Qt::Key_Help;
        case 0xA0: return Qt::Key_Greater;
        case 0xA1: return Qt::Key_BraceLeft;
        case 0xA2: return Qt::Key_BraceRight;
        case 0xC0: return Qt::Key_QuoteLeft;
        case 0xDE: return Qt::Key_Apostrophe;
        case 0xE0: *keypad = true; return Qt::Key_Up;
        case 0xE1: *keypad = true; return Qt::Key_Down;
        case 0xE2: *keypad = true; return Qt::Key_Left;
        case 0xE3: *keypad = true; return Qt::Key_Right;
        case 0x0200: return Qt::Key_At;
        case 0x0201: return Qt::Key_Colon;
        case 0x0202: return Qt::Key_AsciiCircum;
        case 0x0203: return Qt::Key_Dollar;
        case 0x0204: return Qt::Key(0x20AC);   // €
        case 0x0205: return Qt::Key_Exclam;
        case 0x0206: return Qt::Key_exclamdown;
        case 0x0207: return Qt::Key_ParenLeft;
        case 0x0208: return Qt::Key_NumberSign;
        case 0x0209: return Qt::Key_Plus;
        // VK_RIGHT_PARENTHESIS is left out: ')' would end the stored `TAG=(…)` value.
        case 0x020B: return Qt::Key_Underscore;
        case 0x020D: return Qt::Key_Menu;
        default: return Qt::Key(0);
    }
}

// Both the old (SHIFT_MASK…) and the extended (SHIFT_DOWN_MASK…) Java masks
// occur. The Java "menu" modifier is Meta (⌘) on macOS and Ctrl elsewhere,
// as Qt's Ctrl in portable form is.
Qt::KeyboardModifiers javaModifiers(int m) {
    Qt::KeyboardModifiers q;
    if (m & (0x01 | 0x40)) q |= Qt::ShiftModifier;
    if (m & (0x08 | 0x200)) q |= Qt::AltModifier;
    const bool ctrl = m & (0x02 | 0x80);
    const bool meta = m & (0x04 | 0x100);
#ifdef Q_OS_MACOS
    if (meta) q |= Qt::ControlModifier;
    if (ctrl) q |= Qt::MetaModifier;
#else
    if (ctrl) q |= Qt::ControlModifier;
    if (meta) q |= Qt::MetaModifier;
#endif
    return q;
}


#if defined(Q_OS_MACOS)
// The dictionaries of an XML plist, keyed by their slash-separated path; a
// key's own slashes count as path separators, so "/com/panayotis/jubler/"
// and nested "com/" › "panayotis/" › "jubler/" end up at the same place.
void readPlistDict(QXmlStreamReader &x, const QStringList &path, QMap<QString, QMap<QString, QString>> &out) {
    QString key;
    while (x.readNextStartElement()) {
        if (x.name() == QLatin1String("key"))
            key = x.readElementText();
        else if (x.name() == QLatin1String("dict"))
            readPlistDict(x, path + key.split(QLatin1Char('/'), Qt::SkipEmptyParts), out);
        else if (x.name() == QLatin1String("string"))
            out[path.join(QLatin1Char('/'))].insert(key, x.readElementText());
        else
            x.skipCurrentElement();
    }
}

QMap<QString, QString> readMacDomain(const QString &domain) {
    QProcess p;
    p.start(QStringLiteral("/usr/bin/defaults"), {QStringLiteral("export"), domain, QStringLiteral("-")});
    if (!p.waitForFinished(10000) || p.exitCode() != 0)
        return {};
    QXmlStreamReader x(p.readAllStandardOutput());
    QMap<QString, QMap<QString, QString>> dicts;
    if (x.readNextStartElement() && x.name() == QLatin1String("plist") && x.readNextStartElement()
        && x.name() == QLatin1String("dict"))
        readPlistDict(x, {}, dicts);
    QMap<QString, QString> found = dicts.value(NODE_PATH.join(QLatin1Char('/')));
    // A domain named after the node may keep the node's entries at its top
    // level — only when they look like Jubler's (not Cocoa's NS* keys).
    if (found.isEmpty() && domain == NODE_PATH.join(QLatin1Char('.'))) {
        const QMap<QString, QString> top = dicts.value(QString());
        for (const char *known : {"ui.language", "system.lastdirpath", "system.windowstate", "default.fps", "shortcut.keys"})
            if (top.contains(QLatin1String(known))) {
                found = top;
                break;
            }
    }
    return found;
}
#endif

}  // namespace

QString fromRegistry(const QString &s, bool value) {
    QString out;
    for (int i = 0; i < s.size(); ++i) {
        QChar ch = s.at(i);
        if (ch == QLatin1Char('/') && i + 1 < s.size()) {
            const QChar next = s.at(i + 1);
            if (value && next == QLatin1Char('u')) {
                if (i + 6 > s.size())
                    break;
                ch = QChar(s.mid(i + 2, 4).toUShort(nullptr, 16));
                i += 5;
            } else if (next >= QLatin1Char('A') && next <= QLatin1Char('Z')) {
                ch = next;
                ++i;
            } else if (next == QLatin1Char('/')) {
                ch = QLatin1Char('\\');
                ++i;
            }
        } else if (ch == QLatin1Char('\\'))
            ch = QLatin1Char('/');
        out.append(ch);
    }
    return out;
}

QMap<QString, QString> parseXml(const QByteArray &xml, QString *error) {
    QMap<QString, QString> out;
    QXmlStreamReader x(xml);
    QStringList path;
    bool rootIsMap = false;
    bool first = true;
    while (!x.atEnd()) {
        x.readNext();
        if (x.isStartElement()) {
            if (first) {
                first = false;
                rootIsMap = x.name() == QLatin1String("map");
                if (!rootIsMap && x.name() != QLatin1String("preferences")) {
                    if (error) *error = __("Not a Java preferences file");
                    return {};
                }
            }
            if (x.name() == QLatin1String("node"))
                path.append(x.attributes().value(QLatin1String("name")).toString());
            else if (x.name() == QLatin1String("entry") && (rootIsMap ? path.isEmpty() : path == NODE_PATH))
                out.insert(x.attributes().value(QLatin1String("key")).toString(),
                           x.attributes().value(QLatin1String("value")).toString());
        } else if (x.isEndElement() && x.name() == QLatin1String("node") && !path.isEmpty())
            path.removeLast();
    }
    if (x.hasError()) {
        if (error) *error = x.errorString();
        return {};
    }
    if (out.isEmpty() && error)
        *error = __("No Jubler preferences found");
    return out;
}

QString convertShortcuts(const QString &javaValue) {
    static const QRegularExpression re(QStringLiteral("(\\w\\w\\w)=\\((\\d+),(\\d+)\\)"));
    QStringList parts;
    auto it = re.globalMatch(javaValue);
    while (it.hasNext()) {
        const auto m = it.next();
        const int vk = m.captured(2).toInt();
        QString keys;   // key code 0: the shortcut was removed
        if (vk != 0) {
            bool keypad = false;
            const Qt::Key key = javaKey(vk, &keypad);
            if (key == Qt::Key(0)) {
                Debug::debug(QStringLiteral("Java shortcut %1 has an unknown key code %2; the default is kept").arg(m.captured(1)).arg(vk));
                continue;
            }
            Qt::KeyboardModifiers mods = javaModifiers(m.captured(3).toInt());
            if (keypad) mods |= Qt::KeypadModifier;
            keys = QKeySequence(QKeyCombination(mods, key)).toString(QKeySequence::PortableText);
        }
        parts.append(m.captured(1) + QStringLiteral("=(") + keys + QLatin1Char(')'));
    }
    return parts.join(QLatin1Char(','));
}

QMap<QString, QString> readNativeStore() {
#if defined(Q_OS_WIN)
    QMap<QString, QString> out;
    for (const auto &[name, value] : detail::readRegistryValues(L"Software\\JavaSoft\\Prefs\\com\\panayotis\\jubler")) {
        const QString n = QString::fromStdWString(name);
        if (!n.startsWith(QLatin1String("/!")))
            out.insert(fromRegistry(n, false), fromRegistry(QString::fromStdWString(value), true));
    }
    return out;
#elif defined(Q_OS_MACOS)
    QMap<QString, QString> out = readMacDomain(NODE_PATH.join(QLatin1Char('.')));
    if (out.isEmpty())
        out = readMacDomain(QStringLiteral("com.apple.java.util.prefs"));
    return out;
#else
    QFile f(QDir::homePath() + QStringLiteral("/.java/.userPrefs/") + NODE_PATH.join(QLatin1Char('/')) + QStringLiteral("/prefs.xml"));
    if (!f.open(QIODevice::ReadOnly))
        return {};
    return parseXml(f.readAll());
#endif
}

void apply(const QMap<QString, QString> &javaPrefs) {
    // The port's own values of the keys the Java meant differently survive.
    QMap<QString, QString> kept;
    for (const QString &k : SKIPPED)
        if (Prefs::contains(k)) kept.insert(k, Prefs::getString(k, QString()));
    Prefs::resetPrefs();
    for (auto it = kept.constBegin(); it != kept.constEnd(); ++it) Prefs::set(it.key(), it.value());
    for (auto it = javaPrefs.constBegin(); it != javaPrefs.constEnd(); ++it) {
        if (SKIPPED.contains(it.key()))
            continue;
        Prefs::set(it.key(), it.key() == SHORTCUT_KEY ? convertShortcuts(it.value()) : it.value());
    }
}

}  // namespace JavaPrefs
