/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.tools;

import com.panayotis.jubler.JubFrame;
import com.panayotis.jubler.plugins.PluginManager;
import com.panayotis.jubler.tools.ToolMenu.Location;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumMap;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

/**
 *
 * @author teras
 */
public class ToolsManager {
    
    public static final ToolsManager manager = new ToolsManager();
    private final EnumMap<Location, ArrayList<GenericTool>> tools = new EnumMap<Location, ArrayList<GenericTool>>(Location.class);
    
    @SuppressWarnings("LeakingThisInConstructor")
    private ToolsManager() {
        PluginManager.manager.callPluginListeners(this);
    }
    
    public void add(GenericTool tool) {
        ArrayList<GenericTool> list = tools.get(tool.toolmenu.location);
        if (list == null) {
            list = new ArrayList<GenericTool>();
            tools.put(tool.toolmenu.location, list);
        }
        list.add(tool);
    }
    
    public void register(JubFrame current) {
        // Backup existing tools menu
        Component[] oldtools = current.ToolsM.getMenuComponents();
        current.ToolsM.removeAll();
        try {
            /* Populate tools menu */
            for (GenericTool tool : tools.get(Location.FILETOOL))
                addMenu(current, current.ToolsM, tool);
            current.ToolsM.add(new JSeparator());
            for (GenericTool tool : tools.get(Location.TIMETOOL))
                addMenu(current, current.ToolsM, tool);
            current.ToolsM.add(new JSeparator());
            for (GenericTool tool : tools.get(Location.CONTENTTOOL))
                addMenu(current, current.ToolsM, tool);
            current.ToolsM.add(new JSeparator());
            setFileToolsStatus(current, false);

            /* Populate edit menu */
            for (GenericTool tool : tools.get(Location.DELETE))
                addMenu(current, current.DeleteEM, tool);
            for (GenericTool tool : tools.get(Location.MARK))
                addMenu(current, current.MarkEM, tool);
            for (GenericTool tool : tools.get(Location.STYLE))
                addMenu(current, current.StyleEM, tool);
        } catch (NullPointerException ex) {            
        }
        // Restore tools menu old entries
        for (Component comp : oldtools)
            current.ToolsM.add(comp);
    }
    
    private void addMenu(final JubFrame current, final JMenu ToolsM, final GenericTool tool) {
        JMenuItem item = new JMenuItem(tool.toolmenu.text, tool.toolmenu.key);
        item.setName(tool.toolmenu.name);
        ToolsM.add(item);
        item.addActionListener(new ActionListener()             {
            
            public void actionPerformed(ActionEvent e) {
                tool.execute(current);
            }
        });
    }

    /* 
     * Join and Reparent are in the first block of menu, or else this code will break,
     * since it searches for the first separator item
     */
    public void setFileToolsStatus(JubFrame current, boolean status) {
        JMenuItem Join = null;
        JMenuItem Reparent = null;
        for (Component item : current.ToolsM.getMenuComponents())
            if (item instanceof JMenuItem) {
                if ("TJO".equals(((JMenuItem) item).getName()))
                    Join = (JMenuItem) item;
                else if ("TPA".equals(((JMenuItem) item).getName()))
                    Reparent = (JMenuItem) item;
            } else
                break;
        if (Join != null)
            Join.setEnabled(status);
        if (Reparent != null)
            Reparent.setEnabled(status);
    }
}
