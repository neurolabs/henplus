/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Iterator;

import henplus.HenPlus;
import henplus.SQLSession;
import henplus.CommandDispatcher;
import henplus.Command;
import henplus.AbstractCommand;
import henplus.view.util.Terminal;

/**
 * document me.
 */
public class HelpCommand extends AbstractCommand {
    final static int INDENT = 42;

    /**
     * returns the command-string this command can handle.
     */
    public String[] getCommandList() { return new String[]{"help", "?"}; }
    
    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * Returns a list of strings that are possible at this stage.
     */
    public Iterator complete(final CommandDispatcher disp,
			     String partialCommand, final String lastWord) 
    {
	// if we already have one arguemnt and try to expand the next: no.
	int argc = argumentCount(partialCommand);
	if (argc > 2 || (argc == 2 && lastWord.length() == 0)) {
	    return null;
	}

	final Iterator it = disp.getRegisteredCommandNames(lastWord);
	return new Iterator() {
		private String cmdName;
		public boolean hasNext() {
		    while (it.hasNext()) {
			cmdName = (String) it.next();
			if (cmdName.length() == 0) continue;
			if (!cmdName.startsWith(lastWord))
			    return false;
			// return only commands, that provide a detailed help.
			Command cmd = disp.getCommandFrom(cmdName);
			if (cmd.getLongDescription(cmdName) != null)
			    return true;
		    }
		    return false;
		}
		public Object  next() { return cmdName; }
		public void remove() { 
		    throw new UnsupportedOperationException("no!");
		}
	    };
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmdstr, String param) {
	StringTokenizer st = new StringTokenizer(param);
	if (st.countTokens() > 1)
	    return SYNTAX_ERROR;
	param = (st.hasMoreElements()) ? (String) st.nextElement() : null;
	/*
	 * nothing given: provide generic help.
	 */
	if (param == null) {
	    Iterator it = HenPlus.getInstance()
		.getDispatcher().getRegisteredCommands();
	    while (it.hasNext()) {
		Command cmd = (Command) it.next();
		String description = cmd.getShortDescription();
		if (description == null) // no description ..
		    continue;

		StringBuffer cmdPrint = new StringBuffer(" ");
		String[] cmds = cmd.getCommandList();
		String  firstSynopsis = cmd.getSynopsis(cmds[0]);
		/*
		 * either print a list of known commands or the complete
		 * synopsis, if there is only one command.
		 */
		if (cmds.length > 1 || firstSynopsis == null) {
		    for (int i = 0; i < cmds.length; ++i) {
			if (i != 0) cmdPrint.append(" | ");
			cmdPrint.append(cmds[i]);
		    }
		}
		else {
                    cmdPrint.append(firstSynopsis.length() < INDENT ? 
                                    firstSynopsis : cmds[0]);
		}
		System.err.print(cmdPrint.toString());
		for (int i = cmdPrint.length(); i < INDENT; ++i) {
		    System.err.print(" ");
		}
		System.err.print(": ");
		System.err.println(description);
	    }
	    System.err.println("config read from [" 
			       + HenPlus.getInstance().getConfigDir() + "]");
	}
	else {
	    CommandDispatcher disp = HenPlus.getInstance().getDispatcher();
	    String cmdString = disp.getCommandNameFrom(param);
	    Command c = disp.getCommandFrom(param);
	    if (c == null) {
		System.err.println("Help: unknown command '"+param+"'");
		return EXEC_FAILED;
	    }
	    printDescription(cmdString, c);
	}
	return SUCCESS;
    }

    private void printDescription(String cmdStr, Command c) {
	String desc = c.getLongDescription(cmdStr);
	if (desc == null) {
	    if (c.getShortDescription() != null) {
		desc = "\t[short description]: " + c.getShortDescription();
	    }
	}
	String synopsis = c.getSynopsis(cmdStr);
	
	if (synopsis != null) {
	    Terminal.boldface(System.err);
	    System.err.println("SYNOPSIS");
	    Terminal.reset(System.err);
	    System.err.println("\t" + synopsis);
	    System.err.println();
	}
	if (desc != null) {
	    Terminal.boldface(System.err);
	    System.err.println("DESCRIPTION");
	    Terminal.reset(System.err);
	    System.err.println(desc);
	    if (c.requiresValidSession(cmdStr)) {
		System.err.println("\tRequires valid session.");
	    }
	}
	if (desc == null && synopsis == null) {
	    System.err.println("no detailed help for '" + cmdStr + "'");
	}
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() { return "provides help for commands";}
    
    public String getSynopsis(String cmd) { return cmd + " [command]"; }

    public String getLongDescription(String cmd) {
	String dsc;
	dsc="\tProvides help for the given command.   If invoked without a\n"+
	    "\tcommand name as parameter, a list of all available commands\n"+
	    "\tis shown.";
	return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
