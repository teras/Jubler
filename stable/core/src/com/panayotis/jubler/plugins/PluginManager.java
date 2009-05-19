/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.plugins;

import java.net.URL;

/**
 *
 * @author teras
 */
public class PluginManager {

    private DynamicClassLoader cl;

    public PluginManager() {
        URL[] urls = DynamicClassLoader.getUrlsFromPaths(new String[]{"lib", "../dist/lib"}, true);
        cl = new DynamicClassLoader(urls);

        for (int i = 0; i < urls.length; i++)
            System.out.println(urls[i]);
    }

    public Object getClass(String classname) {
        Object ret = null;
        try {
            ret = cl.loadClass(classname).newInstance();
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        } catch (ClassNotFoundException ex) {
        }
        return ret;
    }
}
