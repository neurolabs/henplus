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
	    "load", "start"
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
	long startTime = System.currentTimeMillis();
	int  commandCount = 0;

	HenPlus henplus = HenPlus.getInstance();
	try {
	    File f = new File((String) st.nextElement());
	    BufferedReader reader = new BufferedReader(new FileReader(f));
	    String line;
	    while ((line = reader.readLine()) != null) {
		boolean isComplete = henplus.addLine(line);
		if (isComplete) {
		    ++commandCount;
		}
	    }
	}
	catch (Exception e) {
	    System.err.println(e.getMessage());
	    return EXEC_FAILED;
	}
	finally {
	    henplus.resetBuffer(); // no open state ..
	}
	long execTime = System.currentTimeMillis() - startTime;
	System.err.print(commandCount + " commands in ");
	TimeRenderer.printTime(execTime, System.err);
	System.err.print("; avg. time ");
	TimeRenderer.printTime(execTime / commandCount, System.err);
	System.err.println("; " + 
			   (1000 * commandCount / execTime) + " per second");
	return SUCCESS;
    }

    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "load file and execute commands";
    }

    public String getSynopsis(String cmd) {
	return cmd + " <filename>";
    }

    public String getLongDescription(String cmd) {
	return "\topens the file and reads the sql-commands line by line.\n"
	    +  "\tThe commands 'load' and 'start' do exaclty the same;\n"
	    +  "\t'start' is provided for compatibility with oracle SQLPLUS.";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
