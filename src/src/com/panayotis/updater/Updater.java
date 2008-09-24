/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater;

import com.panayotis.updater.changelog.ChangeLog;
import com.panayotis.updater.list.Version;

/**
 *
 * @author teras
 */
public class Updater {

    public static void main(String[] args) {
        Version vers = Version.loadChangeLog("file:////Users/teras/Works/Development/Java/Jubler/resources/system/updater.xml");
        System.out.println(vers.toString());
    }
}
