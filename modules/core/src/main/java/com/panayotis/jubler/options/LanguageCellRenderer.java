package com.panayotis.jubler.options;

import com.panayotis.jubler.theme.Theme;

import javax.swing.*;
import java.awt.*;

public class LanguageCellRenderer extends DefaultListCellRenderer {
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = 8;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value == null) {
            JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            return separator;
        }
        
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof LanguageOption) {
            LanguageOption option = (LanguageOption) value;
            label.setText(option.getDisplayName());
            
            ImageIcon icon = Theme.loadIcon(option.getIconName());
            if (icon != null) {
                label.setIcon(icon);
                label.setIconTextGap(ICON_PADDING);
            }
        }
        
        return label;
    }
}
