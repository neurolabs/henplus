/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import SQLSession;
import AbstractCommand;

import java.text.DecimalFormat;

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
	    "select", "insert", "update",
	    "create", "alter", "drop",
	    "commit", "rollback",
	    "" // any.
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
	if (command.startsWith("commit")
	    || command.startsWith("rollback"))
	    return true;
	// this will be wrong if we support stored procedures.
	return (command.trim().endsWith(";"));
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	Statement stmt = null;
	ResultSet rset = null;
	try {
	    if (command.startsWith("commit")) {
		session.getConnection().commit();
	    }
	    else if (command.startsWith("rollback")) {
		session.getConnection().rollback();
	    }
	    else {
		long execTime = System.currentTimeMillis();
		stmt = session.getConnection().createStatement();
		if (stmt.execute(command)) {
		    ResultSetRenderer renderer;
		    renderer = new ResultSetRenderer(stmt.getResultSet());
		    int rows = renderer.writeTo(System.out);		    
		    execTime = System.currentTimeMillis() - execTime;
		    System.err.print(rows + " row" + ((rows!=1)?"s":"")
				     + " in result (");
		    printTime(execTime);
		    System.err.println(")");
		}
		else {
		    int updateCount = stmt.getUpdateCount();
		    if (updateCount >= 0) {
			System.err.println("affected " + updateCount);
		    }
		    else {
			System.err.println("ok.");
		    }
		}
	    }
	    return SUCCESS;
	}
	catch (SQLException e) {
	    System.err.println(e.getMessage());
	    return EXEC_FAILED;
	}
	finally {
	    try { if (rset != null) rset.close(); } catch (Exception e) {}
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {}
	}
    }

    
    private void printTime(long execTime) {
	if (execTime > 60000) {
	    System.err.print(execTime/60000);
	    System.err.print(":");
	    execTime %= 60000;
	    if (execTime < 10000)
		System.err.print("0");
	}
	if (execTime >= 1000) {
	    System.err.print(execTime / 1000);
	    System.err.print(".");
	    execTime %= 1000;
	    if (execTime < 100) System.err.print("0");
	    if (execTime < 10)  System.err.print("0");
	    System.err.print(execTime);
	    System.err.print(" ");
	}
	else {
	    System.err.print(execTime + " m");
	}
	System.err.print("sec");
    }
    }

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
