/*
 * IntegerComboBoxModel.java
 *
 * Created on 21 September 2008, 09:51
 *
 * Copyright 2007-2008 Hoang Duy Tran, All rights reserved.
 *
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
 * Contributor(s):
 *
 */

package com.panayotis.jubler.options;

import java.util.Vector;
import javax.swing.DefaultComboBoxModel;

/**
 * Extends the {@link DefaultComboBoxModel} to make this class works with
 * Integer only.
 * @author hdq
 */
public class IntegerComboBoxModel extends DefaultComboBoxModel{

    /**
     * Creates a new instance of IntegerComboBoxModel
     */
    public IntegerComboBoxModel() {
        super();
    }

    /**
     * Constructs a IntegerComboBoxModel object initialized with
     * an array of Integers.
     *
     *
     * @param items  an array of Integer objects
     */
    public IntegerComboBoxModel(final Integer items[]) {
        super(items);
    }

    public IntegerComboBoxModel(final int items[]) {
        if (items == null)
            return;

        for(int item : items)
            this.addElement(item);
    }

    /**
     * Constructs a IntegerComboBoxModel object initialized with
     * an array of Integers.
     *
     *
     * @param items  an array of Integer objects
     */
    public IntegerComboBoxModel(Object items[]) {
        super(items);
    }

    /**
     * Constructs a IntegerComboBoxModel object initialized with
     * a vector.
     *
     *
     * @param v  a Vector of Integers ...
     */
    public IntegerComboBoxModel(Vector<Integer> v) {
        super(v);
    }

    /**
     * Set the value of the selected item. The selected item may be null.
     * @param number The combo box value or null for no selection.
     */
    public void setSelectedItem(Integer number) {
        super.setSelectedItem(number);
    }

    /**
     * Add an integer to the list
     * @param number The integer to add
     */
    public void addElement(Integer number) {
        super.addElement(number);
    }

    /**
     * Insert an Integer at the specified index.
     * @param number The Integer to add
     * @param index The index for the Integer.
     */
    public void insertElementAt(Integer number,int index) {
        super.insertElementAt(number, index);
    }
}
