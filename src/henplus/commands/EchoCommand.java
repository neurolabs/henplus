/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: EchoCommand.java,v 1.1 2002-01-26 18:28:29 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import SQLSession;
import AbstractCommand;

/**
 * document me.
 */
public final class EchoCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "echo"
	};
    }
    
    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * execute the command given.
     */
    public int execute(SQLSession currentSession, String command) {
	command = command.trim();
	System.out.println(command.substring("echo ".length()));
	return SUCCESS;
    }
    
    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "echo argument";
    }

    public String getSynopsis(String cmd) {
	return "echo <whatever>";
    }

    public String getLongDescription(String cmd) { 
	String dsc;
	dsc= "\tjust echo the string given.";
	return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
