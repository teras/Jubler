/*
 * TreeWalker.java
 *
 * Created on October 3, 2006, 3:07 AM
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

package com.panayotis.jubler.os;

import com.panayotis.jubler.tools.externals.ExtPath;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author teras
 */
public class TreeWalker {

    public static File searchExecutable(ArrayList<String> application, String[] parameters, String test_signature, String deflt) {
        ArrayList<ExtPath> paths = new ArrayList<ExtPath>();
        paths.add(new ExtPath(deflt, ExtPath.FILE_ONLY));
        SystemDependent.appendSpotlightApplication(application.get(0), paths);
        SystemDependent.appendPathApplication(paths);
        SystemDependent.appendLocateApplication(application.get(0), paths);

        for (ExtPath path : paths) {
            DEBUG.debug("Wizard is looking inside " + path.getPath());
            File f = new File(path.getPath());
            if (path.searchForFile() && (!f.isFile()))
                continue;   // If we want a file and this is not, ignore this entry
            File res = searchExecutable(f, application, parameters, test_signature, path.getRecStatus());
            if (res != null)
                return res;
        }
        return null;
    }

    /* filename is already in lower case... */
    public static File searchExecutable(File root, ArrayList<String> program, String[] parameters, String test_signature, int recursive) {
        if (!root.exists())
            return null;
        if (root.isFile()) {
            if (!root.canRead())
                return null;
            for (String progname : program)
                if (root.getName().toLowerCase().equals(progname) && execIsValid(root, parameters, test_signature))
                    return root;
            /* No valid executable found */
            return null;
        } else {
            if (recursive <= ExtPath.FILE_ONLY)
                return null;   // No more recursive should be done
            recursive--;
            File[] childs = root.listFiles();
            if (childs != null)
                for (int i = 0; i < childs.length; i++) {
                    File res = searchExecutable(childs[i], program, parameters, test_signature, recursive);
                    if (res != null)
                        return res;
                }
        }
        return null;
    }

    /* when no parameters are set, while checking executable,
     * no real execution of the application is required */
    public static boolean execIsValid(File exec, String[] parameters, String test_signature) {
        if (parameters == null || test_signature == null)
            return exec.isFile();

        Process proc = null;
        String[] cmd = new String[parameters.length + 1];
        cmd[0] = exec.getAbsolutePath();
        if (parameters.length > 0)
            System.arraycopy(parameters, 0, cmd, 1, parameters.length);

        try {
            StringBuilder buf = new StringBuilder();
            buf.append("Testing: ");
            for (int i = 0; i < cmd.length; i++)
                buf.append(cmd[i]).append(' ');
            DEBUG.debug(buf.toString());
            proc = Runtime.getRuntime().exec(cmd);
            BufferedReader infopipe = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = infopipe.readLine()) != null)
                if (line.toLowerCase().contains(test_signature)) {
                    DEBUG.debug("Valid executable found: " + exec.getAbsolutePath());
                    proc.destroy();
                    return true;
                }
        } catch (Exception ex) {
        } finally {
            try {
                proc.destroy();
            } catch (Exception e) {
            }
        }
        return false;
    }
}
