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
public class ExportCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "export" };
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        final int argc = argumentCount(param);
        HenPlus.msg().println("sorry, not implemented yet.");
        return argc == 3 ? SUCCESS : SYNTAX_ERROR;
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "export as XML, SQL or CSV";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return "export <csv|xml|sql> <table> <filename>";
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc;
        dsc = "\texports the given table.";
        return dsc;
    }

}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
