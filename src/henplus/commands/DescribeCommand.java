/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import SQLSession;
import AbstractCommand;

/**
 * document me.
 */
public class DescribeCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] { "describe" }; 
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	int argc = argumentCount(command);
	System.out.println("not yet.");
	return (argc == 2) ? SUCCESS : SYNTAX_ERROR;
    }

    /**
     * return a descriptive string.
     */
    public String  getShortDescription() { 
	return "describe a database object";
    }
    
    public String getSynopsis(String cmd) {
	return "describe <table|view|index>";
    }

    public String getLongDescription(String cmd) {
	String dsc;
	dsc="\tDescribe the meta information of the named user object.\n" +
	    "\tA table,  for instance,  is described as a CREATE TABLE\n" +
	    "\tstatement.";
	return dsc;
    }

}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
