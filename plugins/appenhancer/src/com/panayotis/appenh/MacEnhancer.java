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

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

@SuppressWarnings("UseSpecificCatch")
public class MacEnhancer implements Enhancer {

    private static final Class appClass;
    private static final Object appInstance;

    static {
        Class aCass = null;
        Object aInst = null;
        try {
            aCass = Class.forName("com.apple.eawt.Application");
            aInst = aCass.getMethod("getApplication", (Class[]) null).invoke(null, (Object[]) null);
        } catch (Exception ex) {
        }
        appClass = aCass;
        appInstance = aInst;
    }

    @Override
    public void registerAbout(final Runnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName("com.apple.eawt.AboutHandler");
                appClass.getMethod("setAboutHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("handleAbout"))
                            callback.run();
                        return null;
                    }
                }));
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void registerPreferences(final Runnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName("com.apple.eawt.PreferencesHandler");
                appClass.getMethod("setPreferencesHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("handlePreferences"))
                            callback.run();
                        return null;
                    }
                }));
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void registerQuit(final Runnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName("com.apple.eawt.QuitHandler");
                appClass.getMethod("setQuitHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("handleQuitRequestWith")) {
                            callback.run();
                            if (args != null && args.length > 1)
                                args[1].getClass().getMethod("cancelQuit", (Class[]) null).invoke(args[1], (Object[]) null);
                        }
                        return null;
                    }
                }));
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void registerFileOpen(final FileOpenRunnable callback) {
        try {
            if (appInstance != null) {
                Class handler = Class.forName("com.apple.eawt.OpenFilesHandler");
                appClass.getMethod("setOpenFileHandler", handler).invoke(appInstance, Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{handler}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("openFiles") && args != null && args.length > 0) {
                            Method m = args[0].getClass().getMethod("getFiles", (Class[]) null);
                            List<File> list = (List<File>) m.invoke(args[0], (Object[]) null);
                            for (File f : list)
                                try {
                                    callback.openFile(f);
                                } catch (Exception ex) {
                                }
                        }
                        return null;
                    }
                }));
            }
        } catch (Exception ex) {
        }
    }

    @Override
    public void requestAttention() {
        try {
            if (appInstance != null)
                appClass.getMethod("requestUserAttention", boolean.class).invoke(appInstance, true);
        } catch (Exception ex) {
        }
    }

    @Override
    public void setSafeLookAndFeel() {
    }

    @Override
    public void setDefaultLookAndFeel() {
    }

    @Override
    public boolean providesSystemMenus() {
        return true;
    }

}
