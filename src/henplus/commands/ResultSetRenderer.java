/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ResultSetRenderer.java,v 1.3 2002-02-07 11:02:19 hzeller Exp $ 
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

import java.util.StringTokenizer;

/**
 * document me.
 */
public class ResultSetRenderer {
    private static final int ALIGN_LEFT   = 1;
    private static final int ALIGN_CENTER = 2;
    private static final int ALIGN_RIGHT  = 3;
    private static final int MAX_CACHE_ELEMENTS = 20000;
    private static final int LIMIT = 200;

    private final ResultSet rset;
    private final ColumnMetaData meta[];
    
    public ResultSetRenderer(ResultSet rset) throws SQLException {
	this.rset = rset;
	this.meta = getDisplayMeta(rset.getMetaData());
    }

    public String writeTo(PrintStream out) throws SQLException {
	int rows = 0;
	boolean beyondLimit = false;

	Vector cacheRows = new Vector();
	try {
	    while (rset.next()) {
		Column[] currentRow = new Column[meta.length];
		for (int i = 0 ; i < meta.length ; ++i) {
		    String colString = rset.getString(i+1);
		    Column thisCol = new Column(colString); 
		    currentRow[i] = thisCol;
		    meta[i].updateWidth(thisCol.getWidth());
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
		Column[] currentRow = (Column[]) rowIterator.next();
		boolean hasMoreLines;
		do {
		    hasMoreLines = false;
		    for (int i = 0 ; i < meta.length ; ++i) {
			String txt;
			out.print(" ");
			txt = formatString (currentRow[i].getNextLine(), ' ',
					    meta[i].getWidth(),
					    meta[i].getAlignment());
			hasMoreLines |= currentRow[i].hasNextLine();
			out.print(txt);
			out.print(" |");
		    }
		    out.println();
		}
		while (hasMoreLines);
	    }
	    if (rows > 0) {
		printHorizontalLine(out);
	    }
	    // count stuff beyond limit.
	    if (beyondLimit) {
		System.err.println("limit " + LIMIT + " reached ..");
		return "> " + String.valueOf(LIMIT);
	    }
	}
	finally {
	    rset.close();
	}
	return String.valueOf(rows);
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

    private static final class Column {
	private final static String NULL_TEXT = "[NULL]";
	private final static int    NULL_LENGTH = NULL_TEXT.length();

	private final String columnText[]; // multi-rows
	private int    width;
	private int    pos;

	public Column(String text) {
	    if (text == null) {
		width = NULL_LENGTH;
		columnText = null;
	    }
	    else {
		width = 0;
		StringTokenizer tok = new StringTokenizer(text, "\n");
		columnText = new String [ tok.countTokens() ];
		for (int i=0; i < columnText.length; ++i) {
		    String line = (String) tok.nextElement();
		    int    lWidth = line.length();
		    columnText[i] = line;
		    if (lWidth > width) {
			width = lWidth;
		    }
		}
	    }
	    pos = 0;
	}
	
	public int getWidth() {
	    return width;
	}

	public boolean hasNextLine() {
	    return (columnText != null && pos < columnText.length);
	}

	public String getNextLine() {
	    String result = "";
	    if (columnText == null) {
		if (pos == 0) result = NULL_TEXT;
	    }
	    else if (pos < columnText.length) {
		result = columnText[pos];
	    }
	    ++pos;
	    return result;
	}
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
