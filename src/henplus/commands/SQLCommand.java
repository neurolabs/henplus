/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.SigIntHandler;
import henplus.util.NameCompleter;

import java.text.DecimalFormat;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;

/**
 * document me.
 */
public class SQLCommand extends AbstractCommand {
    private static final boolean verbose = false; // debug.
    private final static String[] TABLE_COMPLETER_KEYWORD = {
	"FROM", "INTO", "UPDATE", "TABLE", /*create index*/"ON"
    };
    
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    // provide tab-completion at least for these command starts..
	    "select", 
	    "insert", "update", "delete",
	    "create", "alter",  "drop",
	    "commit", "rollback",
            "call-procedure",
	    // we support _any_ string, that is not part of the
	    // henplus buildin-stuff; the following empty string flags this.
	    ""
	};
    }

    private final ListUserObjectsCommand tableCompleter;
    
    public SQLCommand(ListUserObjectsCommand tc) {
	tableCompleter = tc;
    }

    /**
     * don't show the commands available in the toplevel
     * command completion list ..
     */
    public boolean participateInCommandCompletion() { return false; }

    /**
     * complicated SQL statements are only complete with
     * semicolon. Simple commands may have no semicolon (like
     * 'commit' and 'rollback'). Yet others are not complete even
     * if we ecounter a semicolon (like triggers and stored
     * procedures). We support the SQL*PLUS syntax in that we consider these
     * kind of statements complete with a single slash ('/') at the
     * beginning of a line.
     */
    public boolean isComplete(String command) {
	command = command.toUpperCase(); // fixme: expensive.
	if (command.startsWith("COMMIT")
	    || command.startsWith("ROLLBACK"))
	    return true;
	// FIXME: this is a very dumb parser. 
        // i.e. string literals are not considered.
	boolean anyProcedure = ((command.startsWith("CREATE")
				 || command.startsWith("REPLACE"))
				&&
				((containsWord(command, "PROCEDURE")
				 || (containsWord(command, "FUNCTION"))
				 || (containsWord(command, "PACKAGE"))
                                  || (containsWord(command, "TRIGGER")))));
	if (!anyProcedure && command.endsWith(";")) return true;
	// sqlplus is complete on a single '/' on a line.
	if (command.length() >= 3) {
	    int lastPos = command.length()-1;
	    if (command.charAt(lastPos) == '\n'
		&& command.charAt(lastPos-1) == '/'
		&& command.charAt(lastPos-2) == '\n')
		return true;
	}
	return false;
    }

    /**
     * looks, if this word is contained in 'all', preceeded and followed by
     * a whitespace.
     */
    private boolean containsWord(String all, String word) {
        int wordLen = word.length();
        int index = all.indexOf( word );
        return (index >= 0
                && (index == 0 || Character.isWhitespace(all.charAt(index-1)))
                && (Character.isWhitespace(all.charAt(index+wordLen))));
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	Statement stmt = null;
	ResultSet rset = null;
	String command = cmd + " " + param;
        boolean background = false;

	if (command.endsWith("/")) {
	    command = command.substring(0, command.length()-1);
	}

        if (command.endsWith("&")) {
            command = command.substring(0, command.length()-1);
            System.err.println("## executing command in the background not yet supported");
            background = true;
        }

	long startTime = System.currentTimeMillis();
	long lapTime  = -1;
	long execTime = -1;
	try {
// 	    SigIntHandler.getInstance()
// 		.registerInterrupt(Thread.currentThread());
	    if (command.startsWith("commit")) {
		session.print("commit..");
		session.getConnection().commit();
		session.println(".done.");
	    }
	    else if (command.startsWith("rollback")) {
		session.print("rollback..");
		session.getConnection().rollback();
		session.println(".done.");
	    }
	    else {
		boolean hasResultSet = false;
		boolean hasSingleResult = false;

		/* this is basically a hack around SAP-DB's tendency to
		 * throw NullPointerExceptions, if the session timed out.
		 */
		for (int retry=2; retry > 0; --retry) {
		    try {
                        if ("call-procedure".equals(cmd)) {
                            CallableStatement pstmt;
                            command = "{? = call " + param + "}";
                            pstmt = session.getConnection()
                                .prepareCall(command);
                            //pstmt.registerOutParameter(1, Types.REF);
                            pstmt.registerOutParameter(1, Types.VARCHAR);
                            pstmt.execute();
                            //System.err.println("call this .." + command);
                            //pstmt.setR
                            hasResultSet = false;
                            hasSingleResult = true;
                            stmt = pstmt;
                        }
                        else {
                            stmt = session.createStatement();
                            hasResultSet = stmt.execute(command);
                        }
			break;
		    }
		    catch (SQLException e) { throw e; }
		    catch (Throwable e) {
			if (retry == 1) {
			    return EXEC_FAILED;
			}
			System.err.println("Problem: " + e.getMessage()
					   + "; trying reconnect...");
			session.connect();
		    }
		}
		
		if (hasResultSet) {
		    rset = stmt.getResultSet();
		    ResultSetRenderer renderer;
		    renderer = new ResultSetRenderer(rset, System.out);
		    SigIntHandler.getInstance().pushInterruptable(renderer);
		    int rows = renderer.execute();
		    if (renderer.limitReached()) {
			session.println("limit reached ..");
			session.print("> ");
		    }
		    session.print(rows + " row" + 
				     ((rows == 1)? "" : "s")
				     + " in result");
		    lapTime = renderer.getFirstRowTime() - startTime;
		}
                else if (hasSingleResult) {
                    System.err.println(((CallableStatement)stmt).getString(1));
                }
		else {
		    int updateCount = stmt.getUpdateCount();
		    if (updateCount >= 0) {
			session.print("affected "+updateCount+" rows");
		    }
		    else {
			session.print("ok.");
		    }
		}
		execTime = System.currentTimeMillis() - startTime;
		session.print(" (");
		if (lapTime > 0) {
		    session.print("first row: ");
		    if (session.printMessages()) {
			TimeRenderer.printTime(lapTime, System.err);
		    }
		    session.print("; total: ");
		}
		if (session.printMessages()) {
		    TimeRenderer.printTime(execTime, System.err);
		}
		session.println(")");
	    }

	    // be smart and retrigger hashing of the tablenames.
	    if ("drop".equals(cmd) || "create".equals(cmd)) {
		tableCompleter.unhash(session);
	    }

	    return SUCCESS;
	}
	/*
	catch (InterruptedException ie) {
	    session.print("interrupted after ");
	    execTime = System.currentTimeMillis() - startTime;
	    TimeRenderer.printTime(execTime, System.err);
	    session.println(".");
	    return SUCCESS;
	}
	*/
	catch (Exception e) {
	    String msg = e.getMessage();
	    if (msg != null) {
		// oracle appends a newline to the message for some reason.
		System.err.println("FAILURE: " + msg.trim());
	    }
	    if (verbose) e.printStackTrace();
	    return EXEC_FAILED;
	}
	finally {
	    try { if (rset != null) rset.close(); } catch (Exception e) {}
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {}
	}
    }

    // very simple completer: try to determine wether we can complete a
    // table name. that is: if some keyword has been found before, switch to
    // table-completer-mode :-)
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, final String lastWord) 
    {
	final String canonCmd = partialCommand.toUpperCase();
        /*
         * look for keywords that expect table names
         */
	int tableMatch = -1;
	for (int i=0; i < TABLE_COMPLETER_KEYWORD.length; ++i) {
	    int match = canonCmd.indexOf( TABLE_COMPLETER_KEYWORD[i] );
	    if (match >= 0) {
		tableMatch = match + TABLE_COMPLETER_KEYWORD[i].length();
		break;
	    }
	}

	if (tableMatch < 0) {
            /*
             * ok, try to complete all columns from all tables since
             * we don't know yet what table the column will be from.
             */
            return tableCompleter.completeAllColumns(lastWord);
        }
        
	int endTabMatch = -1;  // where the table declaration ends.
	if (canonCmd.indexOf("UPDATE") >= 0) {
	    endTabMatch = canonCmd.indexOf ("SET");
	}
	else if (canonCmd.indexOf("INSERT") >= 0) {
	    endTabMatch = canonCmd.indexOf ("(");
	}
	else {
	    endTabMatch = canonCmd.indexOf ("WHERE");
	    if (endTabMatch < 0) {
		endTabMatch = canonCmd.indexOf (";");
	    }
	}
	if (endTabMatch > tableMatch) {
            /*
             * column completion for the tables mentioned between in the
             * table area. This acknowledges as well aliases and prepends
             * the names with these aliases, if necessary.
             */
	    String tables = partialCommand.substring(tableMatch, endTabMatch);
	    HashMap tmp = new HashMap();
	    Iterator it = tableDeclParser(tables).entrySet().iterator();
	    while (it.hasNext()) {
		Map.Entry entry = (Map.Entry) it.next();
		String alias   = (String) entry.getKey();
		String tabName = (String) entry.getValue();
		tabName = tableCompleter.correctTableName(tabName);
		if (tabName == null)
		    continue;
		Collection columns = tableCompleter.columnsFor(tabName);
		Iterator cit = columns.iterator();
		while (cit.hasNext()) {
		    String col = (String) cit.next();
		    Set aliases = (Set) tmp.get(col);
		    if (aliases == null) aliases = new HashSet();
		    aliases.add(alias);
		    tmp.put(col, aliases);
		}
	    }
	    NameCompleter completer = new NameCompleter();
	    it = tmp.entrySet().iterator();
	    while (it.hasNext()) {
		Map.Entry entry = (Map.Entry) it.next();
		String col = (String) entry.getKey();
		Set aliases = (Set) entry.getValue();
		if (aliases.size() == 1) {
		    completer.addName(col);
		}
		else {
		    Iterator ait = aliases.iterator();
		    while (ait.hasNext()) {
			completer.addName(ait.next() + "." + col);
		    }
		}
	    }
	    return completer.getAlternatives(lastWord);
	}
	else { // table completion.
	    return tableCompleter.completeTableName(lastWord);
	}
    }
    
    /**
     * parses 'tablename ((AS)? alias)? [,...]' and returns a map, that maps
     * the names (or aliases) to the tablenames.
     */
    private Map tableDeclParser(String tableDecl) {
	StringTokenizer tokenizer = new StringTokenizer(tableDecl,
							" \t\n\r\f,",
							true);
	Map result = new HashMap();
	String tok;
	String table = null;
	String alias = null;
	int state = 0;
	while (tokenizer.hasMoreElements()) {
	    tok = tokenizer.nextToken();
	    if (tok.length() == 1 && Character.isWhitespace(tok.charAt(0)))
		continue;
	    switch (state) {
	    case 0: { // initial/endstate
		table = tok;
		alias = tok;
		state = 1;
		break;
	    }
	    case 1: { // table seen, waiting for potential alias.
		if ("AS".equals(tok.toUpperCase()))
		    state = 2;
		else if (",".equals(tok)) {
		    state = 0; // we are done.
		}
		else {
		    alias = tok;
		    state = 3;
		}
		break;
	    }
	    case 2: { // 'AS' seen, waiting definitly for alias.
		if (",".equals(tok)) {
		    // error: alias missing for $table.
		    state = 0;
		}
		else {
		    alias = tok;
		    state = 3;
		}
		break;
	    }
	    case 3: {  // waiting for ',' at end of 'table (as)? alias'
		if (!",".equals(tok)) {
		    // error: ',' expected.
		}
		state = 0;
		break;
	    }
	    }

	    if (state == 0) {
		result.put(alias, table);
	    }
	}
	// store any unfinished state..
	if (state == 1 || state == 3) {
	    result.put(alias, table);
	}
	else if (state == 2) {
	    // error: alias expected for $table.
	}
	return result;
    }
    
    public String getSynopsis(String cmd) { 
	cmd = cmd.toLowerCase();
	String syn = null;
	if ("select".equals(cmd)) {
	    syn="select <columns> from <table[s]> [ where <where-clause>]";
	}
	else if ("insert".equals(cmd)) {
	    syn="insert into <table> [(<columns>])] values (<values>)";
	}
	else if ("delete".equals(cmd)) {
	    syn="delete from <table> [ where <where-clause>]";
	}
	else if ("update".equals(cmd)) {
	    syn="update <table> set <column>=<value>[,...] [ where <where-clause> ]";
	}
	else if ("drop".equals(cmd)) {
	    syn="drop <table|index|view|...>";
	}
	else if ("commit".equals(cmd))   { syn = cmd; }
	else if ("rollback".equals(cmd)) { syn = cmd; }
	return syn;
    }

    public String getLongDescription(String cmd) {
	String dsc;
	dsc="\t'" + cmd + "': this is not a build-in command, so would be\n"
	    + "\tconsidered as SQL-command and handed over to the JDBC-driver.\n"
	    + "\tHowever, I don't know anything about its syntax. RTFSQLM.\n"
	    + "\ttry <http://www.google.de/search?q=sql+syntax+" + cmd + ">";
	cmd = cmd.toLowerCase();
	if ("select".equals(cmd)) {
	    dsc="\tselect from tables.";
	}
	else if ("delete".equals(cmd)) {
	    dsc="\tdelete data from tables. DML.";
	}
	else if ("insert".equals(cmd)) {
	    dsc="\tinsert data into tables. DML.";
	}
	else if ("update".equals(cmd)) {
	    dsc="\tupdate existing rows with new data. DML.";
	}
	else if ("create".equals(cmd)) {
	    dsc="\tcreate new database object (such as tables/views/indices..). DDL.";
	}
	else if ("alter".equals(cmd)) {
	    dsc="\talter a database object. DDL.";
	}
	else if ("drop".equals(cmd)) {
	    dsc="\tdrop (remove) a database object. DDL.";
	}
	else if ("rollback".equals(cmd)) {
	    dsc="\trollback transaction.";
	}
	else if ("commit".equals(cmd)) {
	    dsc="\tcommit transaction.";
	}
        else if ("call-procedure".equals(cmd)) {
            dsc="\tcall a function that returns exactly one parameter\n"
                +"\tthat can be gathered as string (EXPERIMENTAL)\n"
                +"\texample:\n"
                +"\t  call-procedure foobar(42);\n";
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
