/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.subdownload;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.Desktop;

/**
 * Small helper for read-only HTML text whose {@code <a href>} links open in the system browser, matching
 * the app's existing pattern (JAbout / Azure config): a JEditorPane with a HyperlinkListener that browses
 * on activation. Rendered with the standard link affordance (hand cursor, underline) and the label font.
 */
final class Links {

    private Links() {
    }

    /** @param htmlBody inner HTML (already localized), typically containing one or more {@code <a>} links. */
    static JEditorPane html(String htmlBody) {
        JEditorPane pane = new JEditorPane("text/html", "<html><body>" + htmlBody + "</body></html>");
        pane.setEditable(false);
        pane.setOpaque(false);
        pane.setBorder(null);
        // Render with the surrounding dialog's label font rather than the JEditorPane default.
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setFont(UIManager.getFont("Label.font"));
        pane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null)
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ignored) {
                }
        });
        return pane;
    }
}
