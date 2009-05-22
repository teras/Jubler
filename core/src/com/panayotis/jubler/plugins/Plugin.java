/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.panayotis.jubler.plugins;

/**
 *
 * @author teras
 */
public interface Plugin {

        public String[] getAffectionList();

        public void postInit(Object o);
}
