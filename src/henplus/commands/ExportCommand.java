/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.AbstractCommand;

/**
 * document me.
 */
public class ExportCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "export"
	};
    }
    
    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	int argc = argumentCount(param);
	return (argc == 2) ? SUCCESS : SYNTAX_ERROR;
    }

    /**
     * return a descriptive string.
     */
    public String  getShortDescription() {
	return "export as XML, SQL or CSV";
    }

    public String getSynopsis(String cmd) {
	return "export <csv|xml|sql> <table> <filename>";
    }

    public String getLongDescription(String cmd) {
	String dsc;
	dsc="\texports the given table.";
	return dsc;
    }

}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
