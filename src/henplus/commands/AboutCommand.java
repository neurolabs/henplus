/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: AboutCommand.java,v 1.5 2002-03-01 14:34:56 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.Version;

/**
 * document me.
 */
public class AboutCommand extends AbstractCommand {
    final static String LICENSE = 
	"GNU Public License <http://www.gnu.org/licenses/gpl.txt>";

    final static String ABOUT = 
"---------------------------------------------------------------------------\n"
+" HenPlus II " + Version.getVersion() + " Copyright(C) 1997, 2001 Henner Zeller <H.Zeller@acm.org>\n"
+" HenPlus is provided AS IS and comes with ABSOLUTELY NO WARRANTY\n"
+" This is free software, and you are welcome to redistribute it under the\n"
+" conditions of the " + LICENSE + "\n"
+"---------------------------------------------------------------------------\n";
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "about", "version", "license"
	};
    }

    public AboutCommand(boolean quiet) {
	if (!quiet) {
	    System.err.print( ABOUT );
	}
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	if ("about".equals(cmd)) {
	    System.err.print( ABOUT );
	}
	else if ("version".equals(cmd)) {
	    System.err.println(Version.getVersion() 
			       + " / compiled " + Version.getCompileTime());
	}
	else if ("license".equals(cmd)) {
	    System.err.println( LICENSE );
	}
	return SUCCESS;
    }

    public boolean requiresValidSession(String cmd) { 
	return false;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "about HenPlus";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
