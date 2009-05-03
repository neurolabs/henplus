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
public class ExitCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "exit", "quit" };
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        HenPlus.getInstance().terminate();
        return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "exits HenPlus";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
