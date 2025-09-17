/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.media.MediaFile;
import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.Subtitles;

/**
 * iTunes Timed Text (ITT) format support.
 *
 * ITT is a subset of TTML specifically designed for iTunes/Apple platforms.
 * It uses the .itt file extension and follows TTML structure with Apple-specific constraints.
 *
 * Loading Strategy: Liberal - can read any valid TTML file with full XML/DOM parsing
 * Saving Strategy: Conservative - only outputs ITT-compliant subset
 *
 * ITT Output Constraints:
 * - Uses only TTML 1.0 compatible namespaces
 * - Limited to basic styling attributes
 * - No animation or advanced TTML features
 * - Apple-compliant document structure
 */
public class ITTSubFormat extends W3CTimedText {

    @Override
    public String getExtension() {
        return "itt";
    }

    @Override
    public String getName() {
        return "iTunes Timed Text";
    }

    @Override
    public String getExtendedName() {
        return "iTunes Timed Text (ITT)";
    }

    @Override
    protected void generateXMLDocument(org.w3c.dom.Document doc, com.panayotis.jubler.subs.Subtitles subs, com.panayotis.jubler.media.MediaFile media) {
        // Create ITT-compliant XML structure (conservative saving)
        org.w3c.dom.Element root = doc.createElement("tt");
        root.setAttribute("xml:lang", "en");
        root.setAttribute("xmlns", "http://www.w3.org/ns/ttml");
        root.setAttribute("xmlns:tts", "http://www.w3.org/ns/ttml#styling");
        root.setAttribute("xmlns:ttp", "http://www.w3.org/ns/ttml#parameter");
        root.setAttribute("ttp:timeBase", "media");
        doc.appendChild(root);

        // Create head section with ITT constraints
        org.w3c.dom.Element head = doc.createElement("head");
        root.appendChild(head);

        // Metadata
        org.w3c.dom.Element metadata = doc.createElement("metadata");
        head.appendChild(metadata);
        org.w3c.dom.Element title = doc.createElement("title");
        title.setTextContent("iTunes Timed Text");
        metadata.appendChild(title);

        // Styling - ITT uses simple default style only
        org.w3c.dom.Element styling = doc.createElement("styling");
        head.appendChild(styling);
        org.w3c.dom.Element defaultStyle = doc.createElement("style");
        defaultStyle.setAttribute("xml:id", "default");
        defaultStyle.setAttribute("tts:fontFamily", "sansSerif");
        defaultStyle.setAttribute("tts:fontSize", "100%");
        defaultStyle.setAttribute("tts:color", "white");
        styling.appendChild(defaultStyle);

        // Layout - ITT uses single bottom region
        org.w3c.dom.Element layout = doc.createElement("layout");
        head.appendChild(layout);
        org.w3c.dom.Element region = doc.createElement("region");
        region.setAttribute("xml:id", "bottom");
        region.setAttribute("tts:origin", "10% 80%");
        region.setAttribute("tts:extent", "80% 15%");
        region.setAttribute("tts:displayAlign", "after");
        layout.appendChild(region);

        // Body
        org.w3c.dom.Element body = doc.createElement("body");
        root.appendChild(body);
        org.w3c.dom.Element div = doc.createElement("div");
        body.appendChild(div);

        // Add subtitle entries with ITT filtering
        for (int i = 0; i < subs.size(); i++) {
            SubEntry sub = subs.elementAt(i);
            org.w3c.dom.Element p = doc.createElement("p");
            p.setAttribute("begin", sub.getStartTime().getSeconds('.'));
            p.setAttribute("end", sub.getFinishTime().getSeconds('.'));
            p.setAttribute("style", "default");
            p.setAttribute("region", "bottom");

            // Filter text content for ITT compliance
            String filteredText = filterForITTCompliance(sub.getText());
            p.setTextContent(filteredText);

            div.appendChild(p);
        }
    }

    /**
     * Filter subtitle text to ensure ITT compliance by removing unsupported features
     */
    private String filterForITTCompliance(String text) {
        if (text == null) {
            return "";
        }

        // Start with the original text
        String filtered = text;

        // Replace line breaks with ITT-compliant <br/> tags
        filtered = filtered.replace("\n", "<br/>");

        // Filter out advanced styling that ITT doesn't support
        filtered = filterUnsupportedTags(filtered);

        // Escape XML characters
        filtered = filtered.replace("&", "&amp;")
                          .replace("<", "&lt;")
                          .replace(">", "&gt;")
                          .replace("\"", "&quot;");

        // Re-allow supported tags
        filtered = restoreSupportedTags(filtered);

        return filtered;
    }

    /**
     * Remove or replace unsupported tags with ITT-compliant alternatives
     */
    private String filterUnsupportedTags(String text) {
        // Remove advanced styling tags not supported in ITT
        text = text.replaceAll("</?font[^>]*>", ""); // Remove font tags
        text = text.replaceAll("</?ruby[^>]*>", ""); // Remove ruby tags
        text = text.replaceAll("</?rt[^>]*>", "");   // Remove rt tags
        text = text.replaceAll("</?v[^>]*>", "");    // Remove voice tags
        text = text.replaceAll("</?c[^>]*>", "");    // Remove class tags

        // Keep only basic formatting supported in ITT
        // ITT supports: span, i, b, u, br

        return text;
    }

    /**
     * Restore ITT-supported tags after XML escaping
     */
    private String restoreSupportedTags(String text) {
        // Restore supported ITT tags
        text = text.replace("&lt;br/&gt;", "<br/>");
        text = text.replace("&lt;br&gt;", "<br/>");
        text = text.replace("&lt;/br&gt;", "");

        // Restore basic formatting tags that ITT supports
        text = text.replace("&lt;i&gt;", "<i>");
        text = text.replace("&lt;/i&gt;", "</i>");
        text = text.replace("&lt;b&gt;", "<b>");
        text = text.replace("&lt;/b&gt;", "</b>");
        text = text.replace("&lt;u&gt;", "<u>");
        text = text.replace("&lt;/u&gt;", "</u>");

        // Basic span tags (without complex attributes)
        text = text.replace("&lt;span&gt;", "<span>");
        text = text.replace("&lt;/span&gt;", "</span>");

        return text;
    }
}