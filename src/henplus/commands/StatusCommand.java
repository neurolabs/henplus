/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.AbstractCommand;

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
	TimeRenderer.printTime(session.getUptime(), System.err);
	System.err.print("; statements: " + session.getStatementCount());
	System.err.println();
	return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "show status of this connection";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
