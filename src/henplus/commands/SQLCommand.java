/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.AbstractCommand;

import java.text.DecimalFormat;

import java.util.Map;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Types;

/**
 * document me.
 */
public class SQLCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    // these commands will be expanded ..
	    "select", "insert", "update",
	    "create", "alter", "drop",
	    "commit", "rollback",
	    // we support _any_ string, that is not part of the
	    // henplus buildin-stuff.
	    ""
	};
    }

    /**
     * don't show the number of commands available in the toplevel
     * command list ..
     */
    public boolean participateInCommandCompletion() { return false; }

    /**
     * complicated SQL statements are only complete with
     * semicolon. Simple commands may have no semicolon (like
     * 'commit' and 'rollback').
     */
    public boolean isComplete(String command) {
	command = command.toUpperCase(); // fixme: expensive.
	if (command.startsWith("COMMIT")
	    || command.startsWith("ROLLBACK"))
	    return true;
	// this will be wrong if and when we support stored procedures.
	return (command.endsWith(";"));
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	Statement stmt = null;
	ResultSet rset = null;
	try {
	    long startTime = System.currentTimeMillis();
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
		    ResultSetRenderer renderer;
		    renderer = new ResultSetRenderer(stmt.getResultSet(),
						     System.out);
		    String rows = renderer.execute();
		    System.err.print(rows + " row" + 
				     (("1".equals(rows))?"":"s")
				     + " in result");
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
	    //e.printStackTrace();
	    return EXEC_FAILED;
	}
	finally {
	    try { if (rset != null) rset.close(); } catch (Exception e) {}
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {}
	}
    }

    public String getLongDescription(String cmd) {
	String dsc;
	dsc="\tThis is a possible SQL-command. But I don't know anything\n"+
	    "\tabout it.";
	if ("select".equals(cmd)) {
	    dsc="\tselect from tables.";
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
