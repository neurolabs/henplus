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
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import henplus.util.*;
import henplus.Interruptable;
import henplus.SigIntHandler;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;

/**
 * document me.
 */
public class DescribeCommand 
    extends AbstractCommand 
    implements Interruptable 
{

    static final boolean verbose     = false;
    private final static ColumnMetaData[] DESC_META;
    static {
	DESC_META = new ColumnMetaData[8];
        DESC_META[0] = new ColumnMetaData("#", ColumnMetaData.ALIGN_RIGHT);
	DESC_META[1] = new ColumnMetaData("table");
	DESC_META[2] = new ColumnMetaData("column");
	DESC_META[3] = new ColumnMetaData("type");
	DESC_META[4] = new ColumnMetaData("null");
	DESC_META[5] = new ColumnMetaData("default");
	DESC_META[6] = new ColumnMetaData("pk");
	DESC_META[7] = new ColumnMetaData("fk");
    }

    private boolean interrupted;
    private final ListUserObjectsCommand tableCompleter;

    public DescribeCommand(ListUserObjectsCommand tc) {
	tableCompleter = tc;
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] { "describe", "idescribe" }; 
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	final StringTokenizer st = new StringTokenizer(param);
	final int argc = st.countTokens();
	if (argc != 1) {
	    return SYNTAX_ERROR;
	}
	boolean correctName = true;
	String tabName = (String) st.nextElement();
	if (tabName.startsWith("\"")) {
	    tabName = stripQuotes(tabName);
	    correctName = false;
	}
	if (correctName) {
	    String alternative = tableCompleter.correctTableName(tabName);
	    if (alternative != null && !alternative.equals(tabName)) {
		tabName = alternative;
		System.out.println("describing table: '" + tabName 
				   + "' (corrected name)");
	    }
	}

	ResultSet rset = null;
        Set doubleCheck = new HashSet();
	try {
            interrupted = false;
            SigIntHandler.getInstance().pushInterruptable(this);
	    boolean anyLeftArrow  = false;
	    boolean anyRightArrow = false;
            long startTime = System.currentTimeMillis();
            String catalog = session.getConnection().getCatalog();

            if (interrupted) return SUCCESS;

	    DatabaseMetaData meta = session.getConnection().getMetaData();
	    for (int i=0; i < DESC_META.length; ++i) {
		DESC_META[i].resetWidth();
	    }

	    /*
	     * get primary keys.
	     */
            if (interrupted) return SUCCESS;
	    Map pks = new HashMap();
	    rset = meta.getPrimaryKeys(null, null, tabName);
	    if (rset != null) while (!interrupted && rset.next()) {
		String col = rset.getString(4);
		int pkseq  = rset.getInt(5);
		String pkname = rset.getString(6);
		String desc = (pkname != null) ? pkname : "*";
		if (pkseq > 1) {
		    desc += "{" + pkseq + "}";
		}
		pks.put(col, desc);
	    }
	    rset.close();

	    /*
	     * get referenced primary keys.
	     */
            if (interrupted) return SUCCESS;
	    rset = meta.getExportedKeys(null, null, tabName);
	    if (rset != null) while (!interrupted && rset.next()) {
		String col = rset.getString(4);
		String fktable = rset.getString(7);
		String fkcolumn  = rset.getString(8);
		fktable = fktable + "(" + fkcolumn + ")";
		String desc = (String) pks.get(col);
		desc = (desc == null) 
		    ? " <- "  + fktable
		    : desc + "\n <- " + fktable;
		anyLeftArrow = true;
		pks.put(col, desc);
	    }
	    rset.close();

	    /*
	     * get foreign keys.
	     */
            if (interrupted) return SUCCESS;
	    Map fks = new HashMap();
	    rset = meta.getImportedKeys(null, null, tabName);
	    if (rset != null) while (!interrupted && rset.next()) {
		String table = rset.getString(3);
		String pkcolumn  = rset.getString(4);
		table = table + "(" + pkcolumn + ")";
		String col = rset.getString(8);
		String fkname = rset.getString(12);
		String desc = (fkname != null) ? fkname +"\n -> " : " -> ";
		desc += table;
		anyRightArrow = true;
		fks.put(col, desc);
	    }
	    rset.close();

            if (catalog != null) System.err.println("catalog: " + catalog);
	    if (anyLeftArrow)  System.err.println(" '<-' : referenced by");
	    if (anyRightArrow) System.err.println(" '->' : referencing");

	    /*
	     * if all columns belong to the same table name, then don't
	     * report it. A different table name may only occur in rare
	     * circumstance like object oriented databases.
	     */
	    boolean allSameTableName = true;

	    /*
	     * build up actual describe table.
	     */
            if (interrupted) return SUCCESS;

	    rset = meta.getColumns(catalog, null, tabName, null);
	    List rows = new ArrayList();
            int colNum = 0;
	    if (rset != null) while (!interrupted && rset.next()) {
		Column[] row = new Column[8];
                row[0] = new Column( ++colNum );
		String thisTabName = rset.getString(3);
		row[1] = new Column( thisTabName );
		allSameTableName &= tabName.equals(thisTabName);
		String colname = rset.getString(4);
                if (doubleCheck.contains(colname)) {
                    continue;
                }
                doubleCheck.add(colname);
		row[2] = new Column( colname );
		String type = rset.getString(6);
		int colSize = rset.getInt(7);
		if (colSize > 0) type = type + "(" + colSize + ")";
		row[3] = new Column( type );
		String defaultVal = rset.getString(13);
		row[4] = new Column( rset.getString(18) );
		// oracle appends newline to default values for some reason.
		row[5] = new Column( ((defaultVal != null) 
				      ? defaultVal.trim() 
				      : null) );
		String pkdesc = (String) pks.get(colname);
		row[6] = new Column( (pkdesc != null) ? pkdesc : "");
		String fkdesc = (String) fks.get(colname);
		row[7] = new Column( (fkdesc != null) ? fkdesc : "");
		rows.add(row);
	    }
	    rset.close();

	    /*
	     * we render the table now, since we only know know, whether we
	     * will show the first column or not.
	     */
	    DESC_META[1].setDisplay(!allSameTableName);
	    TableRenderer table = new TableRenderer(DESC_META, System.out);
	    Iterator it = rows.iterator();
	    while (it.hasNext()) {
                table.addRow((Column[]) it.next());
            }
	    table.closeTable();

            if ("describe".equals(cmd)) {
                /*
                 * no index describe
                 */
                return SUCCESS;
            }

	    /*
	     * index info.
	     */
            if (interrupted) return SUCCESS;
	    System.out.println("index information:");
	    boolean anyIndex = false;
	    rset = meta.getIndexInfo(null, null, tabName, false, true);
	    if (rset != null) while (!interrupted && rset.next()) {
		boolean nonUnique;
		String idxName = null;
		nonUnique = rset.getBoolean(4);
		idxName = rset.getString(6);
		if (idxName == null) continue; // statistics, otherwise.
		// output part.
		anyIndex = true;
		System.out.print("\t");
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
            TimeRenderer.printTime(System.currentTimeMillis() - startTime,
                                   System.out);
            System.out.println();
	}
	catch (Exception e) {
	    if (verbose) e.printStackTrace();
            System.err.println("Database problem reading meta data: "+
                               e.getMessage().trim());
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
			     String partialCommand, String lastWord) 
    {
	StringTokenizer st = new StringTokenizer(partialCommand);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();
	// we accept only one argument.
	if (argc > ("".equals(lastWord) ? 0 : 1)) {
	    return null;
	}
	if (lastWord.startsWith("\"")) {
	    lastWord = lastWord.substring(1);
	}
	return tableCompleter.completeTableName(lastWord);
    }

    private String stripQuotes(String value) {
	if (value.startsWith("\"") && value.endsWith("\"")) {
	    value = value.substring(1, value.length()-1);
	}
	return value;
    }

    //-- Interruptable interface
    public synchronized void interrupt() {
        interrupted = true;
    }
    
    /**
     * return a descriptive string.
     */
    public String  getShortDescription() { 
	return "describe a database object";
    }
    
    public String getSynopsis(String cmd) {
	return cmd + " <tablename>";
    }

    public String getLongDescription(String cmd) {
	String dsc;
	dsc="\tDescribe the meta information of the named user object\n"
	    +"\t(only tables for now). The name you type is case sensitive\n"
	    +"\tbut henplus tries its best to correct it.\n"
            +"\tThe 'describe' command just describes the table, the\n"
            +"\t'idescribe' command determines the index information as\n"
            +"\twell; some databases are really slow in this, so this is\n"
            +"\tan extra command";
	return dsc;
    }

}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
