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

import java.util.StringTokenizer;
import java.sql.SQLException;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * document me.
 */
public class AutocommitCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "autocommit-on", "autocommit-off"
	};
    }
    
    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	try {
	    if ("autocommit-on".equals(cmd)) {
		session.getConnection().setAutoCommit(true);
		System.err.println("set autocommit on");
	    }
	    
	    else if ("autocommit-off".equals(cmd)) {
		session.getConnection().setAutoCommit(false);
		System.err.println("set autocommit off");
	    }
	}
	catch (SQLException e) {
	    System.err.println(e.getMessage());
	}
	return SUCCESS;
    }
    
    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "switches autocommit on/off";
    }

}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
