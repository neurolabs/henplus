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
public class ListUserObjectsCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "tables", "views", "synonyms", "indices"
	};
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	System.out.println("not yet.");
	return SUCCESS;
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
	dsc="\tLists all " + cmd + " available in this schema.";
	return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
