/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import SQLSession;
import AbstractCommand;

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
	// this will be wrong if we support
	return (command.endsWith(";"));
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	System.out.println("SQL-command: not yet.");
	return SUCCESS;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
