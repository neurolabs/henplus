/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import henplus.util.*;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;

/**
 * document me.
 */
public class DescribeCommand extends AbstractCommand {
    static final boolean verbose     = true;
    private final static ColumnMetaData[] DESC_META;
    static {
	DESC_META = new ColumnMetaData[7];
	DESC_META[0] = new ColumnMetaData("table");
	DESC_META[1] = new ColumnMetaData("column");
	DESC_META[2] = new ColumnMetaData("type");
	DESC_META[3] = new ColumnMetaData("null");
	DESC_META[4] = new ColumnMetaData("default");
	DESC_META[5] = new ColumnMetaData("pk");
	DESC_META[6] = new ColumnMetaData("fk");
    }
    
    private final ListUserObjectsCommand tableCompleter;

    public DescribeCommand(ListUserObjectsCommand tc) {
	tableCompleter = tc;
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] { "describe" }; 
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	final StringTokenizer st = new StringTokenizer(command);
	final String cmd = (String) st.nextElement();
	final int argc = st.countTokens();
	if (argc != 1) {
	    return SYNTAX_ERROR;
	}
	final String tabName = (String) st.nextElement();
	ResultSet rset = null;
	try {
	    DatabaseMetaData meta = session.getConnection().getMetaData();
	    for (int i=0; i < DESC_META.length; ++i) {
		DESC_META[i].reset();
	    }
	    /*
	     * get primary keys.
	     */
	    Map pks = new HashMap();
	    rset = meta.getPrimaryKeys(null, null, tabName);
	    while (rset.next()) {
		String col = rset.getString(4);
		String pkseq  = rset.getString(5);
		String pkname = rset.getString(6);
		String desc = (pkname != null) ? pkname : "*";
		if (pkseq != null) {
		    desc += "{" + pkseq + "}";
		}
		pks.put(col, desc);
	    }
	    rset.close();
	    /*
	     * get foreign keys.
	     */
	    Map fks = new HashMap();
	    rset = meta.getImportedKeys(null, null, tabName);
	    while (rset.next()) {
		String table = rset.getString(3);
		String pkcolumn  = rset.getString(4);
		table = table + "(" + pkcolumn + ")";
		String col = rset.getString(8);
		String fkname = rset.getString(12);
		String desc = (fkname != null) ? fkname +"\n-> " : "-> ";
		desc += table;
		fks.put(col, desc);
	    }
	    rset.close();
	    rset = meta.getColumns(null, null, tabName, null);
	    /*
	     * if all columns belong to the same table name, then don't
	     * report it. A different table name may only occur in rare
	     * circumstance like object oriented databases.
	     */
	    boolean allSameTableName = true;
	    List rows = new ArrayList();
	    while (rset.next()) {
		Column[] row = new Column[7];
		String thisTabName = rset.getString(3);
		row[0] = new Column( thisTabName );
		allSameTableName &= tabName.equals(thisTabName);
		String colname = rset.getString(4);
		row[1] = new Column( colname );
		String type = rset.getString(6);
		int colSize = rset.getInt(7);
		if (colSize > 0) type = type + "(" + colSize + ")";
		row[2] = new Column( type );
		String defaultVal = rset.getString(13);
		row[3] = new Column( rset.getString(18) );
		row[4] = new Column( defaultVal );
		String pkdesc = (String) pks.get(colname);
		if (pkdesc != null && pks.size() == 1) {
		    int curlPos = pkdesc.indexOf('{');
		    if (curlPos >= 0) {
			pkdesc = pkdesc.substring(0, curlPos);
		    }
		}
		row[5] = new Column( (pkdesc != null) ? pkdesc : "");
		String fkdesc = (String) fks.get(colname);
		row[6] = new Column( (fkdesc != null) ? fkdesc : "");
		rows.add(row);
	    }
	    rset.close();
	    /*
	     * we render the table now, since we only know know, whether we
	     * will show the first column or not.
	     */
	    DESC_META[0].setDisplay(!allSameTableName);
	    TableRenderer table = new TableRenderer(DESC_META, System.out);
	    Iterator it = rows.iterator();
	    while (it.hasNext()) table.addRow((Column[]) it.next());
	    table.closeTable();
	    /*
	     * index info.
	     */
	    System.out.println("index information:");
	    boolean anyIndex = false;
	    rset = meta.getIndexInfo(null, null, tabName, false, false);
	    while (rset.next()) {
		anyIndex = true;
		System.out.print("\t");
		boolean nonUnique;
		String idxName = null;
		nonUnique = rset.getBoolean(4);
		idxName = rset.getString(6);
		if (idxName == null) continue; // statistics, otherwise.
		if (!nonUnique) System.out.print("unique ");
		System.out.print("index " + idxName);
		String colName = rset.getString(9);
		// work around postgres-JDBC-driver bug:
		if (colName != null && colName.length() > 0) {
		    System.out.print(" on " + colName);
		}
		System.out.println();
	    }
	    rset.close();
	    if (!anyIndex) {
		System.out.println("\t<none>");
	    }
	}
	catch (Exception e) {
	    if (verbose) e.printStackTrace();
	    return EXEC_FAILED;
	}
	finally {
	    if (rset != null) {
		try { rset.close(); } catch (Exception e) {}
	    }
	}
	return SUCCESS;
    }
    
    /**
     * complete the table name.
     */
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, final String lastWord) 
    {
	StringTokenizer st = new StringTokenizer(partialCommand);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();
	// we accept only one argument.
	if (argc > ("".equals(lastWord) ? 0 : 1)) {
	    return null;
	}
	return tableCompleter.completeTableName(lastWord);
    }
    
    /**
     * return a descriptive string.
     */
    public String  getShortDescription() { 
	return "describe a database object";
    }
    
    public String getSynopsis(String cmd) {
	return "describe <table|view|index>";
    }

    public String getLongDescription(String cmd) {
	String dsc;
	dsc="\tDescribe the meta information of the named user object.";
	return dsc;
    }

}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
