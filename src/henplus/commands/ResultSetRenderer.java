/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ResultSetRenderer.java,v 1.6 2002-02-11 16:33:05 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.util.*;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.io.PrintStream;
import java.util.StringTokenizer;

/**
 * document me.
 */
public class ResultSetRenderer {
    private static final int LIMIT = 2000;

    private final ResultSet rset;
    private final TableRenderer table;
    private final int columns;

    public ResultSetRenderer(ResultSet rset, PrintStream out) 
	throws SQLException {
	this.rset = rset;
	ResultSetMetaData meta = rset.getMetaData();
	columns = meta.getColumnCount();
	table = new TableRenderer(getDisplayMeta(meta),  out);
    }

    public String execute() throws SQLException {
	int rows = 0;
	boolean beyondLimit = false;

	try {
	    while (rset.next()) {
		Column[] currentRow = new Column[ columns ];
		for (int i = 0 ; i < columns ; ++i) {
		    String colString = rset.getString(i+1);
		    Column thisCol = new Column(colString); 
		    currentRow[i] = thisCol;
		}
		table.addRow(currentRow);
		++rows;
		if (rows >= LIMIT) {
		    beyondLimit = true;
		    break;
		}
	    }
	    
	    table.closeTable();

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
    
    /**
     * determine meta data necesary for display.
     */
    private ColumnMetaData[] getDisplayMeta(ResultSetMetaData m) 
	throws SQLException {
	ColumnMetaData result[] = new ColumnMetaData [ m.getColumnCount() ];

	for (int i = 1; i <= result.length; ++i) {
	    int alignment  = ColumnMetaData.ALIGN_LEFT;
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
		alignment = ColumnMetaData.ALIGN_RIGHT;
		break;
	    }
	    result[i-1] = new ColumnMetaData(columnLabel,alignment);
	}
	return result;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
