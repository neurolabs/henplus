/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Iterator;

import SQLSession;
import CommandDispatcher;
import Command;
import AbstractCommand;

/**
 * document me.
 */
public class HelpCommand extends AbstractCommand {
    final static int INDENT = 38;

    /**
     * returns the command-string this command can handle.
     */
    public String[] getCommandList() { return new String[]{"help"}; }
    
    /**
     * extract the detail request from some help command. For
     * 'help export-xml', this would be 'export-xml', for
     * just 'help', this would be null.
     */
    private String extractDetail(String help) {
	String result = null;
	Enumeration tok = new StringTokenizer(help);
	if (tok.hasMoreElements()) {
	    tok.nextElement();
	    if (tok.hasMoreElements()) {
		result = (String) tok.nextElement();
	    }
	}
	return (result != null) ? result.trim() : null;
    }

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
    public int execute(SQLSession session, String command) {
	int argc = argumentCount(command);
	if (argc > 2)
	    return SYNTAX_ERROR;
	String detailHelp = extractDetail(command);
	if (detailHelp == null) {
	    Iterator it = session.getDispatcher().getRegisteredCommands();
	    while (it.hasNext()) {
		Command cmd = (Command) it.next();
		String description = cmd.getShortDescription();
		if (description == null) // no description ..
		    continue;
		String[] cmds = cmd.getCommandList();
		StringBuffer cmdPrint = new StringBuffer(" ");
		for (int i = 0; i < cmds.length; ++i) {
		    if (i != 0) cmdPrint.append(" | ");
		    cmdPrint.append(cmds[i]);
		}
		System.err.print(cmdPrint.toString());
		for (int i = cmdPrint.length(); i < INDENT; ++i) {
		    System.err.print(" ");
		}
		System.err.print(": ");
		System.err.println(description);
	    }
	}
	else {
	    Command c = session.getDispatcher().getCommandFrom(detailHelp);
	    if (c == null) {
		System.err.println("Help: unknown command '"+detailHelp+"'");
		return EXEC_FAILED;
	    }
	    printDescription(detailHelp, c);
	}
	return SUCCESS;
    }

    private void printDescription(String cmdStr, Command c) {
	String desc = c.getLongDescription(cmdStr);
	if (desc == null) {
	    desc = "\t[short description]: " + c.getShortDescription();
	}
	String synopsis = c.getSynopsis(cmdStr);
	
	if (synopsis != null) {
	    System.err.println("SYNOPSIS");
	    System.err.println("\t" + synopsis);
	    System.err.println();
	}
	if (desc != null) {
	    System.err.println("DESCRIPTION");
	    System.err.println(desc);
	}
	if (desc == null && synopsis == null) {
	    System.err.println("no detailed help for '" + cmdStr + "'");
	}
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() { return "provides this help."; }
    
    public String getSynopsis(String cmd) { return "help [command]"; }

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
