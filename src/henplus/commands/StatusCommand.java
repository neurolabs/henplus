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
public class StatusCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "status" };
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        HenPlus.msg().println("URL:    " + session.getURL());
        HenPlus.msg().print("uptime: ");
        TimeRenderer.printTime(session.getUptime(), HenPlus.msg());
        HenPlus.msg().println("; statements: " + session.getStatementCount());
        return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "show status of this connection";
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
