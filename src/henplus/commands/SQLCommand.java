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

import java.text.DecimalFormat;

import java.util.Map;
import java.util.Iterator;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Types;

/**
 * document me.
 */
public class SQLCommand extends AbstractCommand {
    private static final boolean verbose = false; // debug.
    private final static String[] COMPLETER_KEYWORD = {
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
     * if there is a semicolon at the end (like triggers and stored
     * procedures). We support the SQL*PLUS syntax in that we consider these
     * kind of statements complete with a single slash ('/') at the
     * beginning of a line.
     */
    public boolean isComplete(String command) {
	command = command.toUpperCase(); // fixme: expensive.
	if (command.startsWith("COMMIT")
	    || command.startsWith("ROLLBACK"))
	    return true;
	// FIXME: this is a very dumb parser. Leave out string literals.
	boolean anyProcedure = ((command.indexOf("PROCEDURE") >= 0)
				|| (command.indexOf("TRIGGER") >= 0));
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
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	Statement stmt = null;
	ResultSet rset = null;
	if (command.endsWith("/")) {
	    command = command.substring(0, command.length()-1);
	}
	try {
	    long startTime = System.currentTimeMillis();
	    long lapTime  = -1;
	    long execTime = -1;
	    if (command.startsWith("commit")) {
		System.err.print("commit..");
		session.getConnection().commit();
		System.err.println(".done.");
	    }
	    else if (command.startsWith("rollback")) {
		System.err.print("rollback..");
		session.getConnection().rollback();
		System.err.println(".done.");
	    }
	    else {
		boolean hasResultSet = false;
		
		/* this is basically a hack around SAP-DB's tendency to
		 * throw NullPointerExceptions, if the session timed out.
		 */
		for (int retry=2; retry > 0; --retry) {
		    try {
			stmt = session.createStatement();
			hasResultSet = stmt.execute(command);
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
		    int rows = renderer.execute();
		    if (renderer.limitReached()) {
			System.err.println("limit reached ..");
			System.err.print("> ");
		    }
		    System.err.print(rows + " row" + 
				     ((rows == 1)? "" : "s")
				     + " in result");
		    lapTime = renderer.getFirstRowTime() - startTime;
		}
		else {
		    int updateCount = stmt.getUpdateCount();
		    if (updateCount >= 0) {
			System.err.print("affected "+updateCount+" rows");
		    }
		    else {
			System.err.print("ok.");
		    }
		}
		execTime = System.currentTimeMillis() - startTime;
		System.err.print(" (");
		if (lapTime > 0) {
		    System.err.print("first row: ");
		    TimeRenderer.printTime(lapTime, System.err);
		    System.err.print("; total: ");
		}
		TimeRenderer.printTime(execTime, System.err);
		System.err.println(")");
	    }
	    return SUCCESS;
	}
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
    // table name. that is: if some keyword has been found before, go to
    // table-completer-mode :-)
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, final String lastWord) 
    {
	partialCommand = partialCommand.toUpperCase();
	for (int i=0; i < COMPLETER_KEYWORD.length; ++i) {
	    if (partialCommand.indexOf( COMPLETER_KEYWORD[i] ) >= 0) {
		return tableCompleter.completeTableName(lastWord);
	    }
	}
	return null;
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
	return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
