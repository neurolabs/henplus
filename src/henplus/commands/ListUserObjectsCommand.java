/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.util.Map;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.ResultSet;

import henplus.HenPlus;
import henplus.SQLSession;
import henplus.AbstractCommand;

/**
 * document me.
 */
public class ListUserObjectsCommand extends AbstractCommand {
    final private static String[] LIST_TABLES = { "TABLE" };
    final private static String[] LIST_VIEWS  = { "VIEW" };
    final private static int[]    DISP_COLS   = { 2, 3, 5 };

    /**
     * all tables in one session.
     */
    final private Map/*<SQLSession,SortedMap>*/  sessionTables;
    final private HenPlus                        henplus;

    public ListUserObjectsCommand(HenPlus hp) {
	sessionTables = new HashMap();
	henplus = hp;
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "tables", "views", "rehash"
	};
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd) {
	if (cmd.equals("rehash")) {
	    rehash(session);
	}
	else {
	    try {
		Connection conn = session.getConnection();  // use createStmt
		DatabaseMetaData meta = conn.getMetaData();
		String catalog = conn.getCatalog();
		/*
		System.err.println("catalog: " + catalog);
		ResultSetRenderer renderer = 
		    new ResultSetRenderer(meta.getCatalogs(), System.out);
		renderer.execute();
		*/
		ResultSetRenderer renderer;
		boolean showViews = "views".equals(cmd);
		String objectType = ((showViews) ? "Views" : "Tables");
		System.err.println(objectType);
		ResultSet rset = meta.getTables(catalog,
						null, null,
						(showViews)
						? LIST_VIEWS
						: LIST_TABLES);
		renderer = new ResultSetRenderer(rset, System.out,
						 DISP_COLS);
		int tables = renderer.execute();
		if (tables > 0) {
		    System.err.println(tables + " " + objectType + " found.");
		    if (renderer.limitReached()) {
			System.err.println("..and probably more; reached display limit");
		    }
		}
	    }
	    catch (Exception e) {
		System.err.println(e.getMessage());
		return EXEC_FAILED;
	    }
	}
	return SUCCESS;
    }

    private SortedSet getTableSet(SQLSession session) {
	SortedSet set = (SortedSet) sessionTables.get(session);
	return (set == null) ? rehash(session) : set;
    }

    private SortedSet rehash(SQLSession session) {
	SortedSet result = new TreeSet();
	Connection conn = session.getConnection();  // use createStmt
	ResultSet rset = null;
	try {
	    DatabaseMetaData meta = conn.getMetaData();
	    rset = meta.getTables(null, null, null, LIST_TABLES);
	    while (rset.next()) {
		result.add(rset.getString(3));
	    }
	}
	catch (Exception e) {
	    // ignore.
	}
	finally {
	    if (rset != null) {
		try { rset.close(); } catch (Exception e) {}
	    }
	}
	sessionTables.put(session, result);
	return result;
    }

    /**
     * used from diverse commands that need table name completion.
     */
    public Iterator completeTableName(String partialTable) {
	SQLSession session = henplus.getSession();
	if (session == null) return null;
	final SortedSet tableSet = getTableSet(session);
	Iterator it0 = tableSet.tailSet(partialTable).iterator();
	if (!it0.hasNext()) {
	    // test uppercase, then:
	    partialTable = partialTable.toUpperCase();
	    //System.err.println("test upper case: " + partialTable);
	    it0 = tableSet.tailSet(partialTable).iterator();
	}
	final Iterator tableIterator = it0;
	final String   tablePattern  = partialTable;

	return new Iterator() {
		String current = null;
		public boolean hasNext() {
		    if (tableIterator.hasNext()) {
			current = (String) tableIterator.next();
			if (current.startsWith(tablePattern))
			    return true;
		    }
		    return false;
		}
		public Object  next() { 
		    return current;
		}
		public void remove() { 
		    throw new UnsupportedOperationException("no!");
		}
	    };
    }

    /**
     * return a descriptive string.
     */
    public String  getShortDescription() { 
	return "list available user objects";
    }
    
    public String getSynopsis(String cmd) {
	return cmd;
    }

    public String getLongDescription(String cmd) {
	String dsc;
	if (cmd.equals("rehash")) {
	    dsc="\trebuild the internal hash for tablename completion.";
	}
	else {
	    dsc="\tLists all " + cmd + " available in this schema.";
	}
	return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
