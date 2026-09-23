/*
 * (c) 2005-2026 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

#pragma once

// Linux AppImage: nothing installs Jubler, so on every start it puts itself in
// the user's application menu, as the Java release did through appenh
// (LinuxEnhancer.registerApplication): a desktop entry that runs this AppImage,
// the icons, the file manager's icon and thumbnail of the AppImage file.
// Nothing happens anywhere else (packages bring their own entry; Flatpak too).
namespace DesktopIntegration {
void registerAppImage();
}
