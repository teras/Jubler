/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.media.player.vlc;

import com.panayotis.jubler.os.MissingProgram;

import uk.co.caprica.vlcj.factory.discovery.strategy.LinuxNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.factory.discovery.strategy.NativeDiscoveryStrategy;
import uk.co.caprica.vlcj.factory.discovery.strategy.OsxNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.factory.discovery.strategy.WindowsNativeDiscoveryStrategy;

import static com.panayotis.jubler.i18n.I18N.__;

/**
 * Single place that tells the user, once per session, that libvlc could not be
 * loaded. It distinguishes the two cases that otherwise look identical:
 * <ul>
 * <li><b>VLC is not installed</b> — show how to install it.</li>
 * <li><b>VLC IS installed but could not be loaded</b> — the library was located on
 *     disk yet the operating system refused to load it. That points at an
 *     incompatibility with this system (rather than a missing VLC), so we say so
 *     and suggest reinstalling the matching build.</li>
 * </ul>
 * The "found vs not found" decision is delegated entirely to vlcj's own native
 * discovery <em>strategies</em>, which know the per-platform install locations: we
 * merely ask them to LOCATE libvlc on disk (a pure directory scan), never to load
 * it. We must not trigger another load here — the failed factory load has already
 * poisoned vlcj's {@code LibVlc} class, so any further load attempt throws
 * {@code NoClassDefFoundError} rather than reporting a clean "not found".
 * <p>
 * Both the video preview ({@link VLCPreview}) and the audio waveform
 * ({@link VLCAudioPreview}) funnel their native-load failure here so the user gets
 * the same, actionable dialog no matter which one is touched first.
 */
final class VLCMissing {

    private VLCMissing() {
    }

    /**
     * Show the platform-specific dialog (at most once per session — {@link MissingProgram}
     * dedups on the {@code "VLC"} key). When vlcj could locate VLC but not load it, the
     * text points at a likely incompatibility instead of wrongly claiming VLC is missing.
     */
    static void warn() {
        if (isInstalledButUnloadable()) {
            MissingProgram.warn("VLC",
                    __("VLC could not be loaded"),
                    __("VLC is installed, but Jubler could not load it, so the video preview and audio waveform are unavailable. This usually means the installed VLC is not compatible with your system — for instance the VLC build does not match your operating system or processor."),
                    __("Reinstall VLC with the build that matches your Mac from https://www.videolan.org/vlc/\nor with Homebrew:  brew install --cask vlc"),
                    __("Reinstall VLC with the build that matches your system from https://www.videolan.org/vlc/"),
                    __("Reinstall VLC through your distribution's package manager so it matches your system."));
        } else {
            MissingProgram.warn("VLC",
                    __("VLC not found"),
                    __("VLC is required for the video preview and audio waveform, but it could not be found on your system."),
                    __("Install VLC with Homebrew:\n    brew install --cask vlc\nor download it from https://www.videolan.org/vlc/"),
                    __("Download VLC from https://www.videolan.org/vlc/\nand install it."),
                    __("Install VLC with your distribution's package manager, e.g.:\n    Debian/Ubuntu:  sudo apt install vlc\n    Fedora:         sudo dnf install vlc\n    Arch:           sudo pacman -S vlc"));
        }
    }

    /**
     * True when a VLC installation is present on disk (so a failed load means it is
     * incompatible), false when no VLC could be located at all (so it is missing).
     * Each strategy's {@code discover()} only scans its platform's install locations
     * for the libvlc files — it does not load anything — so it is safe to call after
     * the load has already failed. All path knowledge stays inside vlcj.
     */
    private static boolean isInstalledButUnloadable() {
        NativeDiscoveryStrategy[] strategies = {
                new LinuxNativeDiscoveryStrategy(),
                new OsxNativeDiscoveryStrategy(),
                new WindowsNativeDiscoveryStrategy(),
        };
        for (NativeDiscoveryStrategy strategy : strategies) {
            try {
                if (strategy.supported() && strategy.discover() != null)
                    return true;
            } catch (Throwable t) {
                // A misbehaving strategy must not decide the outcome; try the next one.
            }
        }
        return false;
    }
}
