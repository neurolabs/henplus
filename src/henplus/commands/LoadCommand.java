/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import HenPlus;
import SQLSession;
import AbstractCommand;

import java.util.StringTokenizer;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * document me.
 */
public class LoadCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "load"
	};
    }
    
    // complete: TODO: file name completion.

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	StringTokenizer st = new StringTokenizer(command);
	st.nextElement(); // remove load.
	int argc = st.countTokens();
	if (argc != 1) {
	    return SYNTAX_ERROR;
	}

	HenPlus henplus = HenPlus.getInstance();
	try {
	    File f = new File((String) st.nextElement());
	    BufferedReader reader = new BufferedReader(new FileReader(f));
	    String line;
	    while ((line = reader.readLine()) != null) {
		henplus.addLine(line);
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	    return EXEC_FAILED;
	}
	finally {
	    henplus.resetBuffer(); // no open state ..
	}
	return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "load file and execute commands";
    }

    public String getSynopsis(String cmd) {
	return "load <filename>";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
