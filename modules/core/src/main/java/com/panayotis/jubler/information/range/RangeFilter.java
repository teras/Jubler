/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.information.range;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.function.Consumer;

public abstract class RangeFilter<T> extends DocumentFilter {
    private final Consumer<T> consumer;

    public RangeFilter(Consumer<T> consumer) {
        this.consumer = consumer;
    }

    private void onNewValue(String value) {
        if (value != null && !value.isEmpty()) {
            T result = processValue(value);
            if (result != null)
                consumer.accept(result);
        }
    }

    public void setDocumentFilter(JTextField field) {
        Document doc = field.getDocument();
        if (doc instanceof AbstractDocument) {
            AbstractDocument ad = (AbstractDocument) doc;
            ad.setDocumentFilter(this);
            ad.addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    onNewValue(field.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    onNewValue(field.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    onNewValue(field.getText());
                }
            });
        }
    }

    protected abstract T processValue(String value);
}
