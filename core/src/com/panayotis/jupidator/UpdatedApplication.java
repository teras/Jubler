/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jupidator;

/**
 *
 * @author teras
 */
public interface UpdatedApplication {

    public abstract boolean requestRestart();
    public abstract void receiveMessage(String message);
}
