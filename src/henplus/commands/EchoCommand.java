/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: EchoCommand.java,v 1.6 2002-02-26 21:15:19 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.AbstractCommand;

/**
 * document me.
 */
public final class EchoCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "echo", "prompt"
	};
    }
    
    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * execute the command given.
     */
    public int execute(SQLSession currentSession, String cmd, String param) {
	String outStr = param.trim();
	System.out.println(stripQuotes(outStr));
	return SUCCESS;
    }
    
    private String stripQuotes(String value) {
	if (value.startsWith("\"") && value.endsWith("\"")) {
	    value = value.substring(1, value.length()-1);
	}
	else if (value.startsWith("\'") && value.endsWith("\'")) {
	    value = value.substring(1, value.length()-1);
	}
	return value;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "echo argument";
    }

    public String getSynopsis(String cmd) {
	return cmd + " <whatever>";
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
