/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ColumnMetaData.java,v 1.3 2004-06-07 08:31:56 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view;

/**
 * own wrapper for the column meta data.
 */
public final class ColumnMetaData {
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_CENTER = 2;
    public static final int ALIGN_RIGHT = 3;

    /** alignment; one of left, center, right. */
    private final int _alignment;

    /** the header of this column. */
    private final String _label;

    /** minimum width of this column; ususally set by the header width. */
    private final int _initialWidth;

    /** wrap columns automatically at this column; -1 = disabled. */
    private int _autoWrapCol;

    private int _width;
    private boolean _display;

    public ColumnMetaData(final String header, final int align) {
        this(header, align, -1);
    }

    /**
     * publically available constructor for the user.
     */
    public ColumnMetaData(final String header, final int align, final int autoWrap) {
        _label = header;
        _initialWidth = header.length();
        _width = _initialWidth;
        _alignment = align;
        _display = true;
        _autoWrapCol = autoWrap;
    }

    public ColumnMetaData(final String header) {
        this(header, ALIGN_LEFT);
    }

    public void resetWidth() {
        _width = _initialWidth;
    }

    /**
     * set, whether a specific column should be displayed.
     */
    public void setDisplay(final boolean val) {
        _display = val;
    }

    public boolean doDisplay() {
        return _display;
    }

    public void setAutoWrap(final int col) {
        _autoWrapCol = col;
    }

    public int getAutoWrap() {
        return _autoWrapCol;
    }

    int getWidth() {
        return _width;
    }

    String getLabel() {
        return _label;
    }

    public int getAlignment() {
        return _alignment;
    }

    void updateWidth(final int w) {
        if (w > _width) {
            _width = w;
        }
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
