/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.os;

public class NumericUtils {
    public static boolean areNumbersEqual(double a, double b, double relativeError) {
        return a == b || Math.abs(a - b) <= relativeError * Math.max(Math.abs(a), Math.abs(b));
    }
}
