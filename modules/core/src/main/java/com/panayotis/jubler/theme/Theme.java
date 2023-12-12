/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.theme;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Theme {
    private static Provider provider = null;

    public static void setProvider(Provider given) {
        if (given == null)
            throw new NullPointerException("Provider can't be null");
        Theme.provider = given;
    }

    private static Provider current() {
        if (provider == null)
            throw new IllegalArgumentException("Provider not set yet");
        return provider;
    }

    public static ImageIcon loadIcon(String name) {
        return loadIcon(name, 1);
    }

    public static ImageIcon loadIcon(String name, float resize) {
        return loadIcon(name, IconStatus.NORMAL, resize);
    }

    public static ImageIcon loadIcon(String name, IconStatus status) {
        return loadIcon(name, status, 1);
    }

    public static ImageIcon loadIcon(String name, IconStatus status, float resize) {
        return loadIcon(current().loadIcon(name, resize), status);
    }

    public static ImageIcon loadIcon(ImageIcon otherIcon, IconStatus status) {
        return status.convert(otherIcon);
    }

    public static List<Image> findFrameImages(String name) {
        return current().findFrameImages(name);
    }

    public interface Provider {
        ImageIcon loadIcon(String name, float resize);

        List<Image> findFrameImages(String name);
    }
}
