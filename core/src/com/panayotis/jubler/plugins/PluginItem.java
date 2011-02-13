/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.plugins;

/**
 *
 * @author teras
 */
public interface PluginItem {

    public String[] getAffectionList();

    public void execPlugin(Object o);
}
