/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: Column.java,v 1.6 2005-06-18 04:58:13 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * One column in the table.
 * This column contains both: the actual data to be printed and the
 * state to print it; this state is represented by the 'position', the
 * internal row if this is an multirow column. This is ok, since this
 * Column is only used once to be filled and once to be printed.
 */
public class Column {
    
    private final static String NULL_TEXT = "[NULL]";
    private final static int NULL_LENGTH = NULL_TEXT.length();

    private String columnText[]; // multi-rows
    private int width;

    /** This holds a state for the renderer */
    private int pos;

    public Column(long value) {
        this(String.valueOf(value));
    }

    public Column(String text) {
        if (text == null) {
            width = NULL_LENGTH;
            columnText = null;
        }
        else {
            width = 0;
            StringTokenizer tok = new StringTokenizer(text, "\n\r");
            columnText = new String[tok.countTokens()];
            for (int i = 0; i < columnText.length; ++i) {
                String line = (String)tok.nextElement();
                int lWidth = line.length();
                columnText[i] = line;
                if (lWidth > width) {
                    width = lWidth;
                }
            }
        }
        pos = 0;
    }

    /**
     * Split a line at the nearest whitespace.
     */
    private String[] splitLine(String str, int autoCol) {
        ArrayList/*<String>*/ tmpRows = new ArrayList/*<String>*/(5);
        int lastPos = 0;
        int pos = lastPos + autoCol;
        final int strLen = str.length();
        while (pos < strLen) {
            while (pos > lastPos && !Character.isWhitespace(str.charAt(pos))) {
                pos--;
            }
            if (pos == lastPos) { // no whitespace found: hard cut
                tmpRows.add(str.substring(lastPos, lastPos + autoCol));
                lastPos = lastPos + autoCol;
            }
            else {
                tmpRows.add(str.substring(lastPos, pos));
                lastPos = pos + /* skip space: */ 1;
            }
            pos = lastPos + autoCol;
        }
        if (lastPos < strLen-1) {
            tmpRows.add(str.substring(lastPos));
        }
        return (String[]) tmpRows.toArray(new String[ tmpRows.size() ]);
    }

    /**
     * replaces a row with multiple other rows.
     */
    private String[] replaceRow(String[] orig, int pos, String[] other) {
        String result[] = new String[ orig.length + other.length - 1];
        System.arraycopy(orig, 0, result, 0, pos);
        System.arraycopy(other, 0, result, pos, other.length);
        System.arraycopy(orig, pos+1, result, pos + other.length, 
                         orig.length-pos-1);
        return result;
    }

    /**
     * Set autowrapping at a given column.
     */
    void setAutoWrap(int autoWrapCol) {
        if (autoWrapCol < 0 || columnText==null) 
            return;
        width = 0;
        for (int i=0; i < columnText.length; ++i) {
            int colWidth = columnText[i].length();
            if (colWidth > autoWrapCol) {
                String[] multiRows = splitLine(columnText[i], autoWrapCol);
                for (int j = 0; j < multiRows.length; ++j) {
                    int l = multiRows[j].length();
                    if (l > width) {
                        width = l;
                    }
                }
                columnText = replaceRow(columnText, i, multiRows);
                i += multiRows.length; // next loop pos here.
            }
            else {
                if (colWidth > width) {
                    width = colWidth;
                }
            }
        }
    }

    // package private methods for the table renderer.
    int getWidth() {
        return width;
    }

    boolean hasNextLine() {
        return (columnText != null && pos < columnText.length);
    }

    boolean isNull() {
        return (columnText == null);
    }

    String getNextLine() {
        String result = "";
        if (columnText == null) {
            if (pos == 0)
                result = NULL_TEXT;
        }
        else if (pos < columnText.length) {
            result = columnText[pos];
        }
        ++pos;
        return result;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
