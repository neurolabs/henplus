/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ResultSetRenderer.java,v 1.14 2003-05-01 18:26:29 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.util.*;
import henplus.Interruptable;

import java.sql.ResultSet;
import java.sql.Clob;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.io.PrintStream;
import java.io.Reader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * document me.
 */
public class ResultSetRenderer implements Interruptable {
    private static final int LIMIT = 2000;

    private final ResultSet rset;
    private final ResultSetMetaData meta;
    private final TableRenderer table;
    private final int columns;
    private final int[] showColumns;

    private boolean beyondLimit;
    private long    firstRowTime;
    private long    clobLimit = 8192;
    private volatile boolean running;

    public ResultSetRenderer(ResultSet rset, String columnDelimiter,
                             PrintStream out, int[] show) 
	throws SQLException {
	this.rset = rset;
	beyondLimit = false;
	firstRowTime = -1;
	showColumns = show;
	meta = rset.getMetaData();
	columns = (show != null) ? show.length : meta.getColumnCount();
	table = new TableRenderer(getDisplayMeta(meta),  out, columnDelimiter);
    }

    public ResultSetRenderer(ResultSet rset, String columnDelimiter,
                             PrintStream out) 
	throws SQLException {
	this(rset, columnDelimiter, out, null);
    }
    
    // Interruptable interface.
    public synchronized void interrupt() {
	running = false;
    }

    private String readClob(Clob c) throws SQLException {
        if (c == null) return null;
        StringBuffer result = new StringBuffer();
        long restLimit = clobLimit;
        try {
            Reader in = c.getCharacterStream();
            char buf[] = new char [ 4096 ];
            int r;

            while (restLimit > 0 
                   && (r = in.read(buf, 0, (int)Math.min(buf.length,restLimit))) > 0) 
                {
                    //System.out.println("read .. " + r);
                    result.append(buf, 0, r);
                    restLimit -= r;
                }
        }
        catch (Exception e) {
            System.err.println(e);
        }
        if (restLimit == 0) {
            result.append("...");
        }
        return result.toString();
    }

    public int execute() throws SQLException {
	int rows = 0;

	running = true;
	try {
	    while (running && rset.next()) {
		Column[] currentRow = new Column[ columns ];
		for (int i = 0 ; i < columns ; ++i) {
		    int col = (showColumns != null) ? showColumns[i] : i+1;
                    String colString;
                    if (meta.getColumnType( col ) == Types.CLOB) {
                        colString = readClob(rset.getClob( col ));
                    }
                    else {
                        colString = rset.getString( col );
                    }
		    Column thisCol = new Column(colString); 
		    currentRow[i] = thisCol;
		}
		if (firstRowTime < 0) {
                    // read first row completely.
		    firstRowTime = System.currentTimeMillis();
		}
		table.addRow(currentRow);
		++rows;
		if (rows >= LIMIT) {
		    beyondLimit = true;
		    break;
		}
	    }
	    
	    table.closeTable();
	}
	finally {
	    rset.close();
	}
	return rows;
    }
    
    public boolean limitReached() {
	return beyondLimit;
    }
    
    public long getFirstRowTime() {
	return firstRowTime;
    }

    /**
     * determine meta data necesary for display.
     */
    private ColumnMetaData[] getDisplayMeta(ResultSetMetaData m) 
	throws SQLException {
	ColumnMetaData result[] = new ColumnMetaData [ columns ];

	for (int i = 0; i < result.length; ++i) {
	    int col = (showColumns != null) ? showColumns[i] : i+1;
	    int alignment  = ColumnMetaData.ALIGN_LEFT;
	    String columnLabel = m.getColumnLabel( col );
	    /*
	    int width = Math.max(m.getColumnDisplaySize(i),
				 columnLabel.length());
	    */
	    switch (m.getColumnType( col )) {
	    case Types.NUMERIC:  
	    case Types.INTEGER: 
	    case Types.REAL:
	    case Types.SMALLINT:
	    case Types.TINYINT:
		alignment = ColumnMetaData.ALIGN_RIGHT;
		break;
	    }
	    result[i] = new ColumnMetaData(columnLabel,alignment);
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
