/*
 *
 * This file is part of ApplicationEnhancer.
 *
 * ApplicationEnhancer is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2.
 *
 *
 * ApplicationEnhancer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Jubler; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package com.panayotis.appenh;

import java.awt.Image;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

abstract class DefaultEnhancer implements Enhancer {

    private Image frameImage;

    public boolean setNimbusLookAndFeel() {
        for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels())
            if ("Nimbus".equals(info.getName()))
                try {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    return true;
                } catch (ClassNotFoundException ex) {
                } catch (InstantiationException ex) {
                } catch (IllegalAccessException ex) {
                } catch (UnsupportedLookAndFeelException ex) {
                }
        return false;
    }

    public boolean setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            return true;
        } catch (ClassNotFoundException ex1) {
        } catch (InstantiationException ex1) {
        } catch (IllegalAccessException ex1) {
        } catch (UnsupportedLookAndFeelException ex1) {
        }
        return false;
    }

    @Override
    public void registerPreferences(Runnable callback) {
    }

    @Override
    public void registerAbout(Runnable callback) {
    }

    @Override
    public void requestAttention() {
    }

    @Override
    public void registerQuit(Runnable callback) {
    }

    @Override
    public void registerFileOpen(Enhancer.FileOpenRunnable callback) {
    }

    @Override
    public boolean providesSystemMenus() {
        return false;
    }

    @Override
    public void setApplicationIcon(String iconResourceName) {
        frameImage = EnhancerManager.getImage(iconResourceName);
    }

    @Override
    public void updateFrameIcon(JFrame frame) {
        if (frameImage != null)
            frame.setIconImage(frameImage);
    }
}
