/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ColumnMetaData.java,v 1.3 2002-02-14 17:09:49 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.util;

/**
 * own wrapper for the column meta data.
 */
public final class ColumnMetaData {
    public static final int ALIGN_LEFT   = 1;
    public static final int ALIGN_CENTER = 2;
    public static final int ALIGN_RIGHT  = 3;

    private final int     alignment;
    private final String  label;
    private final int     initialWidth;
    private       int     width;

    /**
     * publically available constructor for the
     * user.
     */
    public ColumnMetaData(String header, int align) {
	label = header;
	initialWidth = header.length();
	width = initialWidth;
	alignment = align;
    }
    
    public ColumnMetaData(String header) {
	this(header, ALIGN_LEFT);
    }
    
    public void reset() { width = initialWidth; }

    int getWidth()     { return width; }
    String getLabel()  { return label; }
    int getAlignment() { return alignment; }
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
