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
public class StatusCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "status"
	};
    }
    
    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	System.err.println("URL:    " + session.getURL());
	System.err.print  ("uptime: ");
	printTime(session.getUptime());
	System.err.println();
	return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "show status of this connection";
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
