/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ResultSetRenderer.java,v 1.2 2002-01-26 14:06:52 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Vector;
import java.util.Iterator;
import java.io.PrintStream;
/**
 * document me.
 */
public class ResultSetRenderer {
    private static final int ALIGN_LEFT   = 1;
    private static final int ALIGN_CENTER = 2;
    private static final int ALIGN_RIGHT  = 3;
    private static final int MAX_CACHE_ELEMENTS = 20000;
    private static final int LIMIT = 2000;

    private final ResultSet rset;
    private final ColumnMetaData meta[];
    
    public ResultSetRenderer(ResultSet rset) throws SQLException {
	this.rset = rset;
	this.meta = getDisplayMeta(rset.getMetaData());
    }

    public int writeTo(PrintStream out) throws SQLException {
	int rows = 0;
	boolean beyondLimit = false;

	Vector cacheRows = new Vector();
	while (rset.next()) {
	    String[] currentRow = new String[meta.length];
	    for (int i = 0 ; i < meta.length ; ++i) {
		String colString = rset.getString(i+1);
		currentRow[i] = colString;
		if (colString != null) {
		    meta[i].updateWidth(colString.length());
		}
		else {
		    meta[i].updateWidth("[NULL]".length());
		}
	    }
	    cacheRows.add(currentRow);
	    ++rows;
	    if (rows >= LIMIT) {
		beyondLimit = true;
		break;
	    }
	}

	printTableHeader(out);
	Iterator rowIterator = cacheRows.iterator();
	while (rowIterator.hasNext()) {
	    String[] currentRow = (String[]) rowIterator.next();
	    for (int i = 0 ; i < meta.length ; ++i) {
		String txt;
		out.print(" ");
		txt = formatString (currentRow[i], ' ',
				    meta[i].getWidth(),
				    meta[i].getAlignment());
		
		out.print(txt);
		out.print(" |");
	    }
	    out.println();
	}
	// count stuff beyond limit.
	if (beyondLimit) {
	    System.err.println("limit " + LIMIT + " reached ..");
	    /*
	    while (rset.next()) {
		++rows;
	    }
	    */
	    rset.close();
	}
	if (rows > 0) {
	    printHorizontalLine(out);
	}
	return rows;
    }

    private void printHorizontalLine(PrintStream out) {
	for (int i = 0 ; i < meta.length ; ++i) {
	    String txt;
	    txt = formatString ("", '-', meta[i].getWidth()+2,
				ALIGN_LEFT);
	    out.print(txt);
	    out.print('+');
	}
	out.println();
    }

    private void printTableHeader(PrintStream out) {
	printHorizontalLine(out);
	for (int i=0; i < meta.length ; ++i) {
	    String txt;
	    txt = formatString (meta[i].getLabel(), ' ',
				meta[i].getWidth()+1,
				ALIGN_CENTER);
	    out.print(txt);
	    out.print(" |");
	}
	out.println();
	printHorizontalLine(out);
    }
    
    /**
     * determine meta data necesary for display.
     */
    private ColumnMetaData[] getDisplayMeta(ResultSetMetaData m) 
	throws SQLException {
	ColumnMetaData result[] = new ColumnMetaData [ m.getColumnCount() ];

	for (int i = 1; i <= result.length; ++i) {
	    int alignment  = ALIGN_LEFT;
	    String columnLabel = m.getColumnLabel(i);
	    /*
	    int width = Math.max(m.getColumnDisplaySize(i),
				 columnLabel.length());
	    */
	    switch (m.getColumnType(i)) {
	    case Types.NUMERIC:  
	    case Types.INTEGER: 
	    case Types.REAL:
	    case Types.SMALLINT:
	    case Types.TINYINT:
		alignment = ALIGN_RIGHT;
		break;
	    }
	    result[i-1] = new ColumnMetaData(columnLabel,alignment);
	}
	return result;
    }

    private String formatString (String text,
				 char fillchar, int len, int alignment) {
	StringBuffer fillstr = new StringBuffer();
	
	if (len > 4000) {
	    len = 4000;
	}
	
	if (text == null) {
	    text = "[NULL]";
	}
	int slen = text.length();
	
	if (alignment == ALIGN_LEFT) {
	    fillstr.append(text);
	}
	int fillNumber = len - slen;
	int boundary = 0;
	if (alignment == ALIGN_CENTER) {
	    boundary = fillNumber / 2;
	}
	while (fillNumber > boundary) {
	    fillstr.append (fillchar);
	    --fillNumber;
	}
	if (alignment != ALIGN_LEFT) {
	    fillstr.append(text);
	}
	while (fillNumber > 0) {
	    fillstr.append (fillchar);
	    --fillNumber;
	}
	return fillstr.toString();
    }

    /**
     * own wrapper for the column meta data.
     */
    private static final class ColumnMetaData {
	private final int     alignment;
	private final String  label;
	private       int     width;
	
	public ColumnMetaData(String l, int align) {
	    label = l;
	    width = l.length();
	    alignment = align;
	}
	public int getWidth()     { return width; }
	public String getLabel()  { return label; }
	public int getAlignment() { return alignment; }
	public void updateWidth(int w) {
	    if (w > width) {
		width = w;
	    }
	}
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
