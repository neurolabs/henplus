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

/**
 * document me.
 */
public class CommentRemoveCommand extends AbstractCommand {
    private final HenPlus _henplus;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "remove-comments-on", "remove-comments-off"
	};
    }

    public CommentRemoveCommand(HenPlus hp) {
        _henplus = hp;
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
        if ("remove-comments-on".equals(cmd)) {
            _henplus.removeComments(true);
            System.err.println("remove comments on");
        }
        else if ("remove-comments-off".equals(cmd)) {
            _henplus.removeComments(false);
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
	return "switches the implicit removal of comments on/off";
    }

    public String getSynopsis(String cmd) {
        return cmd;
    }

    public String getLongDescription(String cmd) {
	String dsc = null;
        if ("remove-comments-on".equals(cmd)) {
            dsc= "\tSwitch on the default behaviour to remove all comments\n"
                +"\tfound in the string sent to the database. Some databases\n"
                +"\tcan not handle comments in JDBC-Strings.";
        }
        else if ("remove-comments-off".equals(cmd)) {
            dsc= "\tSwitch off the default behaviour to remove all comments\n"
                +"\tfound in the string sent to the database. Usually, this\n"
                +"\tis not necessary, but there are conditions where comments\n"
                +"\tactually convey a meaning to the database. For instance\n"
                +"\thinting in oracle works with comments, like\n"
                +"\t   select /*+ index(foo,foo_fk_idx) */ ....\n"
                +"\t.. so removing of comments should be off in this case ;-)";
        }
        return dsc;
    }

}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
