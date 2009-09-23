/*
 *  NonDuplicatedComboBoxModel.java
 * 
 *  Created on: 23-Sep-2009 at 14:01:51
 * 
 *  
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

package com.panayotis.jubler.tools;

import com.panayotis.jubler.subs.NonDuplicatedVector;
import javax.swing.DefaultComboBoxModel;

/**
 * Extends the DefaultComboBoxModel and replaced the default vector with
 * a non-duplicated one. This vector hold String type only and will check
 * for existing content when new items are added, to avoid duplications.
 * @author hoang_tran <hoangduytran1960@googlemail.com>
 */
public class NonDuplicatedComboBoxModel extends DefaultComboBoxModel{
    private static NonDuplicatedVector<String> ndp = new NonDuplicatedVector<String>();
    public NonDuplicatedComboBoxModel(){
        super(ndp);
    }
}//end public class NonDuplicatedComboBoxModel extends DefaultComboBoxModel
