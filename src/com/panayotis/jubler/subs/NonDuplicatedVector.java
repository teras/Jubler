/*
 * NonDuplicatedVector.java
 *
 * Created on 23 July 2008, 23:14
 *
 * This file is part of Jubler.
 *
 * Jubler is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * Jubler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 */
package com.panayotis.jubler.subs;

import java.util.Vector;

/**
 * The non-duplicated vector allows items to be stored
 * uniquely. Items to be inserted are checked for their existence,
 * before insertions performed. Checking is based on the result of
 * {@link Object#equals} method.
 * @author Hoang Duy Tran
 */
public class NonDuplicatedVector<E> extends Vector<E> {
    
    /** Creates a new instance of NonDuplicatedVector */
    public NonDuplicatedVector() {
        super();
    }
    
    /**
     * Appends the specified element to the end of this Vector.
     *
     * @param e element to be appended to this Vector
     * @return {@code true}
     * @since 1.2
     */
    @Override
    public synchronized boolean add(E e) {
        if (!contains(e)) {
            return super.add(e);
        } else {
            return false;
        }
    }
    
    /**
     * Overrides the {@link Vector#addElement addElement}
     * of the {@link Vector} class to check for the existence
     * of an object before calling the {@link Vector#addElement}
     * to add the object to the list.
     * @param obj Object to be added to the {@link Vector}
     */
    @Override
    public synchronized void addElement(E obj) {
        this.add(obj);
    }
    
    @Override
    public void add(int index, E element) {
        this.insertElementAt(element, index);
    }

    public void insertAtTop(E element) {
        this.insertElementAt(element, 0);
    }
    
    /**
     * Overrides the {@link Vector#insertElementAt insertElementAt}
     * of the {@link Vector} class to check for the existence
     * of an object before calling the {@link Vector#insertElementAt}
     * to add the object to the list. A downward shifting will be performed
     * from the chosen index.
     * 
     * @param obj Object to be added to the {@link Vector}
     * @param index Zero based integer index represents the location at which
     * the object is to be added to the list.
     */
    @Override
    public synchronized void insertElementAt(E obj, int index) {
        if (!contains(obj)) {
            boolean is_last_item = (index >= size()-1);
            if (is_last_item){
                super.add(obj);
            }else{
                super.insertElementAt(obj, index);
            }//end if
        }//end if
    }
}
