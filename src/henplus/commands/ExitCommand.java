/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.HenPlus;
import henplus.SQLSession;
import henplus.AbstractCommand;

/**
 * document me.
 */
public class ExitCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "exit", "quit"
	};
    }
    
    public boolean requiresValidSession(String cmd) { return false; }
    
    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	HenPlus.getInstance().terminate();
	return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    public String  getShortDescription() {
	return "exits HenPlus";
    }

    public String getSynopsis(String cmd) {
	return cmd;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
