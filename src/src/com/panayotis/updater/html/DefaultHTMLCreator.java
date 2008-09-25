/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.updater.html;
import java.awt.Color;
import static com.panayotis.jubler.i18n.I18N._;

/**
 *
 * @author teras
 */
public class DefaultHTMLCreator implements UpdaterHTMLCreator {

    private StringBuffer data;
    private String backcolor;

    public DefaultHTMLCreator() {
        data = new StringBuffer();
        data.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
        data.append("<html>\n");
        data.append("<head><title></title></head>\n");
        data.append("<body>\n");
        setHeaderBackColor(new Color(210, 210, 230));
    }

    public void addInfo(String lastrelease, String information) {
        data.append("<table style=\"text-align: left; margin-top: 6px; margin-left: 10px; margin-right: 10px;\" border=\"0\" cellpadding=\"4\">\n");
        data.append("<tr><td style=\"background-color: "+backcolor+";\"><span style=\"font-weight: bold;\">");
        data.append(_("Version"));
        data.append(": ").append(lastrelease);
        data.append("</span></td></tr>\n<tr><td>");
        data.append(information);
        data.append("</td></tr>\n");
        data.append("</table><br>\n");
    }

    public String getHTML() {
        return data.toString() + "</body>\n</html>\n";
    }
    
    public void setHeaderBackColor(Color c) {
        backcolor = "rgb("+c.getRed()+", "+c.getBlue()+", "+c.getGreen()+")";
    }
}
