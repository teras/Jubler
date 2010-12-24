/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.media.player.terminals;

/**
 *
 * @author teras
 */
public interface Validator<T> {

    public boolean exec(T data);
}
