/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#include "app/dialogs/AboutDialog.h"

#include <QDate>
#include <QDialogButtonBox>
#include <QFile>
#include <QLabel>
#include <QPlainTextEdit>
#include <QTabWidget>
#include <QTextBrowser>
#include <QVBoxLayout>

#include "app/Theme.h"
#include "app/Version.h"
#include "core/i18n/I18N.h"

namespace {
QString mail(const QString &user, const QString &host) {
    return QStringLiteral("&lt;") + user + QLatin1Char('@') + host + QStringLiteral("&gt;");
}
}  // namespace

AboutDialog::AboutDialog(QWidget *parent) : QDialog(parent) {
    setWindowTitle(__("About Jubler"));
    setModal(true);
    auto *lay = new QVBoxLayout(this);
    auto *logo = new QLabel(this);
    logo->setPixmap(Theme::logo().pixmap(97, 97));   // the Java: the logo SVG (484 px) × 0.2
    logo->setAlignment(Qt::AlignCenter);
    lay->addWidget(logo);
    auto *tabs = new QTabWidget(this);
    lay->addWidget(tabs, 1);

    // About.
    auto *about = new QTextBrowser(tabs);
    about->setOpenExternalLinks(true);
    about->setFrameShape(QFrame::NoFrame);
    about->viewport()->setAutoFillBackground(false);
    QFont f = Theme::adjustFont(about->font(), 2);   // the Java's BOLD, size + 2
    f.setBold(true);
    about->setFont(f);
    about->setHtml(QStringLiteral("<div align=\"center\"><br/>") + __("Subtitle Editor") + QStringLiteral("<br/><br/>") + __("Version") + QLatin1Char(' ') +
                   Version::current() + QStringLiteral("<br/><br/>© 2005-") + QString::number(QDate::currentDate().year()) +
                   QStringLiteral(" Panayotis Katsaloulis<br/>Παναγιώτης Κατσαλούλης<br/><br/><a href=\"https://jubler.org\">https://jubler.org</a></div>"));
    tabs->addTab(about, __("About"));

    // Thanks.
    const QStringList transl{QStringLiteral("Tomáš Bambas ") + mail(QStringLiteral("conyx"), QStringLiteral("seznam.cz")),
                             QStringLiteral("Rene ") + mail(QStringLiteral("bmom43"), QStringLiteral("hotmail.com")),
                             QStringLiteral("Julien Escoffier ") + mail(QStringLiteral("jubler"), QStringLiteral("jcpdt7j.com")),
                             QStringLiteral("Christian Weiske ") + mail(QStringLiteral("cweiske"), QStringLiteral("cweiske.de")),
                             QStringLiteral("Panayotis Katsaloulis ") + mail(QStringLiteral("panayotis"), QStringLiteral("panayotis.com")),
                             QStringLiteral("Michele Gianella ") + mail(QStringLiteral("gianella.michele"), QStringLiteral("gmail.com")),
                             QStringLiteral("Doutor.Zero ") + mail(QStringLiteral("doutor.zero"), QStringLiteral("gmail.com")),
                             QStringLiteral("Nikola Karanovic ") + mail(QStringLiteral("dzonithebatee"), QStringLiteral("yahoo.com")),
                             QStringLiteral("Alfredo Quesada Sánchez ") + mail(QStringLiteral("freddy2_es"), QStringLiteral("yahoo.com")),
                             QStringLiteral("Asım Sinan Yüksel ") + mail(QStringLiteral("yuksel.asim.sinan"), QStringLiteral("gmail.com"))};
    const QStringList langs{__("Czech"), __("Dutch"), __("French"), __("German"), __("Greek"), __("Italian"), __("Portuguese (Brazilian)"), __("Serbian"), __("Spanish"), __("Turkish")};
    QString thanks = QStringLiteral("<html><body>");
    thanks += __("Special thanks") + QStringLiteral(":<br/>") + __("{0} plugin", QStringLiteral("ffmpeg")) + QStringLiteral(": Thanos Kyritsis ") + mail(QStringLiteral("djart"), QStringLiteral("linux.gr")) + QStringLiteral("<br/>");
    thanks += QStringLiteral("<br/>") + __("Icon theme") + QStringLiteral(": <a href=\"https://icons8.com\">https://icons8.com</a><br/>");
    thanks += QStringLiteral("<br/>") + __("Jubler mascot") + QStringLiteral(": Adriano Monecchi ") + mail(QStringLiteral("fornacciari"), QStringLiteral("gmail.com")) + QStringLiteral("<br/>");
    thanks += __("Original Jubler mascot") + QStringLiteral(": Dimitris Karakatsanis ") + mail(QStringLiteral("dimkaras"), QStringLiteral("ath.forthnet.gr")) + QStringLiteral("<br/>");
    thanks += __("{0} plugin", QStringLiteral("zemberek")) + QStringLiteral(": Serkan Kaba ") + mail(QStringLiteral("serkan_kaba"), QStringLiteral("yahoo.com")) + QStringLiteral("<br/>");
    thanks += __("{0} plugin", QStringLiteral("W3C TT")) + QStringLiteral(": Albert DeSantis ") + mail(QStringLiteral("netgensuperstar"), QStringLiteral("gmail.com")) + QStringLiteral("<br/>");
    thanks += QStringLiteral("<br/>") + __("Translators") + QStringLiteral(":<br/>");
    for (int i = 0; i < transl.size(); ++i) thanks += langs[i] + QStringLiteral(" : ") + transl[i] + QStringLiteral("<br/>");
    thanks += QStringLiteral("<br/>") + __("Packagers") + QStringLiteral("<br/>");
    thanks += QStringLiteral("Fedora: Marcin Zajączkowski ") + mail(QStringLiteral("mszpak"), QStringLiteral("wp.pl")) + QStringLiteral("<br/>");
    thanks += QStringLiteral("Gentoo: Serkan Kaba ") + mail(QStringLiteral("serkan"), QStringLiteral("gentoo.org")) + QStringLiteral("<br/>");
    thanks += QStringLiteral("Slackware: Thanos Kyritsis ") + mail(QStringLiteral("djart"), QStringLiteral("linux.gr")) + QStringLiteral("<br/></body></html>");
    auto *thanksT = new QTextBrowser(tabs);
    thanksT->setOpenExternalLinks(true);
    thanksT->setHtml(thanks);
    thanksT->setMinimumSize(400, 300);
    tabs->addTab(thanksT, __("Thanks"));

    // License.
    auto *license = new QPlainTextEdit(tabs);
    license->setReadOnly(true);
    license->setFont(Theme::adjustFont(license->font(), -2));
    QFile lic(QStringLiteral(":/LICENSE"));
    QString text = __("This program is distributed under the terms of the GNU Affero General Public License, version 3.") + QStringLiteral("\n\n");
    if (lic.open(QIODevice::ReadOnly)) text += QString::fromUtf8(lic.readAll());
    license->setPlainText(text);
    tabs->addTab(license, __("License"));

    auto *buttons = new QDialogButtonBox(QDialogButtonBox::Close, this);
    connect(buttons, &QDialogButtonBox::rejected, this, &QDialog::reject);
    connect(buttons, &QDialogButtonBox::accepted, this, &QDialog::accept);
    lay->addWidget(buttons);
}
