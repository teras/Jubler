/*
 * (c) 2005-2023 by Panayotis Katsaloulis
 * SPDX-License-Identifier: AGPL-3.0-only
 * This file is part of Jubler.
 */

package  com.panayotis.jubler.subs.loader.text;

import com.panayotis.jubler.subs.SubEntry;
import com.panayotis.jubler.subs.loader.AbstractTextSubFormat;
import com.panayotis.jubler.time.Time;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MicroDVD extends AbstractTextSubFormat {

    private static final Pattern pat;

    /**
     * Creates a new instance of SubFormat
     */
    static {
        pat = Pattern.compile("\\{(\\d+)\\}" + sp + "\\{(\\d+)\\}(.*?)" + nl);
    }

    protected Pattern getPattern() {
        return pat;
    }

    protected SubEntry getSubEntry(Matcher m) {
        Time start = new Time(m.group(1), FPS);
        Time finish = new Time(m.group(2), FPS);
        return new SubEntry(start, finish, m.group(3).replace("|", "\n"));
    }

    public String getExtension() {
        return "sub";
    }

    public String getName() {
        return "MicroDVD";
    }

    @Override
    public String getExtendedName() {
        return "MicroDVD SUB file";
    }

    protected void appendSubEntry(SubEntry sub, StringBuilder str) {
        str.append("{");
        str.append(sub.getStartTime().getFrames(FPS));
        str.append("}{");
        str.append(sub.getFinishTime().getFrames(FPS));
        str.append("}");
        str.append(sub.getText().replace('\n', '|'));
        str.append("\n");
    }

    public boolean supportsFPS() {
        return true;
    }
}
