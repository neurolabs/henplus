/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */

import java.util.StringTokenizer;
import java.util.Iterator;

/**
 * Implementation of a Command with default settings. Override
 * what is necessary in your Command.
 */
public abstract class AbstractCommand implements Command {
    // no description by default.
    public String getShortDescription() { return null; }
    public String getLongDescription(String cmd) { return null; }
    public String getSynopsis(String cmd) { return null; }

    // completion of the command.
    public boolean participateInCommandCompletion() { return true; }

    // no of the commmand internally.
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, String lastWord) 
    {
	return null; 
    }

    // the simple commands are always complete on newline or semicolon
    public boolean isComplete(String command) { return true; }

    public boolean requiresValidSession(String cmd) { return true; }

    /**
     * convenience method: returns the number of elements in this
     * string, separated by whitespace.
     */
    public int argumentCount(String command) {
	return (new StringTokenizer(command)).countTokens();
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
