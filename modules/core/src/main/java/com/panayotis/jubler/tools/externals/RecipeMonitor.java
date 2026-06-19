/*
 * (c) 2005-2025 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package com.panayotis.jubler.tools.externals;

/**
 * UI sink for a running recipe. The execution engine ({@link RecipeExecutor}) talks to
 * the lifecycle view through this interface, so the engine is independent of the
 * concrete widgets (log / progress) used to show it.
 */
public interface RecipeMonitor {

    /** Append a line to the log view. May be called from a background thread. */
    void log(String line);

    /** Hand over the live process so a Cancel action can destroy it. */
    void setProcess(Process process);

    /** True when the user requested cancellation; the engine stops and skips apply-back. */
    boolean isCancelled();

    /**
     * The run is over.
     *
     * @param success true if the tool exited cleanly and apply-back (if any) succeeded
     * @param message a short human-readable result/error message
     */
    void finished(boolean success, String message);
}
