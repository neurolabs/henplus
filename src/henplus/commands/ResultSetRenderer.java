/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ResultSetRenderer.java,v 1.1 2002-01-20 22:59:02 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import java.io.PrintStream;
/**
 * document me.
 */
public class ResultSetRenderer {
    private static final int ALIGN_LEFT   = 1;
    private static final int ALIGN_CENTER = 2;
    private static final int ALIGN_RIGHT  = 3;

    private final ResultSet rset;
    public ResultSetRenderer(ResultSet rset) {
	this.rset = rset;
    }
    
    public int writeTo(PrintStream out) throws SQLException {
	int rows = 0;
	ColumnMetaData meta[] = getDisplayMeta(rset.getMetaData());
	
	for (int i=0; i < meta.length ; ++i) {
	    String txt;
	    txt = formatString (meta[i].getLabel(), ' ',
				meta[i].getWidth()+3,
				ALIGN_CENTER);
	    out.print(txt);
	}
	out.println();
	
	// draw line -------------+------------+------
	for (int i = 0 ; i < meta.length ; ++i) {
	    String txt;
	    txt = formatString ("", '-', meta[i].getWidth()+2,
				ALIGN_LEFT);
	    out.print(txt);
	    out.print('+');
	}
	out.println();
	
	
	while (rset.next()) {
	    for (int i = 0 ; i < meta.length ; ++i) {
		String txt;
		out.print(" ");
		txt = formatString (rset.getString(i+1), ' ',
				    meta[i].getWidth(),
				    meta[i].getAlignment());
		
		out.print(txt);
		out.print(" |");
	    }
	    out.println();
	    ++rows;
	}
	
	for (int i = 0 ; i < meta.length ; ++i) {
	    String txt;
	    txt = formatString ("", '-', meta[i].getWidth()+2, 
				ALIGN_LEFT);
	    out.print(txt);
	    out.print('+');
	}
	out.println();
	return rows;
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
	    int width = Math.max(m.getColumnDisplaySize(i),
				 columnLabel.length());
	    switch (m.getColumnType(i)) {
	    case Types.NUMERIC:  
	    case Types.INTEGER: 
	    case Types.REAL:
	    case Types.SMALLINT:
	    case Types.TINYINT:
		alignment = ALIGN_RIGHT;
		break;
	    }
	    result[i-1] = new ColumnMetaData(columnLabel,width,alignment);
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
	private final int     width;
	private final int     alignment;
	private final String  label;
	
	public ColumnMetaData(String l, int w, int align) {
	    label = l;
	    width = w;
	    alignment = align;
	}
	public int getWidth()     { return width; }
	public String getLabel()  { return label; }
	public int getAlignment() { return alignment; }
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
