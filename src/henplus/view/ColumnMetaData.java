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
    public static final int ALIGN_LEFT   = 1;
    public static final int ALIGN_CENTER = 2;
    public static final int ALIGN_RIGHT  = 3;

    /** alignment; one of left, center, right */
    private final int     alignment;

    /** the header of this column */
    private final String  label;

    /** minimum width of this column; ususally set by the header width */
    private final int     initialWidth;

    /** wrap columns automatically at this column; -1 = disabled */
    private int     autoWrapCol;

    private       int     width;
    private       boolean display;

    public ColumnMetaData(String header, int align) {
        this(header, align, -1);
    }

    /**
     * publically available constructor for the
     * user.
     */
    public ColumnMetaData(String header, int align, int autoWrap) {
	label = header;
	initialWidth = header.length();
	width = initialWidth;
	alignment = align;
	display = true;
        autoWrapCol = autoWrap;
    }
    
    public ColumnMetaData(String header) {
	this(header, ALIGN_LEFT);
    }
    
    public void resetWidth() { width = initialWidth; }

    /**
     * set, whether a specific column should be displayed.
     */
    public void setDisplay(boolean val) { display = val; }
    public boolean doDisplay() { return display; }

    public void setAutoWrap(int col) {
        autoWrapCol = col;
    }
    public int getAutoWrap() { 
        return autoWrapCol; 
    }

    int getWidth()     { return width; }
    String getLabel()  { return label; }
    public int getAlignment() { return alignment; }
    void updateWidth(int w) {
	if (w > width) {
	    width = w;
	}
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
