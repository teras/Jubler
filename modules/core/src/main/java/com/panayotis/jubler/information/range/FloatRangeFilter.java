package com.panayotis.jubler.information.range;

/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.util.function.Consumer;

public class FloatRangeFilter extends RangeFilter<Float> {
    private final float min, max;

    public FloatRangeFilter(float min, float max, Consumer<Float> consumer) {
        super(consumer);
        this.min = min;
        this.max = max;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        replace(fb, offset, 0, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attr) throws BadLocationException {
        String newText = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()))
                .replace(offset, offset + length, string)
                .toString();

        if (newText.isEmpty() || newText.matches("\\d*\\.?\\d*")) {
            try {
                float val = Float.parseFloat(newText);
                if (val >= min && val <= max)
                    super.replace(fb, offset, length, string, attr);
            } catch (NumberFormatException ignored) {
                // skip invalid intermediate state like just "."
            }
        }
    }

    @Override
    protected Float processValue(String value) {
        return Float.parseFloat(value);
    }
}