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
 * One column in the table. This column contains both: the actual data to be
 * printed and the state to print it; this state is represented by the
 * 'position', the internal row if this is an multirow column. This is ok, since
 * this Column is only used once to be filled and once to be printed.
 */
public class Column {

    private static final String NULL_TEXT = "[NULL]";
    private static final int NULL_LENGTH = NULL_TEXT.length();

    private String _columnText[]; // multi-rows
    private int _width;

    /** This holds a state for the renderer. */
    private int _pos;

    public Column(final long value) {
        this(String.valueOf(value));
    }

    public Column(final String text) {
        if (text == null) {
            _width = NULL_LENGTH;
            _columnText = null;
        } else {
            _width = 0;
            final StringTokenizer tok = new StringTokenizer(text, "\n\r");
            _columnText = new String[tok.countTokens()];
            for (int i = 0; i < _columnText.length; ++i) {
                final String line = (String) tok.nextElement();
                final int lWidth = line.length();
                _columnText[i] = line;
                if (lWidth > _width) {
                    _width = lWidth;
                }
            }
        }
        _pos = 0;
    }

    /**
     * Split a line at the nearest whitespace.
     */
    private String[] splitLine(final String str, final int autoCol) {
        final ArrayList<String> tmpRows = new ArrayList<String>(5);
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
            } else {
                tmpRows.add(str.substring(lastPos, pos));
                lastPos = pos + /* skip space: */1;
            }
            pos = lastPos + autoCol;
        }
        if (lastPos < strLen - 1) {
            tmpRows.add(str.substring(lastPos));
        }
        return tmpRows.toArray(new String[tmpRows.size()]);
    }

    /**
     * replaces a row with multiple other rows.
     */
    private String[] replaceRow(final String[] orig, final int pos, final String[] other) {
        final String result[] = new String[orig.length + other.length - 1];
        System.arraycopy(orig, 0, result, 0, pos);
        System.arraycopy(other, 0, result, pos, other.length);
        System.arraycopy(orig, pos + 1, result, pos + other.length, orig.length
                - pos - 1);
        return result;
    }

    /**
     * Set autowrapping at a given column.
     */
    void setAutoWrap(final int autoWrapCol) {
        if (autoWrapCol < 0 || _columnText == null) {
            return;
        }
        _width = 0;
        for (int i = 0; i < _columnText.length; ++i) {
            final int colWidth = _columnText[i].length();
            if (colWidth > autoWrapCol) {
                final String[] multiRows = splitLine(_columnText[i], autoWrapCol);
                for (int j = 0; j < multiRows.length; ++j) {
                    final int l = multiRows[j].length();
                    if (l > _width) {
                        _width = l;
                    }
                }
                _columnText = replaceRow(_columnText, i, multiRows);
                i += multiRows.length; // next loop pos here.
            } else {
                if (colWidth > _width) {
                    _width = colWidth;
                }
            }
        }
    }

    // package private methods for the table renderer.
    int getWidth() {
        return _width;
    }

    boolean hasNextLine() {
        return _columnText != null && _pos < _columnText.length;
    }

    boolean isNull() {
        return _columnText == null;
    }

    String getNextLine() {
        String result = "";
        if (_columnText == null) {
            if (_pos == 0) {
                result = NULL_TEXT;
            }
        } else if (_pos < _columnText.length) {
            result = _columnText[_pos];
        }
        ++_pos;
        return result;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
