/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SpoolCommand.java,v 1.1 2002-04-22 16:16:54 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.AbstractCommand;

/**
 * dummy command. In many sqlplus scripts, there is this spool
 * command used. This is scribble down the commands issued and the
 * response. I am too lazy now to implement this, and don't need
 * it either. But feel free to implement it.
 */
public final class SpoolCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "spool"
	};
    }
    
    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * execute the command given.
     */
    public int execute(SQLSession currentSession, String cmd, String param) {
	System.err.println("[ignoring spool command.]");
	return SUCCESS;
    }
    
    public String getLongDescription(String cmd) { 
	String dsc;
	dsc= "\tThis command does nothing (yet). For now, it is only\n"
	    +"\tthere to work with Oracle SQLplus scripts. But you are\n"
	    +"\tfree to implement it; be part of the henplus team:\n"
	    +"\thttp://www.sourceforge.net/projects/henplus";
	return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
