/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.updater;

/**
 *
 * @author teras
 */
public interface UpdaterCallback {

        public abstract void actionCommit();
        public abstract void actionDefer();
        public abstract void actionIgnore();
}
